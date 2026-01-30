package com.acilnakit.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acilnakit.data.model.Task
import com.acilnakit.data.model.TaskStatus
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.ActiveTasksViewModel
import com.acilnakit.util.toTL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTasksScreen(
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: ActiveTasksViewModel = hiltViewModel()
) {
    val myCreatedTasks by viewModel.myCreatedTasks.collectAsState()
    val myAssignedTasks by viewModel.myAssignedTasks.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aktif Görevlerim", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
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
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = FluentBlue,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Create, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Oluşturduğum (${myCreatedTasks.size})")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Üstlendiğim (${myAssignedTasks.size})")
                        }
                    }
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Task Lists
            val tasks = if (selectedTab == 0) myCreatedTasks else myAssignedTasks
            val isOwner = selectedTab == 0
            
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (isOwner) Icons.Default.Create else Icons.Default.Build,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (isOwner) "Henüz aktif görev oluşturmadınız"
                            else "Henüz bir görev üstlenmediniz",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks) { task ->
                        ActiveTaskCard(
                            task = task,
                            isOwner = isOwner,
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveTaskCard(
    task: Task,
    isOwner: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                ActiveStatusBadge(status = task.status)
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Progress Indicator
            TaskProgressIndicator(status = task.status)
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role Badge
                Surface(
                    color = if (isOwner) FluentBlue.copy(alpha = 0.1f) else Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isOwner) "👑 Görev Sahibi" else "🔧 İşçi",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = if (isOwner) FluentBlue else Color(0xFF4CAF50)
                    )
                }
                
                Text(
                    task.rewardAmount.toTL(),
                    fontWeight = FontWeight.Black,
                    color = FluentBlue,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun ActiveStatusBadge(status: TaskStatus) {
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
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun TaskProgressIndicator(status: TaskStatus) {
    val steps = listOf("Açık", "Atandı", "Devam", "Teslim", "Tamamlandı")
    val currentStep = when (status) {
        TaskStatus.OPEN, TaskStatus.REQUESTED -> 0
        TaskStatus.ASSIGNED -> 1
        TaskStatus.IN_PROGRESS -> 2
        TaskStatus.DELIVERED -> 3
        TaskStatus.COMPLETED -> 4
        else -> 0
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isCompleted = index <= currentStep
            val isCurrent = index == currentStep
            
            // Step Circle
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> FluentBlue
                            isCompleted -> FluentBlue.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        }
                    )
            )
            
            // Connector Line
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            if (index < currentStep) FluentBlue.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                )
            }
        }
    }
}
