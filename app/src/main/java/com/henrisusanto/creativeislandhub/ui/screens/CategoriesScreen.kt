package com.henrisusanto.creativeislandhub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.henrisusanto.creativeislandhub.R
import com.henrisusanto.creativeislandhub.ui.components.BannerAdView
import com.henrisusanto.creativeislandhub.ui.viewmodel.MainViewModel

@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onCategoryClick: (() -> Unit)? = null
) {
    val categories by viewModel.allCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val adsConfig by viewModel.adsConfig.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                CategoryItem(
                    title = stringResource(R.string.all_categories),
                    isSelected = selectedCategory == null,
                    onClick = {
                        viewModel.selectCategory(null)
                        onCategoryClick?.invoke()
                    }
                )
                HorizontalDivider()
            }
            items(categories) { category ->
                CategoryItem(
                    title = category,
                    isSelected = selectedCategory == category,
                    onClick = {
                        viewModel.selectCategory(category)
                        onCategoryClick?.invoke()
                    }
                )
                HorizontalDivider()
            }
        }
        
        if (adsConfig.isAdsEnabled && adsConfig.bannerAdUnitId != null) {
            BannerAdView(adUnitId = adsConfig.bannerAdUnitId!!)
        }
    }
}

@Composable
fun CategoryItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
