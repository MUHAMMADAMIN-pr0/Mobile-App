package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.MosquePlace
import com.example.viewmodel.DeenViewModel
import com.example.viewmodel.MosqueSearchStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

enum class ViewMode {
    MAP, LIST
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MosqueLocatorTab(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val mosques by viewModel.nearbyMosques.collectAsState()
    val searchStatus by viewModel.mosqueSearchStatus.collectAsState()
    val searchRadius by viewModel.searchRadiusKm.collectAsState()
    val isGpsActive by viewModel.isGpsActive.collectAsState()
    val gpsLocation by viewModel.gpsLocation.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()

    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var selectedMosque by remember { mutableStateOf<MosquePlace?>(null) }

    // Location Permission Manager via Accompanist
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Sync GPS State on permission grant
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted && isGpsActive) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.updateGpsLocation(loc)
                    }
                }
            } catch (e: SecurityException) {
                // permission revoked or security issue
            }
        }
    }

    // Active center coordinates (GPS or Selected City fallback)
    val activeLat = if (isGpsActive) gpsLocation?.latitude ?: selectedCity.latitude else selectedCity.latitude
    val activeLng = if (isGpsActive) gpsLocation?.longitude ?: selectedCity.longitude else selectedCity.longitude
    val activeCenterName = if (isGpsActive) "Current GPS Position" else selectedCity.name

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF051E13))
            .padding(16.dp)
    ) {
        // --- 1. Quick Filters & Control Panels ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("locator_controls_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header details
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Mosque Locator",
                            color = Color(0xFFF3C63F),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Searching around: $activeCenterName",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    IconButton(
                        onClick = { viewModel.fetchNearbyMosques(activeLat, activeLng, force = true) },
                        modifier = Modifier.background(Color(0xFF0E5E3A), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh nearby search",
                            tint = Color(0xFFF3C63F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // GPS & Radius selectors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // GPS Filter chip
                    FilterChip(
                        selected = isGpsActive,
                        onClick = {
                            if (!isGpsActive) {
                                if (locationPermissionsState.allPermissionsGranted) {
                                    try {
                                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                            viewModel.toggleGps(true, loc)
                                        }
                                    } catch (e: SecurityException) {
                                        viewModel.toggleGps(true, null)
                                    }
                                } else {
                                    locationPermissionsState.launchMultiplePermissionRequest()
                                }
                            } else {
                                viewModel.toggleGps(false)
                            }
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isGpsActive) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isGpsActive) Color(0xFFF3C63F) else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GPS")
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0E5E3A),
                            selectedLabelColor = Color(0xFFF3C63F),
                            containerColor = Color(0xFF143A2A),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("gps_toggle_chip")
                    )

                    // Radius filters
                    listOf(2, 5, 10, 20).forEach { km ->
                        FilterChip(
                            selected = searchRadius == km,
                            onClick = { viewModel.setSearchRadius(km) },
                            label = { Text("${km}km") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF3C63F),
                                selectedLabelColor = Color(0xFF0A2218),
                                containerColor = Color(0xFF143A2A),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("radius_chip_${km}k")
                        )
                    }
                }
            }
        }

        // --- 2. Informational Key warning banner if api key is missing ---
        if (searchStatus is MosqueSearchStatus.Error && (searchStatus as MosqueSearchStatus.Error).isFallback) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E240D)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, Color(0xFFF3C63F).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Offline Mode Indicator",
                        tint = Color(0xFFF3C63F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Aesthetic Offline Fallback Mode Active",
                            color = Color(0xFFF3C63F),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "We are showing historically accurate authentic mosques. Add a valid Maps API Key in AI Studio Secrets to enable dynamic GPS-guided Places fetching.",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // --- 3. View Switcher Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Results (${mosques.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            // Pill-styled view switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0A2218))
                    .border(1.dp, Color(0xFF0E5E3A), RoundedCornerShape(20.dp))
                    .padding(2.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (viewMode == ViewMode.LIST) Color(0xFF0E5E3A) else Color.Transparent)
                        .clickable { viewMode = ViewMode.LIST }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("list_mode_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = if (viewMode == ViewMode.LIST) Color(0xFFF3C63F) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "List",
                            color = if (viewMode == ViewMode.LIST) Color(0xFFF3C63F) else Color.Gray,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (viewMode == ViewMode.MAP) Color(0xFF0E5E3A) else Color.Transparent)
                        .clickable { viewMode = ViewMode.MAP }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("map_mode_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = if (viewMode == ViewMode.MAP) Color(0xFFF3C63F) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Map",
                            color = if (viewMode == ViewMode.MAP) Color(0xFFF3C63F) else Color.Gray,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // --- 4. Content Area (Map or List) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (viewMode) {
                ViewMode.LIST -> {
                    if (searchStatus is MosqueSearchStatus.Loading && mosques.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFF3C63F))
                        }
                    } else if (mosques.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No mosques found nearby.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(mosques) { mosque ->
                                MosqueListItem(
                                    mosque = mosque,
                                    onSelect = { selectedMosque = mosque },
                                    onDirectionsClick = { launchDirectionsIntent(context, mosque) }
                                )
                            }
                        }
                    }
                }

                ViewMode.MAP -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MosqueMapView(
                            mosques = mosques,
                            centerLat = activeLat,
                            centerLng = activeLng,
                            onMosqueSelected = { selectedMosque = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )

                        // Floating details card when marker clicked on map
                        androidx.compose.animation.AnimatedVisibility(
                            visible = selectedMosque != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            selectedMosque?.let { mosque ->
                                MosqueMapSelectedCard(
                                    mosque = mosque,
                                    onClose = { selectedMosque = null },
                                    onDirectionsClick = { launchDirectionsIntent(context, mosque) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MosqueListItem(
    mosque: MosquePlace,
    onSelect: () -> Unit,
    onDirectionsClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("mosque_card_${mosque.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-contrast custom icon badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0E5E3A))
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Mosque pin icon",
                    tint = Color(0xFFF3C63F),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mosque.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating star",
                        tint = Color(0xFFF3C63F),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${mosque.rating} (${mosque.ratingCount} reviews)",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = mosque.address,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedDistance = if (mosque.distanceMeters >= 1000) {
                        String.format(java.util.Locale.US, "%.1f km", mosque.distanceMeters / 1000f)
                    } else {
                        "${mosque.distanceMeters} m"
                    }
                    val distanceColor = if (mosque.distanceMeters < 1000) Color(0xFF66BB6A) else Color(0xFF81C784)

                    // Proximity Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(distanceColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formattedDistance,
                            color = distanceColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Open status badge
                    if (mosque.openNow != null) {
                        val openColor = if (mosque.openNow) Color(0xFF66BB6A) else Color(0xFFEF5350)
                        val openText = if (mosque.openNow) "OPEN" else "CLOSED"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(openColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = openText,
                                color = openColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onDirectionsClick,
                modifier = Modifier
                    .background(Color(0xFF0E5E3A), CircleShape)
                    .size(36.dp)
                    .testTag("directions_button_${mosque.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Get directions",
                    tint = Color(0xFFF3C63F),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MosqueMapSelectedCard(
    mosque: MosquePlace,
    onClose: () -> Unit,
    onDirectionsClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF0E5E3A), RoundedCornerShape(16.dp))
            .testTag("mosque_detail_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = mosque.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close detailed view",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = mosque.address,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF3C63F),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${mosque.rating} (${mosque.ratingCount} reviews)",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val formattedDistance = if (mosque.distanceMeters >= 1000) {
                    String.format(java.util.Locale.US, "%.1f km", mosque.distanceMeters / 1000f)
                } else {
                    "${mosque.distanceMeters} m"
                }

                Text(
                    text = "Distance: $formattedDistance",
                    color = Color(0xFFF3C63F),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDirectionsClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5E3A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        tint = Color(0xFFF3C63F)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Get Directions & Route",
                        color = Color(0xFFF3C63F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MosqueMapView(
    mosques: List<MosquePlace>,
    centerLat: Double,
    centerLng: Double,
    onMosqueSelected: (MosquePlace) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapView = rememberMapViewWithLifecycle()
    var googleMapState by remember { mutableStateOf<GoogleMap?>(null) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            googleMapState = map
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isCompassEnabled = true
        }
    }

    LaunchedEffect(googleMapState, mosques, centerLat, centerLng) {
        val map = googleMapState ?: return@LaunchedEffect
        map.clear()

        val center = LatLng(centerLat, centerLng)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13.5f))

        // Center search point (Azure pin)
        map.addMarker(
            MarkerOptions()
                .position(center)
                .title("Search Center")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        // Mosques
        mosques.forEach { mosque ->
            val pos = LatLng(mosque.latitude, mosque.longitude)
            val hue = if (mosque.openNow == true) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
            val marker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(mosque.name)
                    .snippet("${mosque.rating} ⭐ - ${mosque.address}")
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
            )
            marker?.tag = mosque
        }

        map.setOnMarkerClickListener { marker ->
            val mosque = marker.tag as? MosquePlace
            if (mosque != null) {
                onMosqueSelected(mosque)
                marker.showInfoWindow()
                true
            } else {
                false
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    return mapView
}

fun launchDirectionsIntent(context: Context, mosque: MosquePlace) {
    // 1. Google navigation intent for turn-by-turn routing
    val gmmIntentUri = Uri.parse("google.navigation:q=${mosque.latitude},${mosque.longitude}")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")

    // 2. Direct web maps directions link as robust fallback
    val webDirectionsUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${mosque.latitude},${mosque.longitude}")
    val webIntent = Intent(Intent.ACTION_VIEW, webDirectionsUri)

    try {
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            context.startActivity(webIntent)
        }
    } catch (e: Exception) {
        // Safe global backup launcher
        try {
            context.startActivity(webIntent)
        } catch (ex: Exception) {
            // ignore completely
        }
    }
}
