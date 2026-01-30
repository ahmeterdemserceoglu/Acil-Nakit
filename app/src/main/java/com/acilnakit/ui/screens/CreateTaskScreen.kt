package com.acilnakit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CurrencyLira
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.CreateTaskState
import com.acilnakit.ui.viewmodel.CreateTaskViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onNavigateToWallet: () -> Unit,
    viewModel: CreateTaskViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var reward by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Yazılım") }
    var customCategory by remember { mutableStateOf("") }
    var showInsufficientBalanceDialog by remember { mutableStateOf(false) }
    
    val state by viewModel.state.collectAsState()
    val userBalance by viewModel.userBalance.collectAsState()
    
    val categories = listOf(
        "Yazılım", "Çeviri", "Tasarım", "Ödev", 
        "Teknik Destek", "Eşya Taşıma", "Evrak/Ofis", "Yardım", "Diğer"
    )

    // Yetersiz Bakiye Dialog
    if (showInsufficientBalanceDialog) {
        val insufficientState = state as? CreateTaskState.InsufficientBalance
        AlertDialog(
            onDismissRequest = { 
                showInsufficientBalanceDialog = false 
                viewModel.resetState()
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Yetersiz Bakiye", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Hesabınızda yeterli bakiye bulunmuyor.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Bakiye Bilgisi
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Mevcut Bakiye:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "₺${String.format("%.2f", insufficientState?.current ?: userBalance)}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gereken:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "₺${String.format("%.2f", insufficientState?.required ?: 0.0)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Eksik:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "₺${String.format("%.2f", (insufficientState?.required ?: 0.0) - (insufficientState?.current ?: 0.0))}",
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInsufficientBalanceDialog = false
                        viewModel.resetState()
                        onNavigateToWallet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hemen Yükle")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showInsufficientBalanceDialog = false
                    viewModel.resetState()
                }) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni Görev Bırak", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    // Bakiye göstergesi
                    Surface(
                        color = FluentBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                null,
                                tint = FluentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "₺${String.format("%.0f", userBalance)}",
                                color = FluentBlue,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Birine nakit kazandır, işini anında bitir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title Input
            Text(text = "Görev Başlığı", style = MaterialTheme.typography.labelLarge, color = FluentBlue)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Örn: Python Script Hata Ayıklama") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category Selection
            Text(text = "Kategori", style = MaterialTheme.typography.labelLarge, color = FluentBlue)
            Spacer(modifier = Modifier.height(12.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) },
                        shape = RoundedCornerShape(100.dp)
                    )
                }
            }

            if (category == "Diğer") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    placeholder = { Text("Kategori adını yazın...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description
            Text(text = "Detaylar", style = MaterialTheme.typography.labelLarge, color = FluentBlue)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Yapılacak işi detaylandırın...") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reward Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CurrencyLira, contentDescription = null, tint = FluentBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Ödenecek Tutar", style = MaterialTheme.typography.labelSmall)
                        BasicTextField(
                            value = reward,
                            onValueChange = { reward = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
            
            // Bakiye uyarısı
            val rewardAmount = reward.toDoubleOrNull() ?: 0.0
            if (rewardAmount > userBalance && rewardAmount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFF9800).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Bakiyeniz yetersiz! ₺${String.format("%.0f", rewardAmount - userBalance)} daha yüklemeniz gerekiyor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = {
                    val amount = reward.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        viewModel.createTask(
                            title = title,
                            description = description,
                            reward = amount,
                            category = if (category == "Diğer") customCategory else category,
                            onSuccess = onSuccess,
                            onInsufficientBalance = { showInsufficientBalanceDialog = true }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FluentBlue),
                enabled = state !is CreateTaskState.Loading
            ) {
                if (state is CreateTaskState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Görevi Yayınla & Ödemeyi Kilitle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "* Ödeme Collabo Escrow hesabında iş bitene kadar kilitlenir.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
