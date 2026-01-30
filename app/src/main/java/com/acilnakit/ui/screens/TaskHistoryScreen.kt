package com.acilnakit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.border
import androidx.hilt.navigation.compose.hiltViewModel
import com.acilnakit.data.model.Task
import com.acilnakit.data.model.TaskStatus
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.TaskViewModel
import com.acilnakit.util.toTL
import com.acilnakit.util.toRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskHistoryScreen(
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val history by viewModel.taskHistory.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Aktif Görevler", "Geçmiş")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Görevlerim", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = FluentBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = FluentBlue
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            val displayList = if (selectedTab == 0) activeTasks else history

            if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Task else Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (selectedTab == 0) "Henüz aktif bir göreviniz yok." else "Geçmiş görev bulunmuyor.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList) { task ->
                        HistoryTaskItem(task = task, onClick = { onTaskClick(task.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTaskItem(task: Task, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = task.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        task.createdAt.toRelativeTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
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

@Composable
fun StatusBadge(status: TaskStatus) {
    val (color, text) = when (status) {
        TaskStatus.OPEN -> Color.Gray to "Açık"
        TaskStatus.REQUESTED -> FluentBlue to "Başvuru Var"
        TaskStatus.ASSIGNED -> Color(0xFFE67E22) to "Atandı"
        TaskStatus.IN_PROGRESS -> Color(0xFF3498DB) to "Devam Ediyor"
        TaskStatus.DELIVERED -> Color(0xFFF1C40F) to "Teslim Edildi"
        TaskStatus.COMPLETED -> Color(0xFF27AE60) to "Tamamlandı"
        TaskStatus.CANCELLED -> Color.Red.copy(alpha = 0.7f) to "İptal Edildi"
        TaskStatus.DISPUTED -> Color.Red to "Sorunlu"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
