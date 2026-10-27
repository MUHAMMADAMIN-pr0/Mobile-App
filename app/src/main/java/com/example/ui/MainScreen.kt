package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.DeenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf(
        NavigationItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavigationItem("Quran", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
        NavigationItem("Mosques", Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
        NavigationItem("News", Icons.Filled.Newspaper, Icons.Outlined.Newspaper),
        NavigationItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "DeenPluse",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif,
                            color = Color(0xFFF3C63F)
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0A2218),
                    titleContentColor = Color(0xFFF3C63F)
                ),
                modifier = Modifier.testTag("main_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0A2218),
                contentColor = Color(0xFFF3C63F),
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color(0xFFF3C63F) else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color(0xFFF3C63F) else Color.Gray,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF0E5E3A)
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.title.lowercase()}")
                    )
                }
            }
        },
        containerColor = Color(0xFF051E13),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(viewModel = viewModel)
                1 -> QuranTab(viewModel = viewModel)
                2 -> MosqueLocatorTab(viewModel = viewModel)
                3 -> NewsTab(viewModel = viewModel)
                4 -> SettingsTab(viewModel = viewModel)
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
