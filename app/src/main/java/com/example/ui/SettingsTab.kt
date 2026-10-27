package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.PrayerTimeCalculator
import com.example.viewmodel.DeenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val calcMethod by viewModel.calcMethod.collectAsState()
    val asrMethod by viewModel.asrMethod.collectAsState()
    val azanAlertsEnabled by viewModel.azanAlertsEnabled.collectAsState()

    var showCalcMethodMenu by remember { mutableStateOf(false) }
    var showAsrMethodMenu by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Indiviudal prayer notification toggle states simulated locally for details
    var alertFajr by remember { mutableStateOf(true) }
    var alertDhuhr by remember { mutableStateOf(true) }
    var alertAsr by remember { mutableStateOf(true) }
    var alertMaghrib by remember { mutableStateOf(true) }
    var alertIsha by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "App Settings & Calculations",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF3C63F)
            )
        )

        // General Azan Alerts global Master toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("general_alerts_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
            border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
            shape = RoundedCornerShape(12.dp)
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
                    Icon(
                        imageVector = if (azanAlertsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (azanAlertsEnabled) Color(0xFFF3C63F) else Color.Gray
                    )
                    Column {
                        Text(
                            text = "Automated Azan Alerts",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (azanAlertsEnabled) "Enabled for active times" else "All prayer alarms disabled",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                        )
                    }
                }
                Switch(
                    checked = azanAlertsEnabled,
                    onCheckedChange = { viewModel.toggleAzanAlerts() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFF3C63F),
                        checkedTrackColor = Color(0xFF0E5E3A),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF0A2218)
                    ),
                    modifier = Modifier.testTag("master_alerts_switch")
                )
            }
        }

        // Individual prayer alert toggles
        if (azanAlertsEnabled) {
            Text(
                text = "Individual Prayers Alarms",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3C63F)
                )
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
                border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val alertList = listOf(
                        Quadruple("Fajr (Dawn)", alertFajr, { value: Boolean -> alertFajr = value }, Icons.Default.WbTwilight),
                        Quadruple("Dhuhr (Noon)", alertDhuhr, { value: Boolean -> alertDhuhr = value }, Icons.Default.WbSunny),
                        Quadruple("Asr (Afternoon)", alertAsr, { value: Boolean -> alertAsr = value }, Icons.Default.WbSunny),
                        Quadruple("Maghrib (Sunset)", alertMaghrib, { value: Boolean -> alertMaghrib = value }, Icons.Default.WbTwilight),
                        Quadruple("Isha (Night)", alertIsha, { value: Boolean -> alertIsha = value }, Icons.Default.NightsStay)
                    )

                    alertList.forEachIndexed { idx, (name, state, onStateChange, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Text(name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(
                                checked = state,
                                onCheckedChange = onStateChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFF3C63F),
                                    checkedTrackColor = Color(0xFF0E5E3A)
                                )
                            )
                        }

                        if (idx < alertList.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color(0x220E5E3A)
                            )
                        }
                    }
                }
            }
        }

        // Calculation Methods selectors
        Text(
            text = "Calculation Settings",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF3C63F)
            )
        )

        // 1. Calculation Method Card Menu
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
            border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Prayer Calculation Convention",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    Button(
                        onClick = { showCalcMethodMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5E3A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_method_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(calcMethod.desc, color = Color.White)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFFF3C63F))
                        }
                    }

                    DropdownMenu(
                        expanded = showCalcMethodMenu,
                        onDismissRequest = { showCalcMethodMenu = false },
                        modifier = Modifier
                            .background(Color(0xFF0A2218))
                            .border(1.dp, Color(0xFF0E5E3A))
                    ) {
                        PrayerTimeCalculator.CalculationMethod.values().forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.desc, color = Color.White) },
                                onClick = {
                                    viewModel.setCalcMethod(method)
                                    showCalcMethodMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Asr Jurisprudence Method
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
            border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Asr Shadow Jurisprudence",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    Button(
                        onClick = { showAsrMethodMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5E3A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("asr_method_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(asrMethod.desc, color = Color.White)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFFF3C63F))
                        }
                    }

                    DropdownMenu(
                        expanded = showAsrMethodMenu,
                        onDismissRequest = { showAsrMethodMenu = false },
                        modifier = Modifier
                            .background(Color(0xFF0A2218))
                            .border(1.dp, Color(0xFF0E5E3A))
                    ) {
                        PrayerTimeCalculator.AsrMethod.values().forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.desc, color = Color.White) },
                                onClick = {
                                    viewModel.setAsrMethod(method)
                                    showAsrMethodMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Ethical monetization details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0x33F3C63F)),
            border = BorderStroke(1.dp, Color(0xFFF3C63F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFFF3C63F))
                    Text(
                        text = "Ethical & Ad-Free Guarantee",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3C63F)
                        )
                    )
                }
                Text(
                    text = "DeenPluse does not contain ads, popups, or user telemetry trackers. Our services are fully supported by optional community grants, voluntary donations, and subscription support without locking daily worship utilities. Your spiritual focus is fully protected.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.LightGray,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Simple Quadruple data class helper
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
