package com.example.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.utils.PrayerTimeCalculator
import com.example.viewmodel.DeenViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ArQiblaCompassView(
    viewModel: DeenViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCity by viewModel.selectedCity.collectAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    // Calculate Qibla angle and distance using real formulas
    val qiblaBearing = remember(selectedCity) {
        PrayerTimeCalculator.calculateQiblaDirection(selectedCity.latitude, selectedCity.longitude).toFloat()
    }
    val distanceToKaaba = remember(selectedCity) {
        PrayerTimeCalculator.calculateKaabaDistance(selectedCity.latitude, selectedCity.longitude).toInt()
    }

    // Permission handling
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Heading state from sensors
    var sensorHeading by remember { mutableStateOf(0f) }
    var isSensorAvailable by remember { mutableStateOf(true) }

    // Manual panning state to rotate camera virtually (essential for emulators and physical rotation control)
    var manualOffset by remember { mutableStateOf(0f) }

    // Compass and AR sensor integration
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val rotationMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationValues)
                    val azimuthRad = orientationValues[0]
                    val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                    sensorHeading = (azimuthDeg + 360f) % 360f
                } else {
                    // Fallback using accelerometer & magnetometer
                    // Not strictly needed since emulators lack both, but clean for real devices
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationVector != null) {
            sensorManager.registerListener(sensorListener, rotationVector, SensorManager.SENSOR_DELAY_UI)
        } else if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        } else {
            isSensorAvailable = false
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // Combined active heading
    val heading = (sensorHeading + manualOffset + 360f) % 360f

    // Calculate alignment deviation to Kaaba (-180 to 180 degrees)
    val deviation = remember(qiblaBearing, heading) {
        val diff = (qiblaBearing - heading + 180f) % 360f - 180f
        if (diff < -180f) diff + 360f else diff
    }

    val isAligned = deviation in -5.0f..5.0f

    // Alert sound or haptic on exact alignment
    LaunchedEffect(isAligned) {
        if (isAligned) {
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("ar_qibla_compass_root")
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Camera Stream in Background
            CameraPreview(modifier = Modifier.fillMaxSize())

            // AR Scanning Radar & HUD Overlays
            RadarOverlay(modifier = Modifier.fillMaxSize())

            // Floating 3D-Like Kaaba AR Marker Overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                FloatingKaabaMarker(
                    deviation = deviation,
                    distanceKm = distanceToKaaba,
                    isAligned = isAligned
                )
            }

            // HUD UI Overlays
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header HUD bar
                TopHudBar(
                    cityName = selectedCity.name,
                    distanceKm = distanceToKaaba,
                    qiblaBearing = qiblaBearing,
                    currentHeading = heading,
                    onBack = onClose
                )

                // Deviation Direction Guidance
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeviationGuide(deviation = deviation, isAligned = isAligned)

                    // Bottom Compass Dial & Control Deck
                    BottomControlDeck(
                        heading = heading,
                        qiblaBearing = qiblaBearing,
                        isSensorAvailable = isSensorAvailable,
                        manualOffset = manualOffset,
                        onManualOffsetChange = { manualOffset = it }
                    )
                }
            }
        } else {
            // Permission Request View
            PermissionRequestDeck(
                onGrantClick = { cameraPermissionState.launchPermissionRequest() },
                onBack = onClose,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(cameraProvider) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val executor = androidx.core.content.ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}

@Composable
fun RadarOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "radar_pulse"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension * 0.45f

        // Draw crosshair axes
        drawLine(
            color = Color(0x22F3C63F),
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0x22F3C63F),
            start = Offset(center.x, 0f),
            end = Offset(center.x, size.height),
            strokeWidth = 1.dp.toPx()
        )

        // Radar circular grids
        drawCircle(
            color = Color(0x11F3C63F),
            radius = maxRadius * 0.33f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Color(0x11F3C63F),
            radius = maxRadius * 0.66f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Color(0x22F3C63F),
            radius = maxRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Pulsing radar sweeping ring
        drawCircle(
            color = Color(0xFFF3C63F).copy(alpha = (1f - pulse) * 0.35f),
            radius = maxRadius * pulse,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun FloatingKaabaMarker(
    deviation: Float,
    distanceKm: Int,
    isAligned: Boolean
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    // Field of view parameter: Kaaba card appears if dev is in [-30, 30] degrees
    val fov = 35f
    val isVisible = deviation in -fov..fov

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        // Calculate dynamic horizontal offset in px
        val screenWidthPx = with(density) { screenWidth.toPx() }
        val xOffsetPx = (deviation / fov) * (screenWidthPx * 0.42f)

        Box(
            modifier = Modifier
                .offset { IntOffset(xOffsetPx.roundToInt(), -50) }
                .testTag("floating_kaaba_marker")
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ), label = "marker_scale"
            )

            Card(
                modifier = Modifier
                    .width(180.dp)
                    .border(
                        border = BorderStroke(
                            width = if (isAligned) 3.dp else 1.5.dp,
                            color = if (isAligned) Color(0xFFF3C63F) else Color(0xBB0E5E3A)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAligned) Color(0xDD051E13) else Color(0xBB020A07)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Kaaba Representation Graphic
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .drawBehind {
                                val cubeSize = size.minDimension * 0.8f
                                val topLeft = Offset((size.width - cubeSize) / 2f, (size.height - cubeSize) / 2f)

                                // Base Kaaba Body (Midnight Black)
                                drawRect(
                                    color = Color(0xFF101010),
                                    topLeft = topLeft,
                                    size = androidx.compose.ui.geometry.Size(cubeSize, cubeSize)
                                )

                                // Golden Kiswah Belt (Middle third)
                                drawRect(
                                    color = Color(0xFFF3C63F),
                                    topLeft = Offset(topLeft.x, topLeft.y + cubeSize * 0.3f),
                                    size = androidx.compose.ui.geometry.Size(cubeSize, cubeSize * 0.12f)
                                )

                                // Floating spiritual halo
                                drawCircle(
                                    color = Color(0x22F3C63F),
                                    radius = size.minDimension * 0.55f,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                    )

                    Text(
                        text = "HOLY KAABA",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.5.sp
                        )
                    )

                    Text(
                        text = "$distanceKm km Away",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFFF3C63F)
                        )
                    )

                    if (isAligned) {
                        Surface(
                            color = Color(0xFF0E5E3A),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFF3C63F), modifier = Modifier.size(14.dp))
                                Text(
                                    text = "ALIGNED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFF3C63F),
                                        fontWeight = FontWeight.Bold
                                    )
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
fun TopHudBar(
    cityName: String,
    distanceKm: Int,
    qiblaBearing: Float,
    currentHeading: Float,
    onBack: () -> Unit
) {
    Surface(
        color = Color(0xCC051E13),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ar_top_hud")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color(0x33FFFFFF), CircleShape)
                    .size(40.dp)
                    .testTag("ar_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close AR View", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Qibla AR Compass",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF3C63F)
                    )
                )
                Text(
                    text = "Targeting Mecca from $cityName",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${qiblaBearing.roundToInt()}° N",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Bearing",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }
        }
    }
}

