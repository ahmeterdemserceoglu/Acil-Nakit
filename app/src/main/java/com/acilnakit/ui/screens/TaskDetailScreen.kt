package com.acilnakit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.acilnakit.data.model.Task
import com.acilnakit.data.model.TaskRequest
import com.acilnakit.data.model.TaskStatus
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.designsystem.MissionControlProgress
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.TaskDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    onContact: (String) -> Unit,
    onViewProfile: (String) -> Unit = {},
    onEditTask: () -> Unit = {},
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val task by viewModel.task.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val isWorker by viewModel.isWorker.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val hasRequested by viewModel.hasRequested.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val cancelSuccess by viewModel.cancelSuccess.collectAsState()

    LaunchedEffect(cancelSuccess) {
        if (cancelSuccess) {
            onBack()
        }
    }
    
    var showRequestDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showDisputeDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var requestMessage by remember { mutableStateOf("") }
    
    var ratingValue by remember { mutableStateOf(5f) }
    var ratingComment by remember { mutableStateOf("") }
    
    var showCancelDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    // Başvuru Dialog
    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = { Text("Göreve Başvur", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Neden bu görevi üstlenmek istiyorsunuz?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = requestMessage,
                        onValueChange = { requestMessage = it },
                        placeholder = { Text("Mesajınız (opsiyonel)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.requestTask(requestMessage)
                        showRequestDialog = false
                        requestMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) {
                    Text("Başvur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Teslim Onay Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp)) },
            title = { Text("Teslimatı Onayla", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("İşin teslim edildiğini onaylıyor musunuz?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Onayladığınızda ödeme serbest bırakılacak ve geri alınamayacaktır.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmDelivery()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Onayla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Oylama Dialog
    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = { Text("Deneyiminizi Oylayın", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isOwner) "İşçinin performansını nasıl buldunuz?" 
                        else "Görev sahibinin iletişimini nasıl buldunuz?",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Basit Yıldız Seçimi (Row içinde 5 ikon)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { index ->
                            Icon(
                                if (index <= ratingValue) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = if (index <= ratingValue) Color(0xFFFFB300) else Color.LightGray,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { ratingValue = index.toFloat() }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = ratingComment,
                        onValueChange = { ratingComment = it },
                        placeholder = { Text("Düşüncelerinizi paylaşın...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReview(ratingValue, ratingComment)
                        showRatingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) {
                    Text("Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Şikayet Dialog
    if (showDisputeDialog) {
        var disputeReason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDisputeDialog = false },
            icon = { Icon(Icons.Default.Report, null, tint = Color.Red, modifier = Modifier.size(48.dp)) },
            title = { Text("Şikayet Et", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Sorunu açıklayın:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = disputeReason,
                        onValueChange = { disputeReason = it },
                        placeholder = { Text("Şikayet sebebi...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "📹 Kanıt videonuzu daha sonra yükleyebilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createDispute(disputeReason)
                        showDisputeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Şikayet Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisputeDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // İptal Onay Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Görevi İptal Et", fontWeight = FontWeight.Bold) },
            text = { Text("Görevi iptal etmek istediğinize emin misiniz? Görev ücreti cüzdanınıza iade edilecektir.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelTask()
                        showCancelDialog = false
                        // onBack() kaldırıldı, state ile yönetilecek
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Görevi İptal Et")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Görev Detayı") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    // Düzenle butonu (sadece görev sahibi ve OPEN/REQUESTED durumunda)
                    if (isOwner && task?.status in listOf(TaskStatus.OPEN, TaskStatus.REQUESTED)) {
                        IconButton(onClick = onEditTask) {
                            Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                        }
                    }
                    if (isOwner || isWorker) {
                        IconButton(onClick = {
                            scope.launch {
                                val chatId = viewModel.getOrCreateChat()
                                if (chatId != null) onContact(chatId)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Mesaj")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        task?.let { currentTask ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Durum Badge
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kategori
                        Surface(
                            color = FluentBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = currentTask.category,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = FluentBlue
                            )
                        }
                        
                        // Durum
                        TaskStatusBadge(currentTask.status)
                    }
                }

                // Başlık
                item {
                    Text(
                        text = currentTask.title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        )
                    )
                }

                // Escrow Durumu & Timeline
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        TaskTimeline(currentStatus = currentTask.status)
                        EscrowStatusGuard(currentTask.status)
                    }
                }

                // Açıklama
                item {
                    Text(
                        text = "Görev Detayı",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentTask.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }

                // Atanmış İşçi Bilgisi (Sadece atandıysa)
                if (currentTask.status != TaskStatus.OPEN && currentTask.status != TaskStatus.REQUESTED) {
                    item {
                        AssignedWorkerCard(
                            isOwner = isOwner,
                            workerPhone = currentTask.workerPhone,
                            creatorPhone = currentTask.creatorPhone,
                            status = currentTask.status
                        )
                    }
                }

                // Başvuranlar (Sadece görev sahibi görür)
                if (isOwner && requests.isNotEmpty() && currentTask.status in listOf(TaskStatus.OPEN, TaskStatus.REQUESTED)) {
                    item {
                        Text(
                            text = "Başvuranlar (${requests.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    
                    items(requests) { request ->
                        RequestCard(
                            request = request,
                            onAssign = { viewModel.assignToUser(request) }
                        )
                    }
                }

                // Ücret ve Aksiyon Kartı
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Ödeme",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "₺${currentTask.rewardAmount.toInt()}",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                                    )
                                }
                                
                                // Aksiyon Butonları
                                TaskActionButton(
                                    task = currentTask,
                                    isOwner = isOwner,
                                    isWorker = isWorker,
                                    hasRequested = hasRequested,
                                    isLoading = isLoading,
                                    onRequest = { showRequestDialog = true },
                                    onStartWork = { viewModel.startWork() },
                                    onDelivered = { viewModel.markAsDelivered() },
                                    onConfirm = { showConfirmDialog = true },
                                    onDispute = { showDisputeDialog = true },

                                    onRate = { showRatingDialog = true },
                                    onCancel = { showCancelDialog = true }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FluentBlue)
        }
    }
}

@Composable
fun TaskStatusBadge(status: TaskStatus) {
    val (text, color) = when (status) {
        TaskStatus.OPEN -> "Açık" to Color(0xFF4CAF50)
        TaskStatus.REQUESTED -> "Başvuru Var" to Color(0xFFFF9800)
        TaskStatus.ASSIGNED -> "Atandı" to FluentBlue
        TaskStatus.IN_PROGRESS -> "Devam Ediyor" to Color(0xFF9C27B0)
        TaskStatus.DELIVERED -> "Teslim Edildi" to Color(0xFF00BCD4)
        TaskStatus.COMPLETED -> "Tamamlandı" to Color(0xFF4CAF50)
        TaskStatus.DISPUTED -> "Şikayette" to Color.Red
        TaskStatus.CANCELLED -> "İptal" to Color.Gray
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
fun TaskTimeline(currentStatus: TaskStatus) {
    val steps = listOf(
        TaskStatus.OPEN to "Görev Oluşturuldu",
        TaskStatus.ASSIGNED to "Aday Seçildi",
        TaskStatus.IN_PROGRESS to "İş Başladı",
        TaskStatus.DELIVERED to "Teslim Edildi",
        TaskStatus.COMPLETED to "Onaylandı"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        steps.forEachIndexed { index, (status, title) ->
            val isCompleted = isStatusReached(currentStatus, status)
            val isCurrent = currentStatus == status
            
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dot and Line Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 12.dp else 8.dp)
                            .background(
                                color = if (isCompleted) FluentBlue else Color.LightGray.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .border(
                                width = if (isCurrent) 2.dp else 0.dp,
                                color = if (isCurrent) FluentBlue.copy(alpha = 0.3f) else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                    
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(
                                    if (isCompleted && isStatusReached(currentStatus, steps[index+1].first)) 
                                        FluentBlue 
                                    else 
                                        Color.LightGray.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface 
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                
                if (isCurrent) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.PlayArrow, 
                        null, 
                        tint = FluentBlue, 
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun isStatusReached(current: TaskStatus, target: TaskStatus): Boolean {
    val order = listOf(
        TaskStatus.OPEN,
        TaskStatus.REQUESTED,
        TaskStatus.ASSIGNED,
        TaskStatus.IN_PROGRESS,
        TaskStatus.DELIVERED,
        TaskStatus.COMPLETED
    )
    val currentIndex = order.indexOf(current)
    val targetIndex = order.indexOf(target)
    return currentIndex >= targetIndex
}

@Composable
fun EscrowStatusGuard(status: TaskStatus) {
    val icon: ImageVector
    val title: String
    val subtitle: String
    val bgColor: Color
    
    when (status) {
        TaskStatus.COMPLETED -> {
            icon = Icons.Default.CheckCircle
            title = "Ödeme Onaylandı"
            subtitle = "Para çalışanın cüzdanına başarıyla aktarıldı."
            bgColor = Color(0xFF4CAF50)
        }
        TaskStatus.DISPUTED -> {
            icon = Icons.Default.Warning
            title = "Şikayet İnceleniyor"
            subtitle = "Ödeme, moderasyon kararına kadar sistemde kilitlendi."
            bgColor = Color.Red
        }
        else -> {
            icon = Icons.Default.Lock
            title = "Güvenceli Ödeme (Escrow)"
            subtitle = "İşveren parayı bloke etti, iş onaylandığında yatar."
            bgColor = FluentBlue
        }
    }
    
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = bgColor, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = bgColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AssignedWorkerCard(
    isOwner: Boolean,
    workerPhone: String?,
    creatorPhone: String,
    status: TaskStatus
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Phone, null, tint = FluentBlue)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isOwner) "İşçi Telefonu" else "Görev Sahibi Telefonu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isOwner) (workerPhone ?: "Belirtilmedi") else creatorPhone,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RequestCard(
    request: TaskRequest,
    onAssign: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FluentBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = FluentBlue)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(request.userName, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${request.userRating}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            Button(
                onClick = onAssign,
                colors = ButtonDefaults.buttonColors(containerColor = FluentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Görevi Ver")
            }
        }
        
        if (request.message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "\"${request.message}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TaskActionButton(
    task: Task,
    isOwner: Boolean,
    isWorker: Boolean,
    hasRequested: Boolean,
    isLoading: Boolean,
    onRequest: () -> Unit,
    onStartWork: () -> Unit,
    onDelivered: () -> Unit,
    onConfirm: () -> Unit,
    onDispute: () -> Unit,
    onRate: () -> Unit,
    onCancel: () -> Unit
) {
    when {
        isLoading -> {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = FluentBlue)
        }
        
        isOwner -> {
            // Görev sahibi aksiyonları
            when (task.status) {
                TaskStatus.DELIVERED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDispute,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Şikayet")
                        }
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Teslim Aldım")
                        }
                    }
                }
                TaskStatus.COMPLETED -> {
                    if (!task.ratedByCreator) {
                        Button(
                            onClick = onRate,
                            colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                        ) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("İşçiyi Oyla")
                        }
                    } else {
                        Text("✅ Tamamlandı", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
                TaskStatus.DISPUTED -> {
                    Text("⚠️ İnceleniyor", color = Color.Red, fontWeight = FontWeight.Bold)
                }
                TaskStatus.OPEN, TaskStatus.REQUESTED -> {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Red),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Görevi İptal Et")
                    }
                }
                TaskStatus.CANCELLED -> {
                     Text("❌ İptal Edildi", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                else -> {
                    Text("Kendi görevin", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        isWorker -> {
            // İşçi aksiyonları
            when (task.status) {
                TaskStatus.ASSIGNED -> {
                    Button(
                        onClick = onStartWork,
                        colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                    ) {
                        Text("İşe Başla")
                    }
                }
                TaskStatus.IN_PROGRESS -> {
                    Button(
                        onClick = onDelivered,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Teslim Ettim")
                    }
                }
                TaskStatus.DELIVERED -> {
                    if (!task.workerConfirmed) {
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Onaylıyorum")
                        }
                    } else {
                        Text("⏳ Onay Bekleniyor", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                    }
                }
                TaskStatus.COMPLETED -> {
                    if (!task.ratedByWorker) {
                        Button(
                            onClick = onRate,
                            colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                        ) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deneyimi Oyla")
                        }
                    } else {
                        Text("✅ Tamamlandı", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        }
        
        else -> {
            // Dış kullanıcı
            when (task.status) {
                TaskStatus.OPEN, TaskStatus.REQUESTED -> {
                    if (hasRequested) {
                        Text("📩 Başvurun Gönderildi", fontWeight = FontWeight.Bold, color = FluentBlue)
                    } else {
                        Button(
                            onClick = onRequest,
                            colors = ButtonDefaults.buttonColors(containerColor = FluentBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Görevi İste")
                        }
                    }
                }
                else -> {
                    Text("Bu görev başkasına atandı", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
