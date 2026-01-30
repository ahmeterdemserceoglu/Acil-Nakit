package com.acilnakit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acilnakit.data.model.Review
import com.acilnakit.data.repository.UserProfile
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.AuthViewModel
import com.acilnakit.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    authViewModel: AuthViewModel,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var showNameDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadIdCard(it) }
    }

    if (showNameDialog) {
        var tempName by remember { mutableStateOf(profile?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("İsim Değiştir") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Yeni İsim") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateName(tempName)
                    showNameDialog = false
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PROFILIM", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Çıkış Yap", tint = Color.Red.copy(alpha = 0.7f))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (profile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FluentBlue)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Gradient Aura
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(FluentBlue.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(10.dp)) }

                    // Profile Content
                    item {
                        PremiumProfileHeader(
                            profile = profile!!,
                            onUpdateName = { showNameDialog = true }
                        )
                    }

                    item {
                        PremiumStatsBoard(profile!!)
                    }

                    item {
                        PremiumCampusSection(profile!!)
                    }

                    item {
                        PremiumVerificationSection(
                            profile = profile!!,
                            onUploadClick = { imagePickerLauncher.launch("image/*") },
                            uploadProgress = uploadProgress
                        )
                    }

                    item {
                        PremiumActionsSection(onNavigateToHistory)
                    }

                    item {
                        AchievementSection(profile!!.badges)
                    }

                    if (reviews.isNotEmpty()) {
                        item {
                            ReviewCarouselSection(reviews)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }
        }
    }
}

@Composable
fun PremiumProfileHeader(profile: UserProfile, onUpdateName: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar with Glow & Border
        Box(contentAlignment = Alignment.Center) {
            // Glow Effect
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(FluentBlue.copy(alpha = 0.2f), CircleShape)
                    .blur(15.dp)
            )
            
            // Main Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(FluentBlue, FluentBlue.copy(alpha = 0.6f))
                        )
                    )
                    .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person, 
                    contentDescription = null, 
                    modifier = Modifier.size(50.dp), 
                    tint = Color.White
                )
            }
            
            // Edit Badge mini button
            Surface(
                onClick = onUpdateName,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = FluentBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = profile.name.ifEmpty { "İsimsiz Kullanıcı" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = profile.department.ifEmpty { "Öğrenci" }.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))
        
        PremiumRatingBadge(profile.rating)
    }
}

@Composable
fun PremiumRatingBadge(rating: Double) {
    Surface(
        color = Color(0xFFFFD700).copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$rating Skoru",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB8860B)
            )
        }
    }
}

@Composable
fun PremiumStatsBoard(profile: UserProfile) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItemEnhanced("Tamamlanan", profile.completedTasks.toString(), Icons.Default.TaskAlt, Color(0xFF4CAF50))
            VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp).background(Color.Gray.copy(alpha = 0.1f)))
            StatItemEnhanced("Yayınlanan", profile.publishedTasks.toString(), Icons.Default.Campaign, FluentBlue)
            VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp).background(Color.Gray.copy(alpha = 0.1f)))
            StatItemEnhanced("Güven", "${(profile.rating * 20).toInt()}%", Icons.Default.Shield, Color(0xFFFF9800))
        }
    }
}

@Composable
fun StatItemEnhanced(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PremiumCampusSection(profile: UserProfile) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "KAMPÜS BİLGİLERİ", 
            style = MaterialTheme.typography.labelLarge, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CampusInfoItem(Icons.Default.School, "Üniversite", profile.school.ifEmpty { "Seçilmedi" })
                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = Color.Gray.copy(alpha = 0.05f))
                CampusInfoItem(Icons.Default.LocationOn, "Kampüs", profile.campus.ifEmpty { "Seçilmedi" })
            }
        }
    }
}

