package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NewsEntity
import com.example.viewmodel.DeenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsTab(
    viewModel: DeenViewModel,
    modifier: Modifier = Modifier
) {
    val newsList by viewModel.newsFeed.collectAsState()
    val searchQuery by viewModel.newsSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedNewsCategory.collectAsState()

    var selectedArticle by remember { mutableStateOf<NewsEntity?>(null) }

    val categories = listOf("All", "Spiritual Guidance", "History & Science", "Community Updates", "Charity & Welfare")

    Box(modifier = modifier.fillMaxSize()) {
        if (selectedArticle != null) {
            // Full Article Detail Reader View
            ArticleDetailView(
                article = selectedArticle!!,
                onClose = { selectedArticle = null }
            )
        } else {
            // Main News Feed View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Title
                Text(
                    text = "Global Islamic News Feed",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3C63F)
                    )
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setNewsSearchQuery(it) },
                    placeholder = { Text("Search articles, updates...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setNewsSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("news_search_bar"),
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

                // Horizontal Category filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectNewsCategory(category) },
                            label = { Text(category) },
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
                            modifier = Modifier.testTag("news_category_chip_${category.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                // Filter the news list locally in UI based on VM search and category
                val filteredNews = newsList.filter {
                    val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) ||
                            it.summary.contains(searchQuery, ignoreCase = true) ||
                            it.content.contains(searchQuery, ignoreCase = true)
                    
                    val matchesCategory = selectedCategory == "All" || it.category == selectedCategory
                    
                    matchesSearch && matchesCategory
                }

                if (filteredNews.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Newspaper,
                        message = "No articles found matching your criteria. Check back later!"
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("news_list")
                    ) {
                        items(filteredNews) { article ->
                            NewsCardItem(
                                article = article,
                                onClick = { selectedArticle = article }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCardItem(
    article: NewsEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("news_card_${article.id}"),
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
                Surface(
                    color = Color(0x330E5E3A),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = article.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFF3C63F),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Text(
                    text = article.publishDate,
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.LightGray,
                    lineHeight = 20.sp
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Source: ${article.source}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )

                Text(
                    text = "Read Article →",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFFF3C63F),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun ArticleDetailView(
    article: NewsEntity,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF051E13))
            .testTag("article_detail_view")
    ) {
        // Top Toolbar
        Surface(
            color = Color(0xFF0A2218),
            border = BorderStroke(1.dp, Color(0xFF0E5E3A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("close_article_btn")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Article Reader",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        // Article Body Scroll content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .background(Color(0xFF0A2218), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF0E5E3A), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0x330E5E3A),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = article.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFF3C63F),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Published ${article.publishDate}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 32.sp
                )
            )

            HorizontalDivider(color = Color(0x330E5E3A))

            // Scrollable Content
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = article.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.LightGray,
                            lineHeight = 28.sp
                        )
                    )
                }
            }

            HorizontalDivider(color = Color(0x330E5E3A))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Authorized Source: ${article.source}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5E3A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close Reader", color = Color(0xFFF3C63F))
                }
            }
        }
    }
}
