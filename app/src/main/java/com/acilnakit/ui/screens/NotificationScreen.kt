package com.acilnakit.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acilnakit.data.model.Notification
import com.acilnakit.data.model.NotificationType
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.NotificationViewModel
import com.acilnakit.util.timeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Bildirimler", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Text(
                                "$unreadCount okunmamış",
                                fontSize = 12.sp,
                                color = FluentBlue
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = { viewModel.markAllAsRead() },
                            enabled = !isLoading
                        ) {
                            Text("Tümünü Okundu Yap", color = FluentBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = FluentBlue
                    )
                }
                notifications.isEmpty() -> {
                    EmptyNotificationsState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bugünkü bildirimler
                        val todayNotifications = notifications.filter { 
                            it.createdAt?.let { ts ->
                                val diff = System.currentTimeMillis() - ts.toDate().time
                                diff < 24 * 60 * 60 * 1000
                            } ?: false
                        }
                        
                        if (todayNotifications.isNotEmpty()) {
                            item {
                                Text(
                                    "Bugün",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            items(todayNotifications, key = { it.id }) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onClick = {
                                        viewModel.markAsRead(notification.id)
                                        notification.taskId?.let { onTaskClick(it) }
                                        notification.chatId?.let { onChatClick(it) }
                                    },
                                    onDelete = { viewModel.deleteNotification(notification.id) }
                                )
                            }
                        }
                        
                        // Daha eski bildirimler
                        val olderNotifications = notifications.filter { 
                            it.createdAt?.let { ts ->
                                val diff = System.currentTimeMillis() - ts.toDate().time
                                diff >= 24 * 60 * 60 * 1000
                            } ?: true
                        }
                        
                        if (olderNotifications.isNotEmpty()) {
                            item {
                                Text(
                                    "Daha Önce",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            items(olderNotifications, key = { it.id }) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onClick = {
                                        viewModel.markAsRead(notification.id)
                                        notification.taskId?.let { onTaskClick(it) }
                                        notification.chatId?.let { onChatClick(it) }
                                    },
                                    onDelete = { viewModel.deleteNotification(notification.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Bildirimi Sil") },
            text = { Text("Bu bildirimi silmek istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Sil", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
    
    val notificationType = notification.getNotificationType()
    val (icon, iconColor) = getNotificationStyle(notificationType)
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // İkon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            // İçerik
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        notification.title,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Okunmadı göstergesi
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(FluentBlue)
                        )
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    notification.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (!notification.isRead) 0.8f else 0.6f
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    notification.createdAt?.let { timestamp ->
                        Text(
                            timestamp.timeAgo(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(FluentBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = FluentBlue,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            "Bildirim Yok",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            "Yeni bildirimler burada görünecek",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Bildirim türüne göre ikon ve renk döndürür
 */
fun getNotificationStyle(type: NotificationType): Pair<ImageVector, Color> {
    return when (type) {
        NotificationType.TASK_REQUEST -> Icons.Default.PersonAdd to Color(0xFF2196F3)
        NotificationType.TASK_ASSIGNED -> Icons.Default.Assignment to Color(0xFF4CAF50)
        NotificationType.TASK_COMPLETED -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        NotificationType.TASK_CANCELLED -> Icons.Default.Cancel to Color(0xFFF44336)
        NotificationType.PAYMENT_RECEIVED -> Icons.Default.AccountBalanceWallet to Color(0xFF4CAF50)
        NotificationType.PAYMENT_SENT -> Icons.Default.Payment to Color(0xFFFF9800)
        NotificationType.NEW_MESSAGE -> Icons.Default.Message to Color(0xFF2196F3)
        NotificationType.NEW_REVIEW -> Icons.Default.Star to Color(0xFFFFB300)
        NotificationType.SYSTEM -> Icons.Default.Info to Color(0xFF9E9E9E)
    }
}
