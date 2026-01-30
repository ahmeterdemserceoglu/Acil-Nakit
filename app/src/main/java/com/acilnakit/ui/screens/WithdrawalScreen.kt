package com.acilnakit.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.WithdrawalViewModel
import com.acilnakit.util.toTL
import com.acilnakit.util.hapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalScreen(
    onBack: () -> Unit,
    currentBalance: Double,
    viewModel: WithdrawalViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("TR") }
    var accountHolder by remember { mutableStateOf("") }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val success by viewModel.success.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(success) {
        if (success) {
            context.hapticFeedback(android.os.VibrationEffect.EFFECT_HEAVY_CLICK)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Para Çekme", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mevcut Bakiye Kartı
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Çekilebilir Bakiye",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            currentBalance.toTL(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = FluentBlue
                        )
                    }
                }

                // İş Bankası / Hızlı Transfer Bilgi Notu
                Surface(
                    color = Color(0xFF0038A8).copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF0038A8).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFF0038A8).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF0038A8), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Hızlı Transfer Aktif",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0038A8)
                            )
                            Text(
                                "Çekim talebiniz İş Bankası altyapısı ile 1 saat içinde tamamlanır.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Form Alanları
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val requestedAmt = amount.toDoubleOrNull() ?: 0.0
                    val fee = 5.0
                    val total = if (requestedAmt > 0) requestedAmt + fee else 0.0

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("İşlem Özeti", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Talep Edilen:", style = MaterialTheme.typography.bodyMedium)
                                Text("₺${String.format("%.2f", requestedAmt)}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("İşlem Ücreti:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                Text("₺5.00", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Toplam Düşecek:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("₺${String.format("%.2f", total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0038A8))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                        label = { Text("Transfer Edilecek Net Tutar") },
                        prefix = { Text("₺", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0038A8))
                    )

                        OutlinedTextField(
                            value = accountHolder,
                            onValueChange = { accountHolder = it },
                            label = { Text("Alıcı Ad Soyad") },
                            placeholder = { Text("İsim Soyisim") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0038A8))
                        )

                        OutlinedTextField(
                            value = iban,
                            onValueChange = { if (it.length <= 26) iban = it.uppercase() },
                            label = { Text("IBAN Numarası") },
                            placeholder = { Text("TR...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0038A8)),
                            supportingText = { Text("${iban.length}/26") }
                        )
                    }
                }

                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { 
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        viewModel.requestWithdrawal(amt, iban, accountHolder)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0038A8)),
                    enabled = !isLoading && amount.isNotBlank() && iban.length >= 24 && accountHolder.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("ÇEKİM TALEBİNİ İLET", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
