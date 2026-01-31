package com.acilnakit.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.acilnakit.ui.designsystem.GlassCard
import com.acilnakit.ui.theme.FluentBlue
import com.acilnakit.ui.viewmodel.AuthViewModel

data class University(
    val name: String,
    val logoUrl: String,
    val campuses: List<String>,
    val primaryColor: Long = 0xFF0078D4 // Default FluentBlue
)

@Composable
fun CampusSelectionScreen(
    onProfileComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(1) } // 1: School, 2: Campus, 3: Name
    var selectedSchool by remember { mutableStateOf<University?>(null) }
    var selectedCampus by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }

    val universities = remember {
        listOf(
            University("İstanbul Teknik Üniversitesi", "https://upload.wikimedia.org/wikipedia/tr/d/df/İtü_logo.png", listOf("Ayazağa", "Maçka", "Taşkışla", "Gümüşsuyu", "Tuzla"), 0xFF0038A8),
            University("Boğaziçi Üniversitesi", "https://upload.wikimedia.org/wikipedia/tr/a/a2/Bogazici_Universitesi_Logo.png", listOf("Güney", "Kuzey", "Hisar", "Uçaksavar", "Sarıtepe"), 0xFF003A70),
            University("Orta Doğu Teknik Üniversitesi", "https://upload.wikimedia.org/wikipedia/tr/b/b9/Odtü_logo.png", listOf("Merkez Yerleşke"), 0xFFE30A17),
            University("Yıldız Teknik Üniversitesi", "https://upload.wikimedia.org/wikipedia/tr/5/5e/Yıldız_Teknik_Üniversitesi_Logosu.png", listOf("Davutpaşa", "Beşiktaş"), 0xFF003D7C),
            University("Hacettepe Üniversitesi", "https://upload.wikimedia.org/wikipedia/tr/2/23/Hacettepe_Üniversitesi_Logosu.png", listOf("Beytepe", "Sıhhiye"), 0xFFDA291C),
            University("Koç Üniversitesi", "https://upload.wikimedia.org/wikipedia/tr/0/05/Koç_Üniversitesi_logosu.png", listOf("Rumelifeneri", "Batı", "İstinye", "Nişantaşı"), 0xFFAA182D),
            University("Bilkent Üniversitesi", "https://upload.wikimedia.org/wikipedia/tr/b/bf/Bilkent_Üniversitesi_Logosu.png", listOf("Merkez", "Doğu"), 0xFF004F9F),
            University("İstanbul Üniversitesi", "https://upload.wikimedia.org/wikipedia/tr/d/d3/İstanbul_Üniversitesi_logosu.png", listOf("Beyazıt", "Laleli", "Vezneciler"), 0xFF00563F),
            University("Ankara Üniversitesi", "https://upload.wikimedia.org/wikipedia/tr/0/0e/Ankara_Üniversitesi_Logosu.png", listOf("Tandoğan", "Beşevler", "Cebeci", "Gölbaşı"), 0xFF003F7F),
            University("Gazi Üniversitesi", "https://upload.wikimedia.org/wikipedia/tr/7/77/Gazi_Üniversitesi_Logosu.png", listOf("Merkez", "Maltepe", "Gölbaşı"), 0xFF002F6C)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFF), Color(0xFFE0E7FF))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Progress Indicator
            OnboardingProgress(currentStep)
            
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step_animation"
            ) { step ->
                when (step) {
                    1 -> SchoolSelectionStep(universities) { 
                        selectedSchool = it
                        currentStep = 2
                    }
                    2 -> CampusSelectionStep(
                        schoolName = selectedSchool?.name ?: "",
                        campuses = selectedSchool?.campuses ?: emptyList(),
                        onBack = { currentStep = 1 },
                        onSelect = {
                            selectedCampus = it
                            currentStep = 3
                        }
                    )
                    3 -> NameEntryStep(
                        onBack = { currentStep = 2 },
                        onComplete = {
                            userName = it
                            viewModel.updateProfile(userName, selectedSchool?.name ?: "", selectedCampus)
                            onProfileComplete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingProgress(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val step = index + 1
            val isActive = step <= currentStep
            Box(
                modifier = Modifier
                    .size(if (step == currentStep) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) FluentBlue else Color.LightGray)
            )
            if (index < 2) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(if (step < currentStep) FluentBlue else Color.LightGray)
                )
            }
        }
    }
}

@Composable
fun SchoolSelectionStep(
    universities: List<University>,
    onSelect: (University) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Üniversiteni Seç",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E)
        )
        Text(
            text = "Sana en yakın görevleri bulmak için.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxHeight(0.85f)
        ) {
            items(universities) { university ->
                SchoolItem(university) { onSelect(university) }
            }
        }
    }
}

@Composable
fun SchoolItem(university: University, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = university.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = university.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun CampusSelectionStep(
    schoolName: String,
    campuses: List<String>,
    onBack: () -> Unit,
    onSelect: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Text("Geri", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Yerleşkeni Seç",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = schoolName,
            style = MaterialTheme.typography.bodyMedium,
            color = FluentBlue
        )
        Spacer(modifier = Modifier.height(32.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(campuses) { campus ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(campus) }
                ) {
                    Text(
                        text = campus,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NameEntryStep(
    onBack: () -> Unit,
    onComplete: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Text("Geri", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Seninle Tanışalım",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sana nasıl hitap edelim?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(40.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Adın Soyadın", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White.copy(alpha = 0.5f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
                focusedIndicatorColor = FluentBlue,
                cursorColor = FluentBlue
            )
        )

        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { if (name.isNotBlank()) onComplete(name) },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
        ) {
            Text("Kaydı Tamamla", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
