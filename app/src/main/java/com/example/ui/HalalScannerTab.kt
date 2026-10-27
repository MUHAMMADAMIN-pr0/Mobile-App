package com.example.ui

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.HalalAnalysisResult
import com.example.data.ScannedProductEntity
import com.example.viewmodel.BarcodeLookupState
import com.example.viewmodel.DeenViewModel
import com.example.viewmodel.IngredientsAnalysisState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HalalScannerTab(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Scan, 1 = Manual Code, 2 = Ingredients Check
    val scannedHistory by viewModel.scannedProducts.collectAsState()
    
    var selectedProductForDetail by remember { mutableStateOf<ScannedProductEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Halal Guard Scanner",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3C63F)
                )
            )
            IconButton(
                onClick = { viewModel.clearScannedProducts() },
                modifier = Modifier.testTag("clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear History",
                    tint = Color.Gray
                )
            }
        }

        // Sub Tab Row
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = Color(0xFF0A2218),
            contentColor = Color(0xFFF3C63F),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                    color = Color(0xFFF3C63F)
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF0E5E3A), RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0; viewModel.clearIngredientsAnalysis() },
                text = { Text("Camera Scan", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1; viewModel.clearIngredientsAnalysis() },
                text = { Text("Code Lookup", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Dialpad, contentDescription = null) }
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = { Text("Ingredients", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.ListAlt, contentDescription = null) }
            )
        }

        // Main Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            when (activeSubTab) {
                0 -> CameraScanView(viewModel) { product -> selectedProductForDetail = product }
                1 -> ManualLookupView(viewModel) { product -> selectedProductForDetail = product }
                2 -> IngredientsCheckView(viewModel)
            }
        }

        // Scanned History Section
        if (scannedHistory.isNotEmpty() && activeSubTab != 2) {
            HorizontalDivider(color = Color(0xFF0E5E3A), thickness = 1.dp)
            Text(
                text = "Recent Scan History",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3C63F)
                )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scannedHistory.take(5).forEach { item ->
                    HistoryCard(
                        item = item,
                        onClick = { selectedProductForDetail = item },
                        onDelete = { viewModel.deleteScannedProduct(item.barcode) }
                    )
                }
            }
        }
    }

    // Product Detail Dialog
    if (selectedProductForDetail != null) {
        ProductDetailDialog(
            product = selectedProductForDetail!!,
            onDismiss = { selectedProductForDetail = null }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScanView(
    viewModel: DeenViewModel,
    onProductSelected: (ScannedProductEntity) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val lookupState by viewModel.barcodeLookupState.collectAsState()

    LaunchedEffect(lookupState) {
        if (lookupState is BarcodeLookupState.Success) {
            onProductSelected((lookupState as BarcodeLookupState.Success).product)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(450.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
        border = BorderStroke(1.dp, Color(0xFF0E5E3A))
    ) {
        if (cameraPermissionState.status.isGranted) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Live CameraX view
                HalalCameraPreview(
                    modifier = Modifier.fillMaxSize()
                )

                // High Contrast Viewfinder overlay
                ScannerOverlay(
                    modifier = Modifier.fillMaxSize()
                )

                // Quick Demo Scan Buttons for emulators/no camera testing
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color(0xE0051E13), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚡ NO PHYSICAL BARCODE? TRY AN INSTANT DEMO:",
                        color = Color(0xFFF3C63F),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.lookupBarcode("041220032549") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5E3A)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Cookies (Halal)", fontSize = 11.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { viewModel.lookupBarcode("4902430521802") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A2E2E)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Gelatin (Haram)", fontSize = 11.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { viewModel.lookupBarcode("737628064502") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC7801A)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Ramen (Doubtful)", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                if (lookupState is BarcodeLookupState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99051E13)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFFF3C63F))
                            Text("Analyzing product ingredients...", color = Color.White)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Permission Required",
                    tint = Color(0xFFF3C63F),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scan product barcodes in real-time to analyze ingredients instantly for Halal certification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3C63F), contentColor = Color(0xFF051E13))
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HalalCameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    // Ignore errors
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val translationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Dark translucent frame margins
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0x7F000000)))
            Row(modifier = Modifier.height(200.dp).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0x7F000000)))
                // Scan Square
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .border(2.dp, Color(0xFFF3C63F), RoundedCornerShape(12.dp))
                ) {
                    // Moving Laser Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(y = translationY.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFF3C63F),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0x7F000000)))
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0x7F000000)))
        }

        Text(
            text = "Center product barcode inside the frame",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = Color.White
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .background(Color(0x99000000), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ManualLookupView(
    viewModel: DeenViewModel,
    onProductSelected: (ScannedProductEntity) -> Unit
) {
    var barcodeInput by remember { mutableStateOf("") }
    val lookupState by viewModel.barcodeLookupState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(lookupState) {
        if (lookupState is BarcodeLookupState.Success) {
            onProductSelected((lookupState as BarcodeLookupState.Success).product)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
        border = BorderStroke(1.dp, Color(0xFF0E5E3A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Manual Barcode Look-up",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            OutlinedTextField(
                value = barcodeInput,
                onValueChange = { if (it.length <= 13) barcodeInput = it },
                label = { Text("Enter 12 or 13-digit Barcode") },
                placeholder = { Text("e.g. 737628064502") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (barcodeInput.length >= 8) {
                            viewModel.lookupBarcode(barcodeInput)
                            keyboardController?.hide()
                        }
                    }
                ),
                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (barcodeInput.isNotEmpty()) {
                        IconButton(onClick = { barcodeInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("barcode_input_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF3C63F),
                    unfocusedBorderColor = Color(0xFF0E5E3A),
                    focusedContainerColor = Color(0xFF051E13),
                    unfocusedContainerColor = Color(0xFF051E13),
                    focusedLabelColor = Color(0xFFF3C63F),
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Button(
                onClick = {
                    if (barcodeInput.length >= 8) {
                        viewModel.lookupBarcode(barcodeInput)
                        keyboardController?.hide()
                    }
                },
                enabled = barcodeInput.length >= 8 && lookupState !is BarcodeLookupState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF3C63F),
                    contentColor = Color(0xFF051E13),
                    disabledContainerColor = Color(0xFF0E5E3A)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("barcode_lookup_submit")
            ) {
                if (lookupState is BarcodeLookupState.Loading) {
                    CircularProgressIndicator(
                        color = Color(0xFF051E13),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify Barcode Status", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick lookup presets
            Text(
                text = "Popular Products Quick Presets:",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetProductChip(
                        name = "Nongshim Ramen",
                        barcode = "737628064502",
                        onClick = { barcodeInput = "737628064502"; viewModel.lookupBarcode("737628064502") },
                        modifier = Modifier.weight(1f)
                    )
                    PresetProductChip(
                        name = "Chips Ahoy!",
                        barcode = "041220032549",
                        onClick = { barcodeInput = "041220032549"; viewModel.lookupBarcode("041220032549") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetProductChip(
                        name = "Strawberry Gelatin",
                        barcode = "4902430521802",
                        onClick = { barcodeInput = "4902430521802"; viewModel.lookupBarcode("4902430521802") },
                        modifier = Modifier.weight(1f)
                    )
                    PresetProductChip(
                        name = "Organic Honey Oats",
                        barcode = "5012345678901",
                        onClick = { barcodeInput = "5012345678901"; viewModel.lookupBarcode("5012345678901") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PresetProductChip(
    name: String,
    barcode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("preset_chip_$barcode"),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF051E13))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF3C63F),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = barcode,
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun IngredientsCheckView(
    viewModel: DeenViewModel
) {
    var ingredientsInput by remember { mutableStateOf("") }
    val analysisState by viewModel.ingredientsAnalysisState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
        border = BorderStroke(1.dp, Color(0xFF0E5E3A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Pasted Ingredients Analyzer",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            OutlinedTextField(
                value = ingredientsInput,
                onValueChange = { ingredientsInput = it },
                label = { Text("Paste Ingredient List Text") },
                placeholder = { Text("e.g., Wheat flour, sugar, gelatin (pork), soy lecithin, natural flavors, cochineal E120, food coloring...") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ingredients_input_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF3C63F),
                    unfocusedBorderColor = Color(0xFF0E5E3A),
                    focusedContainerColor = Color(0xFF051E13),
                    unfocusedContainerColor = Color(0xFF051E13),
                    focusedLabelColor = Color(0xFFF3C63F),
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        ingredientsInput = "Enriched flour, sugar, palm oil, soybean oil, cocoa butter, milk solids, pork gelatine, citric acid, natural vanilla extract, whey powder."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF051E13)),
                    border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text("Preset Haram", fontSize = 11.sp, color = Color.LightGray)
                }
                Button(
                    onClick = {
                        ingredientsInput = "Unbleached wheat flour, cane sugar, organic palm oil, organic honey, honey powder, baking soda, natural vanilla, sea salt."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF051E13)),
                    border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text("Preset Halal", fontSize = 11.sp, color = Color.LightGray)
                }
                Button(
                    onClick = {
                        ingredientsInput = "Wheat flour, palm oil, salt, cheese powder, whey, monosodium glutamate, emulsifier (E471), colors, artificial flavors."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF051E13)),
                    border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text("Preset Doubtful", fontSize = 11.sp, color = Color.LightGray)
                }
            }

            Button(
                onClick = {
                    if (ingredientsInput.isNotBlank()) {
                        viewModel.analyzeIngredients(ingredientsInput)
                        keyboardController?.hide()
                    }
                },
                enabled = ingredientsInput.isNotBlank() && analysisState !is IngredientsAnalysisState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF3C63F),
                    contentColor = Color(0xFF051E13),
                    disabledContainerColor = Color(0xFF0E5E3A)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ingredients_submit_button")
            ) {
                if (analysisState is IngredientsAnalysisState.Loading) {
                    CircularProgressIndicator(
                        color = Color(0xFF051E13),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Ingredients with Gemini AI", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Analysis Result Display
            AnimatedContent(
                targetState = analysisState,
                label = "analysis_result_transition"
            ) { state ->
                when (state) {
                    is IngredientsAnalysisState.Success -> {
                        IngredientsAnalysisCard(result = state.result)
                    }
                    is IngredientsAnalysisState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0x339A2E2E)),
                            border = BorderStroke(1.dp, Color(0xFF9A2E2E))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFE57373))
                                Text(state.message, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun IngredientsAnalysisCard(result: HalalAnalysisResult) {
    val statusColor = when (result.status.uppercase()) {
        "HALAL" -> Color(0xFF388E3C)
        "HARAM" -> Color(0xFFD32F2F)
        else -> Color(0xFFF57C00)
    }

    val statusIcon = when (result.status.uppercase()) {
        "HALAL" -> Icons.Default.CheckCircle
        "HARAM" -> Icons.Default.Cancel
        else -> Icons.Default.Warning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ingredients_analysis_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF051E13)),
        border = BorderStroke(1.dp, statusColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = result.status,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = result.status,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            }

            Text(
                text = result.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            if (result.flaggedIngredients.isNotEmpty()) {
                Text(
                    text = "Flagged Concerns:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3C63F)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    result.flaggedIngredients.forEach { ingredient ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF451A1A), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFFD32F2F), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Dangerous, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(12.dp))
                                Text(ingredient, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x22388E3C), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                    Text("No doubtful additives or forbidden animal-derived E-numbers found.", color = Color(0xFFA5D6A7), fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

@Composable
fun HistoryCard(
    item: ScannedProductEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (item.status.uppercase()) {
        "HALAL" -> Color(0xFF388E3C)
        "HARAM" -> Color(0xFFD32F2F)
        else -> Color(0xFFF57C00)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_card_${item.barcode}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
        border = BorderStroke(1.dp, Color(0xFF0E5E3A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Badge for status
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.status.uppercase()) {
                            "HALAL" -> Icons.Default.Check
                            "HARAM" -> Icons.Default.Close
                            else -> Icons.Default.QuestionMark
                        },
                        contentDescription = item.status,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = "${item.brand} • ${item.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Scan Record",
                    tint = Color.Gray.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ProductDetailDialog(
    product: ScannedProductEntity,
    onDismiss: () -> Unit
) {
    val statusColor = when (product.status.uppercase()) {
        "HALAL" -> Color(0xFF388E3C)
        "HARAM" -> Color(0xFFD32F2F)
        else -> Color(0xFFF57C00)
    }

    val statusIcon = when (product.status.uppercase()) {
        "HALAL" -> Icons.Default.CheckCircle
        "HARAM" -> Icons.Default.Cancel
        else -> Icons.Default.Warning
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(2.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = product.status,
                        tint = statusColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${product.brand} • Barcode: ${product.barcode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(color = Color(0xFF0E5E3A))
                
                Column {
                    Text("HALAL STATUS DECISION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF3C63F))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.status.uppercase(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }

                Column {
                    Text("DIETARY ANALYSIS REASONING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF3C63F))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }

                Column {
                    Text("INGREDIENTS DETECTED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF3C63F))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.ingredients,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                if (product.flaggedIngredients.isNotBlank()) {
                    Column {
                        Text("FLAGGED INGREDIENT CONCERNS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.flaggedIngredients,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF5350)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3C63F), contentColor = Color(0xFF051E13)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Dismiss", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF0A2218),
        shape = RoundedCornerShape(16.dp)
    )
}
