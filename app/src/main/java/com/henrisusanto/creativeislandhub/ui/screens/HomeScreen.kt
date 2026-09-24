package com.henrisusanto.creativeislandhub.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.henrisusanto.creativeislandhub.R
import com.henrisusanto.creativeislandhub.ads.AdManager
import com.henrisusanto.creativeislandhub.data.model.AdsConfig
import com.henrisusanto.creativeislandhub.ui.components.BannerAdView
import com.henrisusanto.creativeislandhub.ui.components.IslandCard
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
    val adsConfig by viewModel.adsConfig.collectAsState()
    
    val context = LocalContext.current
    var showUnlockDialog by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true
        )

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
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(islands, key = { it.code }) { island ->
                    IslandCard(
                        island = island,
                        isUnlocked = unlockedIslands.contains(island.code),
                        isLiked = likedIslands.contains(island.code),
                        onUnlockClick = { showUnlockDialog = island.code },
                        onLikeClick = { viewModel.toggleLike(island.code) }
                    )
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
            title = { Text(stringResource(R.string.app_name)) },
            text = { Text(stringResource(R.string.msg_unlock_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val codeToUnlock = showUnlockDialog
                        showUnlockDialog = null
                        if (codeToUnlock != null && adsConfig.isAdsEnabled && adsConfig.rewardedAdUnitId != null) {
                            viewModel.adManager.loadRewardedAd(adsConfig.rewardedAdUnitId!!)
                            // For simplicity, wait a moment or show directly if preloaded. 
                            // Ideal UX is preloading.
                            viewModel.adManager.showRewardedAd(
                                activity = context as Activity,
                                onRewardEarned = { viewModel.unlockIsland(codeToUnlock) },
                                onAdClosed = {}
                            )
                        } else if (codeToUnlock != null) {
                            // Fallback unlock if ads disabled
                            viewModel.unlockIsland(codeToUnlock)
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
