package com.henrisusanto.creativeislandhub.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.henrisusanto.creativeislandhub.R

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        
        Text(
            text = stringResource(R.string.title_settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Note: Full persistent language/theme toggle omitted for brevity, 
        // normally requires recreating activity or using specialized composable states.
        
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_privacy_policy)) },
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tokiocv.blogspot.com/2026/07/privacy-policy.html"))
                context.startActivity(intent)
            }
        )
        Divider()
        
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_about)) },
            supportingContent = { 
                Text("Version 1.0.0\n\n${stringResource(R.string.disclaimer_about)}") 
            }
        )
        Divider()
    }
}
