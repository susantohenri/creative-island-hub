package com.henrisusanto.creativeislandhub.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.henrisusanto.creativeislandhub.R
import com.henrisusanto.creativeislandhub.analytics.AnalyticsLogger
import com.henrisusanto.creativeislandhub.ui.components.BannerAdView
import com.henrisusanto.creativeislandhub.ui.components.IslandCard
import com.henrisusanto.creativeislandhub.ui.components.NativeAdCard
import com.henrisusanto.creativeislandhub.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val islands by viewModel.filteredIslands.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val unlockedIslands by viewModel.unlockedIslands.collectAsState()
    val likedIslands by viewModel.likedIslands.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val adsConfig by viewModel.adsConfig.collectAsState()
    
    val context = LocalContext.current
    var showUnlockDialog by remember { mutableStateOf<String?>(null) }

    val quickFilterKeywords = remember {
        listOf("UEFN", "PVP", "Boxfight", "Zero Build", "Battle Royale", "Parkour", "Adventure")
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true
        )

        // Quick Category Chips for high-intent keywords
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = {
                        viewModel.selectCategory(null)
                        AnalyticsLogger.logSelectCategory("all")
                    },
                    label = { Text(stringResource(R.string.category_all)) }
                )
            }
            items(quickFilterKeywords) { category ->
                val isSelected = selectedCategory?.equals(category, ignoreCase = true) == true
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            viewModel.selectCategory(null)
                            AnalyticsLogger.logSelectCategory("all")
                        } else {
                            viewModel.selectCategory(category)
                            AnalyticsLogger.logSelectCategory(category)
                        }
                    },
                    label = { Text(category) }
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error ?: stringResource(R.string.msg_error_loading))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.fetchData() }) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        } else if (islands.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.msg_empty_search),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            if (adsConfig.isAdsEnabled && adsConfig.bannerAdUnitId != null) {
                BannerAdView(adUnitId = adsConfig.bannerAdUnitId!!)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(islands, key = { _, it -> it.code }) { index, island ->
                    IslandCard(
                        island = island,
                        isUnlocked = unlockedIslands.contains(island.code),
                        isLiked = likedIslands.contains(island.code),
                        onUnlockClick = { showUnlockDialog = island.code },
                        onLikeClick = {
                            val isLikedNow = !likedIslands.contains(island.code)
                            viewModel.toggleLike(island.code)
                            AnalyticsLogger.logToggleLike(island.code, isLikedNow)
                        },
                        onTagClick = { tag -> viewModel.updateSearchQuery(tag) },
                        onCopyClick = { (context as? Activity)?.let { viewModel.onCodeCopied(it) } }
                    )

                    // Insert Native Ad every 6 items
                    if (adsConfig.isAdsEnabled && adsConfig.nativeAdUnitId != null && (index + 1) % 6 == 0) {
                        NativeAdCard(
                            adUnitId = adsConfig.nativeAdUnitId!!,
                            adManager = viewModel.adManager
                        )
                    }
                }
            }
            
            if (adsConfig.isAdsEnabled && adsConfig.bannerAdUnitId != null) {
                BannerAdView(adUnitId = adsConfig.bannerAdUnitId!!)
            }
        }
    }

    if (showUnlockDialog != null) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = null },
            title = { Text(stringResource(R.string.dialog_unlock_title)) },
            text = { Text(stringResource(R.string.msg_unlock_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val codeToUnlock = showUnlockDialog
                        showUnlockDialog = null
                        if (codeToUnlock != null) {
                            if (adsConfig.isAdsEnabled && adsConfig.rewardedAdUnitId != null) {
                                val activity = context as? Activity
                                if (activity != null) {
                                    viewModel.adManager.loadAndShowRewardedAd(
                                        activity = activity,
                                        adUnitId = adsConfig.rewardedAdUnitId!!,
                                        onRewardEarned = {
                                            viewModel.unlockIsland(codeToUnlock)
                                            val targetIsland = islands.find { it.code == codeToUnlock }
                                            AnalyticsLogger.logUnlockCode(
                                                islandCode = codeToUnlock,
                                                islandTitle = targetIsland?.title,
                                                category = targetIsland?.category
                                            )
                                        },
                                        onAdClosed = {},
                                        onFailed = {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.msg_ad_not_ready),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            } else {
                                viewModel.unlockIsland(codeToUnlock)
                                val targetIsland = islands.find { it.code == codeToUnlock }
                                AnalyticsLogger.logUnlockCode(
                                    islandCode = codeToUnlock,
                                    islandTitle = targetIsland?.title,
                                    category = targetIsland?.category
                                )
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_watch_ad))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
