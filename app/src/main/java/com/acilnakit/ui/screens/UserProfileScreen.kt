package com.acilnakit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acilnakit.data.model.Review
import com.acilnakit.data.repository.UserProfile
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kullanıcı Profili", fontWeight = FontWeight.Bold) },
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
                profile == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Kullanıcı bulunamadı", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            PublicProfileHeader(profile = profile!!)
                        }
                        
                        item {
                            TrustScoreCard(profile = profile!!)
                        }
                        
                        item {
                            PublicStatsCard(profile = profile!!)
                        }
                        
                        if (reviews.isNotEmpty()) {
                            item {
                                Text(
                                    "Değerlendirmeler (${reviews.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            
                            items(reviews.take(5)) { review ->
                                PublicReviewCard(review = review)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicProfileHeader(profile: UserProfile) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(FluentBlue, FluentBlue.copy(alpha = 0.6f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profile.name.take(2).uppercase(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                profile.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.School,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${profile.schoolName} - ${profile.campusName}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Rating Stars
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    val filled = index < profile.rating.toInt()
                    val half = index == profile.rating.toInt() && profile.rating % 1 >= 0.5
                    Icon(
                        when {
                            filled -> Icons.Default.Star
                            half -> Icons.Default.StarHalf
                            else -> Icons.Default.StarOutline
                        },
                        null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    String.format("%.1f", profile.rating),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB300)
                )
                Text(
                    " (${profile.reviewCount} değerlendirme)",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun TrustScoreCard(profile: UserProfile) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Güven Skoru", fontWeight = FontWeight.Bold)
                
                val trustLevel = when {
                    profile.trustScore >= 90 -> "Elit" to Color(0xFFFFD700)
                    profile.trustScore >= 70 -> "Güvenilir" to Color(0xFF4CAF50)
                    profile.trustScore >= 50 -> "Orta" to Color(0xFFFF9800)
                    else -> "Yeni" to Color.Gray
                }
                
                Surface(
                    color = trustLevel.second.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        trustLevel.first,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = trustLevel.second,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { (profile.trustScore.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    profile.trustScore >= 90 -> Color(0xFFFFD700)
                    profile.trustScore >= 70 -> Color(0xFF4CAF50)
                    profile.trustScore >= 50 -> Color(0xFFFF9800)
                    else -> Color.Gray
                },
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "${profile.trustScore.toInt()}/100",
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PublicStatsCard(profile: UserProfile) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PublicStatItem(
                icon = Icons.Default.TaskAlt,
                value = profile.completedTasks.toString(),
                label = "Tamamlanan",
                color = Color(0xFF4CAF50)
            )
            PublicStatItem(
                icon = Icons.Default.Create,
                value = profile.publishedTasks.toString(),
                label = "Yayınlanan",
                color = FluentBlue
            )
            PublicStatItem(
                icon = Icons.Default.CalendarMonth,
                value = "2024",
                label = "Katılım",
                color = Color(0xFF9C27B0)
            )
        }
    }
}

@Composable
fun PublicStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun PublicReviewCard(review: Review) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    repeat(5) { index ->
                        Icon(
                            if (index < review.rating.toInt()) Icons.Default.Star else Icons.Default.StarOutline,
                            null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    "Anonim",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            if (review.comment.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "\"${review.comment}\"",
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}
