package com.acilnakit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acilnakit.data.model.Task
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.designsystem.RadarPulse
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.SortOrder
import com.acilnakit.ui.viewmodel.TaskViewModel
import com.acilnakit.ui.viewmodel.WalletViewModel
import com.acilnakit.util.*

@Composable
fun HomeScreen(
    onCreateTask: () -> Unit,
    onTaskClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onWalletClick: () -> Unit,
    onActiveTasksClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    viewModel: TaskViewModel = hiltViewModel(),
    walletViewModel: WalletViewModel = hiltViewModel(),
    notificationViewModel: com.acilnakit.ui.viewmodel.NotificationViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val currentSort by viewModel.sortOrder.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val balance by walletViewModel.balance.collectAsState()
    val unreadNotifications by notificationViewModel.unreadCount.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTask,
                containerColor = FluentBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Görev")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Header: Profile & Greeting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Selam, Ahmet!", 
                                    style = MaterialTheme.typography.headlineSmall, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Bugün nasıl bir katkı sağlayalım?", 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Row {
                                // Bildirimler Butonu
                                Box {
                                    IconButton(
                                        onClick = onNotificationsClick,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(FluentBlue.copy(alpha = 0.1f))
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Bildirimler", tint = FluentBlue)
                                    }
                                    // Badge
                                    if (unreadNotifications > 0) {
                                        Badge(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 2.dp, y = (-2).dp),
                                            containerColor = Color.Red
                                        ) {
                                            Text(
                                                if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.width(8.dp))
                                
                                // Profil Butonu
                                IconButton(
                                    onClick = onProfileClick,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(FluentBlue.copy(alpha = 0.1f))
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = FluentBlue)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Header: Balance Card
                        WalletHeader(balance = balance, onClick = onWalletClick)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Aktif Görevler Butonu
                        OutlinedButton(
                            onClick = onActiveTasksClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FluentBlue)
                        ) {
                            Icon(Icons.Default.Assignment, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Aktif Görevlerim")
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Arama Çubuğu
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Görev ara...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = FluentBlue) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, null)
                                    }
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Hızlı Keşfet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Category Selector (Horizontal Scroll)
                item {
                    CategorySelector(
                        selectedCategory = selectedCategory ?: "Hepsi",
                        onCategorySelected = { viewModel.setCategory(it) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        // Sort Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sırala:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SortChip("En Yeni", currentSort == SortOrder.NEWEST) { viewModel.setSortOrder(SortOrder.NEWEST) }
                            SortChip("Fiyat ↓", currentSort == SortOrder.PRICE_HIGH) { viewModel.setSortOrder(SortOrder.PRICE_HIGH) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Real-time Radar Scan UI
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            RadarPulse()
                            Text(
                                text = "Kampüs taranıyor...",
                                style = MaterialTheme.typography.labelSmall,
                                color = FluentBlue.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 80.dp)
                            )
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Şu an uygun görev bulunmuyor.", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(tasks, key = { it.id }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            TaskItem(task, onClick = { onTaskClick(task.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelector(selectedCategory: String, onCategorySelected: (String) -> Unit) {
    val categories = listOf("Hepsi", "Market", "Akademik", "Yemek", "Teknik", "Eşya Taşınma")
    
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            Surface(
                onClick = { onCategorySelected(category) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) FluentBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.height(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = category, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun WalletHeader(balance: Double, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dijital Cüzdan",
                    style = MaterialTheme.typography.labelMedium,
                    color = FluentBlue
                )
                Text(
                    text = balance.toTL(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black
                    )
                )
            }
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = FluentBlue,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
}

@Composable
fun TaskItem(task: Task, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        onClick = {
            context.hapticFeedback()
            onClick()
        },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = FluentBlue
                    )
                    
                    // Dynamic Trust Badges
                    if (task.creatorTasksCompleted > 50) {
                        com.acilnakit.ui.designsystem.Badge("Elite Tasker", Icons.Default.Star, Color(0xFFFFD700))
                    } else if (task.creatorTasksCompleted > 10) {
                        com.acilnakit.ui.designsystem.Badge("Güvenilir", Icons.Default.CheckCircle, Color(0xFF4CAF50))
                    }
                    
                    if (task.creatorRating >= 4.8 && task.creatorTasksCompleted > 5) {
                        com.acilnakit.ui.designsystem.Badge("Yüksek Puan", Icons.Default.MilitaryTech, Color(0xFF673AB7))
                    }
                }
                
                Text(
                    text = task.rewardAmount.toTL().substringBefore(","), // Tam sayı göster
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.campusName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${task.creatorRating} ★",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFB300),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
}
@Composable
fun SortChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        shape = RoundedCornerShape(100.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = FluentBlue.copy(alpha = 0.1f),
            selectedLabelColor = FluentBlue,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = FluentBlue
        )
    )
}
