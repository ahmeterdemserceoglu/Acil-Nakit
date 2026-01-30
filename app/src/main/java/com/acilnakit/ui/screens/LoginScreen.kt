package com.acilnakit.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.AuthState
import com.acilnakit.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val onSurf = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Verified -> onLoginSuccess()
            is AuthState.NeedsCampus -> onLoginSuccess()
            else -> {} // Handle other states if necessary or do nothing
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ACİL NAKİT",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = FluentBlue,
                    letterSpacing = 2.sp
                )
            )
            Text(
                text = "KAMPÜS ACİL GÖREV RADARI",
                style = MaterialTheme.typography.labelMedium,
                color = FluentBlue,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedContent(
                targetState = authState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "AuthContent"
            ) { state ->
                when (state) {
                    is AuthState.VerificationSent -> VerificationSentView(
                        email = email, 
                        onCheck = { viewModel.checkStatus() },
                        isLoading = false
                    )
                    is AuthState.NotVerifiedYet -> NotVerifiedView(
                        message = (state as AuthState.NotVerifiedYet).message,
                        onCheck = { viewModel.checkStatus() },
                        onResend = { viewModel.resendVerification() },
                        onBack = { viewModel.resetState() }
                    )   
                    is AuthState.Loading, is AuthState.Verified, is AuthState.NeedsCampus -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FluentBlue)
                    }
                    else -> LoginInputView(
                        email = email,
                        onEmailChange = { email = it },
                        onContinue = { viewModel.loginOrRegister(email) },
                        error = (state as? AuthState.Error)?.message
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Hızlı ödeme, anında nakit.\nSadece öğrenciler için.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LoginInputView(
    email: String,
    onEmailChange: (String) -> Unit,
    onContinue: () -> Unit,
    error: String?
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Kampüs E-postası ile Giriş",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = { Text("ogrenci@itu.edu.tr") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = FluentBlue) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FluentBlue,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )

            if (error != null) {
                Text(text = error, color = Color.Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FluentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Devam Et", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VerificationSentView(email: String, onCheck: () -> Unit, isLoading: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(FluentBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = FluentBlue, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "E-posta Gönderildi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$email adresine bir doğrulama bağlantısı gönderdik. Lütfen gelen kutunu kontrol et.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onCheck,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FluentBlue),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Doğruladım, Giriş Yap", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotVerifiedView(message: String, onCheck: () -> Unit, onResend: () -> Unit, onBack: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(Color.Red.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Henüz Doğrulanmadı", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onCheck,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FluentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Şimdi Tekrar Kontrol Et", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(onClick = onResend) {
                Text("Doğrulama Bağlantısını Yeniden Gönder", color = FluentBlue, fontWeight = FontWeight.Bold)
            }
            
            TextButton(onClick = onBack) {
                Text("Farklı bir e-posta dene", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
