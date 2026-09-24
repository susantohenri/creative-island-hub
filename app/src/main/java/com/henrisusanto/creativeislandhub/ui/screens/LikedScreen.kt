package com.henrisusanto.creativeislandhub.ui.screens

import android.app.Activity
import android.widget.Toast
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
import com.henrisusanto.creativeislandhub.ui.components.BannerAdView
import com.henrisusanto.creativeislandhub.ui.components.IslandCard
import com.henrisusanto.creativeislandhub.ui.viewmodel.MainViewModel

@Composable
fun LikedScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val islands by viewModel.filteredIslands.collectAsState()
    val likedIslands by viewModel.likedIslands.collectAsState()
    val unlockedIslands by viewModel.unlockedIslands.collectAsState()
    val adsConfig by viewModel.adsConfig.collectAsState()
    
    val context = LocalContext.current
    var showUnlockDialog by remember { mutableStateOf<String?>(null) }
    val likedItems = islands.filter { likedIslands.contains(it.code) }

    Column(modifier = modifier.fillMaxSize()) {
        if (likedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.msg_empty_liked),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(likedItems, key = { it.code }) { island ->
                    IslandCard(
                        island = island,
                        isUnlocked = unlockedIslands.contains(island.code),
                        isLiked = true,
                        onUnlockClick = { showUnlockDialog = island.code },
                        onLikeClick = { viewModel.toggleLike(island.code) },
                        onTagClick = { tag -> viewModel.updateSearchQuery(tag) }
                    )
                }
            }
        }
        
        if (adsConfig.isAdsEnabled && adsConfig.bannerAdUnitId != null) {
            BannerAdView(adUnitId = adsConfig.bannerAdUnitId!!)
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
                                        onRewardEarned = { viewModel.unlockIsland(codeToUnlock) },
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
