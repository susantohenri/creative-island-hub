package com.henrisusanto.creativeislandhub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    
    val likedItems = islands.filter { likedIslands.contains(it.code) }

    Column(modifier = modifier.fillMaxSize()) {
        if (likedItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.msg_empty_liked))
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
                        onUnlockClick = { /* Similar unlock logic if needed, or disable since it's liked */ },
                        onLikeClick = { viewModel.toggleLike(island.code) }
                    )
                }
            }
        }
        
        if (adsConfig.isAdsEnabled && adsConfig.bannerAdUnitId != null) {
            BannerAdView(adUnitId = adsConfig.bannerAdUnitId!!)
        }
    }
}
