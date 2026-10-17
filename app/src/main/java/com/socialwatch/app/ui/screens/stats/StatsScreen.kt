package com.socialwatch.app.ui.screens.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.socialwatch.app.R
import com.socialwatch.app.ui.components.SWBarChart
import com.socialwatch.app.ui.components.SWCard
import com.socialwatch.app.ui.components.SWSegmentedControl
import com.socialwatch.app.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Pager for swipe between Day / Week / Month
    val pagerState = rememberPagerState(
        initialPage = when (state.selectedRange) {
            TimeRange.DAY -> 0
            TimeRange.WEEK -> 1
            TimeRange.MONTH -> 2
        },
        pageCount = { 3 }
    )

    // Sync pager swipe → ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val range = when (page) {
                0 -> TimeRange.DAY
                1 -> TimeRange.WEEK
                else -> TimeRange.MONTH
            }
            if (range != state.selectedRange) {
                viewModel.selectRange(range)
            }
        }
    }

    // Sync ViewModel selection → pager (when user taps segmented control)
    LaunchedEffect(state.selectedRange) {
        val targetPage = when (state.selectedRange) {
            TimeRange.DAY -> 0
            TimeRange.WEEK -> 1
            TimeRange.MONTH -> 2
        }
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.stats_title),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Segmented control: Day / Week / Month
            SWSegmentedControl(
                items = listOf(
                    stringResource(R.string.day),
                    stringResource(R.string.week),
                    stringResource(R.string.month)
                ),
                selectedIndex = when (state.selectedRange) {
                    TimeRange.DAY -> 0
                    TimeRange.WEEK -> 1
                    TimeRange.MONTH -> 2
                },
                onSelectionChange = { index ->
                    viewModel.selectRange(
                        when (index) {
                            0 -> TimeRange.DAY
                            1 -> TimeRange.WEEK
                            else -> TimeRange.MONTH
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Swipeable chart area
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                // Each page shows the same chart card, data changes via ViewModel
                StatsPageContent(
                    state = state,
                    page = page
                )
            }
        }
    }
}

@Composable
private fun StatsPageContent(
    state: StatsState,
    page: Int
) {
    val rangeForPage = when (page) {
        0 -> TimeRange.DAY
        1 -> TimeRange.WEEK
        else -> TimeRange.MONTH
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary card
        item {
            SWCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = when (rangeForPage) {
                            TimeRange.DAY -> "Today"
                            TimeRange.WEEK -> "This Week"
                            TimeRange.MONTH -> "This Month"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (state.selectedRange == rangeForPage) {
                            TimeFormatter.formatDuration(state.totalMs)
                        } else {
                            "—"
                        },
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Chart card
        item {
            SWCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (state.selectedRange == rangeForPage && state.chartEntries.isNotEmpty()) {
                        SWBarChart(
                            entries = state.chartEntries,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )
                    } else if (state.selectedRange == rangeForPage && !state.isLoading) {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No data for this period",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Use your tracked apps and check back",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Loading placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading…",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Legend
        if (state.selectedRange == rangeForPage && state.chartEntries.isNotEmpty()) {
            item {
                SWCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "APPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Collect unique app segments with their colors
                        val appSegments = state.chartEntries
                            .flatMap { it.segments }
                            .groupBy { it.label }
                            .mapValues { (_, segments) -> segments.first().color to segments.sumOf { it.durationMs } }

                        appSegments.forEach { (appName, colorAndDuration) ->
                            if (appName.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(colorAndDuration.first)
                                        )
                                        Text(
                                            text = appName,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(start = 10.dp)
                                        )
                                    }
                                    Text(
                                        text = TimeFormatter.formatDuration(colorAndDuration.second),
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}