@Composable
fun CampusInfoItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(FluentBlue.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = FluentBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AchievementSection(badgeNames: List<String>) {
    val badgeMap = mapOf(
        "Güvenilir" to BadgeData("Güvenilir", Icons.Default.VerifiedUser, Color(0xFF4CAF50)),
        "Hızlı" to BadgeData("Hızlı", Icons.Default.Bolt, Color(0xFFFFC107)),
        "Uzman" to BadgeData("Uzman", Icons.Default.School, Color(0xFF2196F3)),
        "Elite Tasker" to BadgeData("Elite Tasker", Icons.Default.MilitaryTech, Color(0xFFFFD700)),
        "Gece Kuşu" to BadgeData("Gece Kuşu", Icons.Default.NightsStay, Color(0xFF9C27B0))
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "BAŞARI ROZETLERİ", 
            style = MaterialTheme.typography.labelLarge, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        if (badgeNames.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Henüz rozet kazanılmadı. Görev tamamlayarak kazanmaya başla!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(badgeNames) { name ->
                    badgeMap[name]?.let { PremiumBadgeItem(it) }
                }
            }
        }
    }
}

@Composable
fun PremiumBadgeItem(badge: BadgeData) {
    Surface(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = badge.color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, badge.color.copy(alpha = 0.15f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(badge.color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(badge.icon, contentDescription = null, tint = badge.iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = badge.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = badge.iconColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PremiumActionsSection(onHistoryClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "İŞLEMLER", 
            style = MaterialTheme.typography.labelLarge, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHistoryClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(FluentBlue.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = FluentBlue, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Görev Geçmişim", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos, 
                    contentDescription = null, 
                    modifier = Modifier.size(12.dp), 
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun ReviewCarouselSection(reviews: List<com.acilnakit.data.model.Review>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "REFERANSLAR & YORUMLAR", 
            style = MaterialTheme.typography.labelLarge, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(reviews) { review ->
                ReviewCard(review)
            }
        }
    }
}

@Composable
fun ReviewCard(review: com.acilnakit.data.model.Review) {
    Surface(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < review.rating) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = null,
                            tint = if (index < review.rating) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "Ref #${review.taskId.takeLast(4)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "\"${review.comment}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).background(FluentBlue.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = FluentBlue, modifier = Modifier.size(12.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Doğrulanmış Kullanıcı",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PremiumVerificationSection(
    profile: UserProfile,
    onUploadClick: () -> Unit,
    uploadProgress: Float?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "KİMLİK DOĞRULAMA", 
            style = MaterialTheme.typography.labelLarge, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = profile.verificationStatus != "pending" && !profile.isVerified) { 
                    onUploadClick() 
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = when {
                                profile.isVerified -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                profile.verificationStatus == "pending" -> FluentBlue.copy(alpha = 0.1f)
                                else -> Color.Gray.copy(alpha = 0.05f)
                            },
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            profile.isVerified -> Icons.Default.Verified
                            profile.verificationStatus == "pending" -> Icons.Default.Timer
                            else -> Icons.Default.UploadFile
                        },
                        contentDescription = null,
                        tint = when {
                            profile.isVerified -> Color(0xFF4CAF50)
                            profile.verificationStatus == "pending" -> FluentBlue
                            else -> Color.Gray
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    val title = when {
                        profile.isVerified -> "Kimlik Doğrulandı"
                        profile.verificationStatus == "pending" -> "Onay Bekliyor"
                        profile.verificationStatus == "rejected" -> "Reddedildi (Tekrar Yükle)"
                        else -> "Öğrenci Kimliğini Yükle"
                    }
                    val subtitle = when {
                        profile.isVerified -> "Tüm ayrıcalıklara sahipsiniz."
                        profile.verificationStatus == "pending" -> "Kimliğiniz inceleniyor."
                        else -> "Mavi tik almak için kartını yükle."
                    }
                    
                    Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (uploadProgress != null) {
                    CircularProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.size(24.dp),
                        color = FluentBlue,
                        strokeWidth = 3.dp
                    )
                } else if (!profile.isVerified && profile.verificationStatus != "pending") {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

data class BadgeData(val name: String, val icon: ImageVector, val iconColor: Color, val color: Color = iconColor)

