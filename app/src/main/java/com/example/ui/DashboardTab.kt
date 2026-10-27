package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.utils.LunarCalculator
import com.example.utils.PrayerTimeCalculator
import com.example.viewmodel.City
import com.example.viewmodel.DeenViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTab(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCity by viewModel.selectedCity.collectAsState()
    val schedule by viewModel.prayerSchedule.collectAsState()
    val hijri by viewModel.hijriDate.collectAsState()
    val moon by viewModel.moonPhase.collectAsState()
    val isPlaying by viewModel.isAzanPlaying.collectAsState()
    
    var showArQiblaCompass by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (showArQiblaCompass) {
            ArQiblaCompassView(
                viewModel = viewModel,
                onClose = { showArQiblaCompass = false }
            )
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
        // Preset Cities Quick Picker
        Text(
            text = "Select Location",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFF3C63F))
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(viewModel.cities) { city ->
                val isSelected = city.name == selectedCity.name
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectCity(city) },
                    label = { Text(city.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0E5E3A),
                        selectedLabelColor = Color(0xFFF3C63F),
                        containerColor = Color(0xFF0A2218),
                        labelColor = Color.LightGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFF0E5E3A),
                        selectedBorderColor = Color(0xFFF3C63F),
                        selectedBorderWidth = 1.dp
                    ),
                    modifier = Modifier.testTag("city_chip_${city.name.lowercase().replace(" ", "_")}")
                )
            }
        }

        // Hero Banner Card with Hijri Date & Moon Phase Integration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .testTag("hero_banner_card"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background generated image
                Image(
                    painter = painterResource(id = R.drawable.img_deenpluse_banner),
                    contentDescription = "DeenPluse background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark emerald gradient overlay for premium styling
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xDD051E13)),
                                startY = 100f
                            )
                        )
                )

                // Banner Details
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0x33000000),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Current Location: ${selectedCity.name}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        // Active timezone display
                        Text(
                            text = "GMT ${if (selectedCity.timezoneOffset >= 0) "+" else ""}${selectedCity.timezoneOffset.toInt()}",
                            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFF3C63F))
                        )
                    }

                    Column {
                        Text(
                            text = "${hijri.day} ${hijri.monthName} ${hijri.year} AH",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3C63F)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
                        )
                    }
                }
            }
        }

        // Quick Stats row: Moon Phase drawing and Countdown to Next Prayer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Moon Phase Custom Tracker Widget
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .testTag("moon_phase_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF0E5E3A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Moon Phase",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    // Draw the physical moon with Canvas
                    MoonPhaseView(
                        fraction = moon.illuminatedFraction,
                        isWaxing = moon.isWaxing,
                        modifier = Modifier.size(64.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = moon.phaseName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3C63F)
                            )
                        )
                        Text(
                            text = "${(moon.illuminatedFraction * 100).toInt()}% Illumination",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }
                }
            }

            // Countdown to next prayer Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .testTag("countdown_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF0E5E3A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Next Prayer",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(70.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            color = Color(0xFF0E5E3A),
                            strokeWidth = 4.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFFF3C63F),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = schedule.nextPrayerName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3C63F)
                            )
                        )
                        Text(
                            text = "In ${schedule.nextPrayerTimeLeft}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Live Audio Azan Trigger Preview Screen Element
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("azan_audio_panel"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF0E5E3A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleAzanAudio() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF0E5E3A), CircleShape)
                            .testTag("play_azan_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop Azan" else "Play Azan",
                            tint = Color(0xFFF3C63F)
                        )
                    }
                    Column {
                        Text(
                            text = "Beautiful Azan Audio",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (isPlaying) "Streaming Peaceful Azan..." else "Tap to hear Azan preview",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                        )
                    }
                }

                // Waveform Equalizer simulation when playing
                if (isPlaying) {
                    WaveformEqualizer()
                } else {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Halal Scanner Section
        HalalScannerTab(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth()
        )

        // Interactive AR Qibla Compass Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ar_qibla_dashboard_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF0E5E3A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = Color(0xFFF3C63F)
                        )
                        Text(
                            text = "AR Qibla Compass",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Text(
                        text = "Scan your environment using high-tech Augmented Reality overlays to locate the Holy Kaaba instantly.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { showArQiblaCompass = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5E3A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("launch_ar_compass_btn")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFF3C63F), modifier = Modifier.size(16.dp))
                            Text("Launch AR Finder", color = Color(0xFFF3C63F), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                
                val qiblaBearing = remember(selectedCity) {
                    PrayerTimeCalculator.calculateQiblaDirection(selectedCity.latitude, selectedCity.longitude).toInt()
                }
                val distanceKm = remember(selectedCity) {
                    PrayerTimeCalculator.calculateKaabaDistance(selectedCity.latitude, selectedCity.longitude).toInt()
                }
                
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .background(Color(0x22051E13), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "QIBLA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "$qiblaBearing° N",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF3C63F)
                        )
                    )
                    HorizontalDivider(color = Color(0x11FFFFFF), modifier = Modifier.padding(vertical = 2.dp))
                    Text(
                        text = "$distanceKm km",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "to Makkah",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }
            }
        }

        // Prayer Times Detailed Table
        Text(
            text = "Today's Prayer Times",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFF3C63F))
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prayer_schedule_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF0E5E3A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                val prayers = listOf(
                    Triple("Fajr", schedule.fajr, Icons.Default.WbTwilight),
                    Triple("Sunrise", schedule.sunrise, Icons.Default.WbSunny),
                    Triple("Dhuhr", schedule.dhuhr, Icons.Default.WbSunny),
                    Triple("Asr", schedule.asr, Icons.Default.WbSunny),
                    Triple("Maghrib", schedule.maghrib, Icons.Default.WbTwilight),
                    Triple("Isha", schedule.isha, Icons.Default.NightsStay)
                )

                prayers.forEachIndexed { index, (name, time, icon) ->
                    val isNext = schedule.nextPrayerName == name || 
                                (schedule.nextPrayerName == "Fajr (Tomorrow)" && name == "Fajr")
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isNext) Color(0x220E5E3A) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isNext) Color(0xFFF3C63F) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isNext) Color.White else Color.LightGray
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isNext) {
                                Surface(
                                    color = Color(0xFF0E5E3A),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "CURRENT / NEXT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFF3C63F),
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNext) Color(0xFFF3C63F) else Color.White
                                )
                            )
                        }
                    }
                    
                    if (index < prayers.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0x330E5E3A)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
        }
    }
}

