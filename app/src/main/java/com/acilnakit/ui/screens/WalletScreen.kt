package com.acilnakit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acilnakit.data.model.TransactionType
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.WalletState
import com.acilnakit.ui.viewmodel.WalletViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    onNavigateToWithdraw: (Double) -> Unit,
    onNavigateToTopUp: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Yükle, 1: Çek
    var selectedMethod by remember { mutableStateOf("IBAN") }
    var accountDetails by remember { mutableStateOf("") }
    var withdrawAmount by remember { mutableStateOf("") }
    val context = LocalContext.current
    val balance by viewModel.balance.collectAsState()
    val escrowBalance by viewModel.escrowBalance.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val paymentUrl by viewModel.paymentUrl.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    
    // İş Bankası Güvenli Ödeme Penceresini Aç (Custom Tabs)
    LaunchedEffect(paymentUrl) {
        paymentUrl?.let { url ->
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(0xFF0038A8.toInt()) // İş Bankası Mavisi
                .build()
            
            customTabsIntent.launchUrl(context, Uri.parse(url))
            viewModel.clearPaymentUrl()
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is WalletState.Success -> {
                snackbarHostState.showSnackbar((uiState as WalletState.Success).message)
                withdrawAmount = ""
                accountDetails = ""
                viewModel.resetState()
            }
            is WalletState.Error -> {
                snackbarHostState.showSnackbar((uiState as WalletState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dijital Cüzdan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
        ) {
            // Bakiye Kartı
            BalancePremiumCard(
                balance = balance,
                escrowBalance = escrowBalance,
                onTopUp = { onNavigateToTopUp() },
                onWithdraw = { selectedTab = 1 }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tab Seçimi
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = FluentBlue,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Para Yükle", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Add, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Para Çek", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ArrowDownward, null) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            when (selectedTab) {
                0 -> {
                    // Manuel Para Yükleme Yönlendirme
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { onNavigateToTopUp() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                        ) {
                            Icon(Icons.Default.AddCard, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bakiye Yükle", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                1 -> {
                    // Para Çekme Formu
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { onNavigateToWithdraw(balance) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                        ) {
                            Icon(Icons.Default.AccountBalance, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Banka Hesabına Çek", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Son İşlemler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (transactions.isEmpty()) {
                    item {
                        Text(
                            text = "Henüz işlem bulunmuyor.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(transactions) { tx ->
                        TransactionItem(
                            title = getTransactionTitle(tx.type),
                            type = tx.description,
                            amount = "${if (tx.amount > 0) "+" else ""}₺${kotlin.math.abs(tx.amount).toInt()}",
                            isPlus = tx.amount > 0
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BalancePremiumCard(balance: Double, escrowBalance: Double, onTopUp: () -> Unit, onWithdraw: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                brush = Brush.linearGradient(colors = listOf(FluentBlue, Color(0xFF00B4D8))),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Mevcut Bakiyen",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "₺${String.format("%.2f", balance)}",
                        color = Color.White,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                
                if (escrowBalance > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Blokeli",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "₺${String.format("%.1f", escrowBalance)}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Güvende",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onTopUp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = FluentBlue
                        ),
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Yükle", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onWithdraw,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Çek", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WithdrawSection(
    selectedMethod: String,
    onMethodChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    accountDetails: String,
    onAccountChange: (String) -> Unit,
    balance: Double,
    isLoading: Boolean,
    onWithdraw: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WithdrawMethodChip("IBAN", Icons.Default.AccountBalance, selectedMethod == "IBAN") {
                onMethodChange("IBAN")
            }
            WithdrawMethodChip("Papara", Icons.Default.Wallet, selectedMethod == "Papara") {
                onMethodChange("Papara")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Çekilecek Tutar (₺)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            supportingText = {
                Text("Min: ₺50 | Mevcut: ₺${balance.toInt()}")
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = accountDetails,
            onValueChange = onAccountChange,
            label = { Text(if (selectedMethod == "IBAN") "IBAN (TR...)" else "Papara No") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onWithdraw,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FluentBlue),
            enabled = !isLoading && (amount.toDoubleOrNull() ?: 0.0) >= 50 && 
                      (amount.toDoubleOrNull() ?: 0.0) <= balance &&
                      accountDetails.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Nakit Olarak Çek", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "⏱️ İşlem süresi: 1-3 iş günü",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RowScope.WithdrawMethodChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        color = if (isSelected) FluentBlue.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) FluentBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) FluentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) FluentBlue else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TransactionItem(title: String, type: String, amount: String, isPlus: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isPlus) Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else Color.Red.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlus) Icons.Default.Add else Icons.Default.Remove,
                        contentDescription = null,
                        tint = if (isPlus) Color(0xFF4CAF50) else Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (isPlus) Color(0xFF4CAF50) else Color.Red
            )
        }
    }
}

fun getTransactionTitle(type: TransactionType): String {
    return when (type) {
        TransactionType.DEPOSIT -> "Para Yükleme"
        TransactionType.EARNING -> "Görev Kazancı"
        TransactionType.WITHDRAWAL -> "Para Çekme"
        TransactionType.TASK_PAYMENT -> "Görev Ödemesi"
        TransactionType.REFUND -> "İade"
    }
}
