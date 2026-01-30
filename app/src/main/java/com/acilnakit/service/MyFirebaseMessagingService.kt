package com.acilnakit.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.acilnakit.MainActivity
import com.acilnakit.R
import com.acilnakit.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var firestore: FirebaseFirestore

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            updateTokenInFirestore(userId, token)
        }
    }

    private fun updateTokenInFirestore(userId: String, token: String) {
        scope.launch {
            try {
                firestore.collection("users").document(userId)
                    .update("fcmToken", token)
                android.util.Log.d("FCMService", "Token updated successfully")
            } catch (e: Exception) {
                android.util.Log.e("FCMService", "Failed to update token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        
        if (title != null && body != null) {
            sendNotification(title, body, message.data)
        }
    }

    private fun sendNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Verileri intent'e ekle (tıklandığında açılması için)
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "high_priority_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Önemli Bildirimler",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Zengin İçerik: Eğer resim URL'si varsa ekle
        val imageUrl = data["imageUrl"]
        if (imageUrl != null) {
            scope.launch {
                try {
                    val loader = coil.ImageLoader(this@MyFirebaseMessagingService)
                    val request = coil.request.ImageRequest.Builder(this@MyFirebaseMessagingService)
                        .data(imageUrl)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    if (result is coil.request.SuccessResult) {
                        val bitmap = (result.drawable as android.graphics.drawable.BitmapDrawable).bitmap
                        notificationBuilder.setLargeIcon(bitmap)
                        notificationBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as android.graphics.Bitmap?))
                        notificationManager.notify(notificationId, notificationBuilder.build())
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FCMService", "Error loading notification image: ${e.message}")
                }
            }
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