/**
 * Renders a gorgeous moon phase using custom canvas drawing.
 * Dynamically draws the shadow and the illuminated portion.
 */
@Composable
fun MoonPhaseView(
    fraction: Float,
    isWaxing: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2.0f
        val center = Offset(size.width / 2.0f, size.height / 2.0f)

        // 1. Draw the starry glow behind the moon
        drawCircle(
            color = Color(0x1AFFF7D6),
            radius = radius + 15f,
            center = center
        )

        // 2. Draw the dark base circle representing the dark side of the moon
        drawCircle(
            color = Color(0xFF152A20),
            radius = radius,
            center = center,
            style = Fill
        )

        // Draw craters on the dark side (aesthetic touch!)
        drawCircle(color = Color(0x11FFFFFF), radius = radius * 0.15f, center = Offset(center.x - radius * 0.4f, center.y - radius * 0.2f))
        drawCircle(color = Color(0x11FFFFFF), radius = radius * 0.12f, center = Offset(center.x + radius * 0.3f, center.y + radius * 0.3f))
        drawCircle(color = Color(0x11FFFFFF), radius = radius * 0.08f, center = Offset(center.x - radius * 0.1f, center.y + radius * 0.5f))

        // 3. Draw the illuminated part based on fraction and phase
        // If 100% illumination (Full Moon)
        if (fraction >= 0.98f) {
            drawCircle(
                color = Color(0xFFFFF7D6),
                radius = radius,
                center = center
            )
        } else if (fraction > 0.02f) {
            val path = Path()
            // Using standard clipping or geometric arc drawings for accurate phase
            if (isWaxing) {
                // Waxing starts on the right side
                if (fraction <= 0.5f) {
                    // Crescent: Draw full arc on right, inner crescent curve back
                    val sweepFactor = fraction * 2.0f // 0 to 1
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = true
                    )
                    // curve back using Bezier
                    path.quadraticTo(
                        center.x + radius * (1.0f - sweepFactor * 2f), center.y,
                        center.x, center.y - radius
                    )
                } else {
                    // Gibbous: Left side is partially dark, right side fully illuminated
                    val sweepFactor = (fraction - 0.5f) * 2.0f // 0 to 1
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = true
                    )
                    path.quadraticTo(
                        center.x - radius * sweepFactor, center.y,
                        center.x, center.y - radius
                    )
                }
            } else {
                // Waning starts on the left side
                if (fraction <= 0.5f) {
                    val sweepFactor = fraction * 2.0f
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = true
                    )
                    path.quadraticTo(
                        center.x - radius * (1.0f - sweepFactor * 2f), center.y,
                        center.x, center.y + radius
                    )
                } else {
                    val sweepFactor = (fraction - 0.5f) * 2.0f
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = true
                    )
                    path.quadraticTo(
                        center.x + radius * sweepFactor, center.y,
                        center.x, center.y + radius
                    )
                }
            }
            path.close()

            // Draw the computed glowing crescent path
            drawPath(
                path = path,
                color = Color(0xFFFFF4BF),
                style = Fill
            )
        }

        // Draw outer ring representing the edge shine
        drawCircle(
            color = Color(0xFFFFF4BF),
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/**
 * Beautiful animated sound equalizer simulation when playing Azan
 */
@Composable
fun WaveformEqualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 2f, targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "h4"
    )

    Row(
        modifier = Modifier
            .height(32.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(h1, h2, h3, h4).forEach { height ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .background(Color(0xFFF3C63F), RoundedCornerShape(2.dp))
            )
        }
    }
}