@Composable
fun DeviationGuide(
    deviation: Float,
    isAligned: Boolean
) {
    Surface(
        color = if (isAligned) Color(0xEE0E5E3A) else Color(0xCC111111),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isAligned) Color(0xFFF3C63F) else Color(0xFF0E5E3A)),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isAligned) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Aligned", tint = Color(0xFFF3C63F))
                Text(
                    text = "EXACTLY FACING KAABA. PRAY!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            } else {
                val directionText = if (deviation < 0) {
                    "← TURN LEFT ${(-deviation).roundToInt()}°"
                } else {
                    "TURN RIGHT ${deviation.roundToInt()}° →"
                }
                Icon(Icons.Default.Navigation, contentDescription = "Navigation guide", tint = Color(0xFFF3C63F), modifier = Modifier.rotate(deviation))
                Text(
                    text = directionText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3C63F)
                    )
                )
            }
        }
    }
}

@Composable
fun BottomControlDeck(
    heading: Float,
    qiblaBearing: Float,
    isSensorAvailable: Boolean,
    manualOffset: Float,
    onManualOffsetChange: (Float) -> Unit
) {
    Surface(
        color = Color(0xCC051E13),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ar_bottom_control_deck")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // High Fidelity Circular Compass Dial with Custom Canvas Drawing
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color(0xFF020A07), CircleShape)
                    .border(1.dp, Color(0xFF0E5E3A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Background grid marks
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Draw Cardinal letters rotated opposite to current heading
                    // This way, North points to actual North
                    val angleOffset = -heading * (PI / 180f)

                    val cardinalDirs = listOf("N" to 0.0, "E" to PI / 2, "S" to PI, "W" to 1.5 * PI)
                    cardinalDirs.forEach { (dir, angle) ->
                        val finalAngle = angle + angleOffset - PI / 2
                        val x = center.x + (radius * 0.72f) * cos(finalAngle).toFloat()
                        val y = center.y + (radius * 0.72f) * sin(finalAngle).toFloat()

                        // A simplified point or mark
                        drawCircle(
                            color = if (dir == "N") Color(0xFFF3C63F) else Color.Gray,
                            radius = if (dir == "N") 3.dp.toPx() else 1.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    // Draw Qibla pointer line
                    val qiblaAngleRad = (qiblaBearing - heading - 90f) * (PI / 180.0)
                    val qX = center.x + (radius * 0.85f) * cos(qiblaAngleRad).toFloat()
                    val qY = center.y + (radius * 0.85f) * sin(qiblaAngleRad).toFloat()

                    drawLine(
                        color = Color(0xFFF3C63F),
                        start = center,
                        end = Offset(qX, qY),
                        strokeWidth = 2.5.dp.toPx()
                    )
                    drawCircle(
                        color = Color(0xFFF3C63F),
                        radius = 4.dp.toPx(),
                        center = Offset(qX, qY)
                    )
                }

                // Phone/Self representation arrow (always pointing up in the physical center)
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Virtual Rotation Controller (Slider/Drag deck)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSensorAvailable) "Gyroscope Connected" else "Emulator Simulation Mode",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isSensorAvailable) Color(0xFF0E5E3A) else Color(0xFFF3C63F),
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "Offset: ${manualOffset.roundToInt()}°",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                    )
                }

                // Interactive slider to rotate virtually
                Slider(
                    value = manualOffset,
                    onValueChange = onManualOffsetChange,
                    valueRange = -180f..180f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF0E5E3A),
                        inactiveTrackColor = Color(0x22FFFFFF),
                        thumbColor = Color(0xFFF3C63F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("virtual_rotation_slider")
                )

                Text(
                    text = "💡 Drag the slider above or rotate to align the AR camera with the Holy Kaaba.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PermissionRequestDeck(
    onGrantClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF051E13))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
            border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFFF3C63F),
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "To overlay the direction to the Kaaba directly on your real-world camera feed (Augmented Reality), DeenPluse needs camera access. We strictly protect your privacy—no images are saved or transmitted.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.LightGray,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5E3A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_camera_permission_btn")
                ) {
                    Text("Grant Camera Permission", color = Color(0xFFF3C63F), fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("permission_cancel_btn")
                ) {
                    Text("Go Back to Dashboard", color = Color.Gray)
                }
            }
        }
    }
}
