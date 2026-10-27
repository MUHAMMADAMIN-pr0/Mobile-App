package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookmarkEntity
import com.example.data.QuranData
import com.example.data.Surah
import com.example.data.Verse
import com.example.viewmodel.DeenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranTab(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSurah by viewModel.selectedSurah.collectAsState()
    val searchQuery by viewModel.quranSearchQuery.collectAsState()
    val fontSize by viewModel.quranFontSize.collectAsState()
    val bookmarks by viewModel.bookmarkedVerses.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Surahs, 1 = Bookmarks

    Box(modifier = modifier.fillMaxSize()) {
        if (selectedSurah != null) {
            // Surah Reader View
            SurahReaderView(
                surah = selectedSurah!!,
                fontSize = fontSize,
                bookmarks = bookmarks,
                onClose = { viewModel.selectSurah(null) },
                onToggleBookmark = { verse -> viewModel.toggleBookmark(selectedSurah!!, verse) },
                onFontSizeChanged = { viewModel.setQuranFontSize(it) }
            )
        } else {
            // Main Library List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Title
                Text(
                    text = "Digital Quran Library",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3C63F)
                    )
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setQuranSearchQuery(it) },
                    placeholder = { Text("Search Surah name, number, or meaning...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuranSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_search_bar"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF3C63F),
                        unfocusedBorderColor = Color(0xFF0E5E3A),
                        focusedContainerColor = Color(0xFF0A2218),
                        unfocusedContainerColor = Color(0xFF0A2218),
                        focusedLabelColor = Color(0xFFF3C63F),
                        unfocusedLabelColor = Color.LightGray
                    )
                )

                // Tab Selector (All Surahs vs Bookmarks)
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color(0xFF0A2218),
                    contentColor = Color(0xFFF3C63F),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = Color(0xFFF3C63F)
                        )
                    }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("ALL SURAHS") },
                        modifier = Modifier.testTag("quran_all_surahs_tab")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("MY BOOKMARKS") },
                        modifier = Modifier.testTag("quran_bookmarks_tab")
                    )
                }

                // Conditional List Display
                if (activeTab == 0) {
                    // Filtered list of Surahs
                    val filteredSurahs = viewModel.surahs.filter {
                        it.englishName.contains(searchQuery, ignoreCase = true) ||
                                it.name.contains(searchQuery) ||
                                it.number.toString() == searchQuery ||
                                it.englishMeaning.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredSurahs.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.MenuBook,
                            message = "No Surahs found matching your search."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredSurahs) { surah ->
                                SurahListItem(
                                    surah = surah,
                                    onClick = { viewModel.selectSurah(surah) }
                                )
                            }
                        }
                    }
                } else {
                    // Bookmarks Tab
                    if (bookmarks.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.BookmarkBorder,
                            message = "Your bookmarks will appear here. Tap the bookmark icon next to any verse inside a Surah to save it."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bookmarks_list")
                        ) {
                            items(bookmarks) { bookmark ->
                                BookmarkListItem(
                                    bookmark = bookmark,
                                    onRemove = {
                                        // Retrieve the matching surah object and verse to toggle off
                                        val matchSurah = QuranData.surahs.firstOrNull { it.number == bookmark.surahNumber }
                                        val matchVerse = matchSurah?.verses?.firstOrNull { it.verseNumber == bookmark.verseNumber }
                                        if (matchSurah != null && matchVerse != null) {
                                            viewModel.toggleBookmark(matchSurah, matchVerse)
                                        }
                                    }
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
fun SurahListItem(
    surah: Surah,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("surah_item_${surah.number}"),
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
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Surah Number circle badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF0E5E3A), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = surah.number.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFF3C63F),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Column {
                    Text(
                        text = surah.englishName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = surah.englishMeaning,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color.Gray, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "${surah.verses.size} Verses",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = surah.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFFF3C63F),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = surah.type,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (surah.type == "Meccan") Color(0xFFD4AF37) else Color(0xFF00C853)
                    )
                )
            }
        }
    }
}

@Composable
fun BookmarkListItem(
    bookmark: BookmarkEntity,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
        border = BorderStroke(1.dp, Color(0xFF0E5E3A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${bookmark.surahName} [Verse ${bookmark.verseNumber}]",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3C63F)
                    )
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Remove Bookmark",
                        tint = Color(0xFFF3C63F)
                    )
                }
            }

            // Arabic text
            Text(
                text = bookmark.verseArabic,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    lineHeight = 32.sp,
                    color = Color.White,
                    textDirection = TextDirection.Rtl
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Translation text
            Text(
                text = bookmark.verseEnglish,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
            )
        }
    }
}

@Composable
fun SurahReaderView(
    surah: Surah,
    fontSize: Float,
    bookmarks: List<BookmarkEntity>,
    onClose: () -> Unit,
    onToggleBookmark: (Verse) -> Unit,
    onFontSizeChanged: (Float) -> Unit
) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF051E13))
            .testTag("surah_reader_view")
    ) {
        // Reader Header / Toolbar
        Surface(
            color = Color(0xFF0A2218),
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFF0E5E3A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("close_reader_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = surah.englishName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3C63F)
                            )
                        )
                        Text(
                            text = "${surah.englishMeaning} • ${surah.verses.size} Verses",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                        )
                    }

                    Box(modifier = Modifier.size(48.dp)) // spacer to balance close btn
                }

                // Font Size slider container for Accessibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.FormatSize, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Text Size: ${fontSize.toInt()}sp",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                    )
                    Slider(
                        value = fontSize,
                        onValueChange = onFontSizeChanged,
                        valueRange = 14f..36f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF3C63F),
                            activeTrackColor = Color(0xFF0E5E3A),
                            inactiveTrackColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Surah Verses Scrolling Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Beautiful Bismillah Calligraphy Display Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1A0E5E3A), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF0E5E3A), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color(0xFFF3C63F),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(surah.verses) { verse ->
                val isBookmarked = bookmarks.any { 
                    it.surahNumber == surah.number && it.verseNumber == verse.verseNumber 
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
                    border = BorderStroke(1.dp, Color(0x660E5E3A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Verse number badge and bookmark button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0x330E5E3A), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = verse.verseNumber.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFF3C63F),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            IconButton(
                                onClick = { onToggleBookmark(verse) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("bookmark_btn_${surah.number}_${verse.verseNumber}")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark verse",
                                    tint = if (isBookmarked) Color(0xFFF3C63F) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Arabic script verse
                        Text(
                            text = verse.arabic,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = fontSize.sp,
                                fontFamily = FontFamily.Serif,
                                lineHeight = (fontSize * 1.6f).sp,
                                color = Color.White,
                                textAlign = TextAlign.Right,
                                textDirection = TextDirection.Rtl
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // English translation verse
                        Text(
                            text = verse.translation,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (fontSize * 0.75f).coerceAtLeast(12f).sp,
                                color = Color.LightGray,
                                lineHeight = (fontSize * 1.1f).sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF0E5E3A),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        )
    }
}
