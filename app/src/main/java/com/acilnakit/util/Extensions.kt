package com.acilnakit.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp

/**
 * Architect's Power Pack - Kotlin Extensions for AcilNakit
 * Designed for senior-level productivity and premium UX.
 */

// --- UNIT CONVERSIONS & FORMATTING ---

/**
 * Formats a Double as Turkish Lira currency.
 * Usage: 100.0.toTL() -> "₺100,00"
 */
fun Double.toTL(): String = String.format(Locale("tr", "TR"), "₺%.2f", this)

/**
 * Formats a Double for price display (no decimal if whole number).
 * Usage: 100.0.toPrice() -> "₺100"
 */
fun Double.toPrice(): String {
    return if (this % 1.0 == 0.0) {
        "₺${this.toInt()}"
    } else {
        this.toTL()
    }
}

/**
 * Human-readable date extension.
 * Usage: timestamp.toRelativeTime() -> "2 dakika önce"
 */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    return when {
        diff < 60_000 -> "Az önce"
        diff < 3600_000 -> "${diff / 60_000} dk önce"
        diff < 86400_000 -> "${diff / 3600_000} saat önce"
        else -> SimpleDateFormat("dd MMM yyyy", Locale("tr")).format(Date(this))
    }
}


/**
 * Human-readable date extension for Timestamp.
 */
fun Timestamp.toRelativeTime(): String {
    return this.toDate().time.toRelativeTime()
}

/**
 * Human-readable time ago for notifications.
 * Usage: timestamp.timeAgo() -> "5 dk önce"
 */
fun Timestamp.timeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this.toDate().time
    
    return when {
        diff < 60_000 -> "Az önce"
        diff < 3600_000 -> "${diff / 60_000} dk önce"
        diff < 86400_000 -> "${diff / 3600_000} saat önce"
        diff < 604800_000 -> "${diff / 86400_000} gün önce"
        else -> SimpleDateFormat("dd MMM", Locale("tr")).format(this.toDate())
    }
}

// --- VALIDATION EXTENSIONS ---

/**
 * Universal email validator.
 */
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Validates if the email belongs to a university domain.
 */
fun String.isUniversityEmail(): Boolean {
    val regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(edu|edu\\.tr)$".toRegex()
    return regex.matches(this)
}

/**
 * Simple Turkish phone number validator.
 * Matches: 05xx xxx xx xx or 5xx xxx xx xx
 */
fun String.isValidPhone(): Boolean {
    val cleanPhone = this.replace(" ", "")
    val regex = "^(05|5)[0-9]{9}$".toRegex()
    return regex.matches(cleanPhone)
}

/**
 * Premium haptic feedback. Essential for a premium app feel.
 */
fun Context.hapticFeedback(effect: Int = -1) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val finalEffect = if (effect == -1) VibrationEffect.EFFECT_CLICK else effect
        vibrator.vibrate(VibrationEffect.createPredefined(finalEffect))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(10)
    }
}

/**
 * Success tactical feedback.
 */
fun Context.successHaptic() {
    this.hapticFeedback(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) VibrationEffect.EFFECT_HEAVY_CLICK else -1)
}

/**
 * Error tactical feedback (Double pulse).
 */
fun Context.errorHaptic() {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1))
    } else {
        @Suppress("DEPRECATION") vibrator.vibrate(200)
    }
}

/**
 * Warning tactical feedback (Soft pulse).
 */
fun Context.warningHaptic() {
    this.hapticFeedback(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else -1)
}

// --- COMPOSE UI MODIFIERS ---

/**
 * A clickable modifier without the default grey ripple. 
 * Perfect for premium Glass containers.
 */
fun Modifier.glassClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

/**
 * Easy animation entry for lists or cards.
 */
@Composable
fun AnimateFadeIn(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { 40 }),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        content()
    }
}

// --- FIRESTORE UTILS ---

/**
 * Safe conversion from Firestore Map to Model with defaults.
 */
inline fun <reified T> Map<String, Any?>.getValueOrDefault(key: String, default: T): T {
    return (this[key] as? T) ?: default
}

/**
 * Senior Logger - Clean logs for Architects
 */
fun Any.log(message: String) {
    android.util.Log.d("AcilNakit_Arch", "[${this::class.simpleName}] $message")
}

// --- FIRESTORE ARCHITECT EXTENSIONS ---

/**
 * Converts a Firestore DocumentSnapshot to a specific Model.
 * Automatically injects the document ID if the model has an 'id' field.
 * Usage: val task = snapshot.toModel<Task>()
 */
inline fun <reified T : Any> com.google.firebase.firestore.DocumentSnapshot.toModel(): T? {
    return try {
        val model = this.toObject(T::class.java)
        if (model != null) {
            // Find 'id' field even if declared in superclass or private
            var currentClass: Class<*>? = T::class.java
            while (currentClass != null) {
                try {
                    val idField = currentClass.getDeclaredField("id")
                    idField.isAccessible = true
                    idField.set(model, this.id)
                    break // Found and set
                } catch (e: NoSuchFieldException) {
                    currentClass = currentClass.superclass
                }
            }
        }
        model
    } catch (e: Exception) {
        android.util.Log.e("Extensions", "Error converting to model ${T::class.simpleName}: ${e.message}")
        null
    }
}

/**
 * Converts a Firestore QuerySnapshot to a List of Models.
 * Usage: val tasks = querySnapshot.toListModel<Task>()
 */
inline fun <reified T : Any> com.google.firebase.firestore.QuerySnapshot.toListModel(): List<T> {
    return this.documents.mapNotNull { it.toModel<T>() }
}
