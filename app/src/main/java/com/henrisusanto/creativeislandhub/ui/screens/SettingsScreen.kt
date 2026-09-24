package com.henrisusanto.creativeislandhub.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.henrisusanto.creativeislandhub.BuildConfig
import com.henrisusanto.creativeislandhub.R
import com.henrisusanto.creativeislandhub.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsState()
    val currentLanguage by viewModel.language.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.title_settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Section: Preferences
        Text(
            text = stringResource(R.string.settings_theme) + " & " + stringResource(R.string.settings_language),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Theme Item
        val themeSubtitle = when (currentTheme) {
            "LIGHT" -> stringResource(R.string.theme_light)
            "DARK" -> stringResource(R.string.theme_dark)
            else -> stringResource(R.string.theme_system)
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_theme)) },
            supportingContent = { Text(themeSubtitle) },
            modifier = Modifier.clickable { showThemeDialog = true }
        )
        HorizontalDivider()

        // Language Item
        val languageSubtitle = when (currentLanguage) {
            "EN" -> stringResource(R.string.language_en)
            "ID" -> stringResource(R.string.language_id)
            else -> stringResource(R.string.language_auto)
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_language)) },
            supportingContent = { Text(languageSubtitle) },
            modifier = Modifier.clickable { showLanguageDialog = true }
        )
        HorizontalDivider()

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Legal & Info
        Text(
            text = "Legal & Info",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Privacy Policy Item
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_privacy_policy)) },
            supportingContent = { Text("https://tokiocv.blogspot.com/2026/07/privacy-policy.html") },
            modifier = Modifier.clickable {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://tokiocv.blogspot.com/2026/07/privacy-policy.html")
                )
                context.startActivity(intent)
            }
        )
        HorizontalDivider()

        // About Item
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_about)) },
            supportingContent = {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.disclaimer_about),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
        HorizontalDivider()
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        val themeOptions = listOf(
            "SYSTEM" to stringResource(R.string.theme_system),
            "LIGHT" to stringResource(R.string.theme_light),
            "DARK" to stringResource(R.string.theme_dark)
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.dialog_select_theme)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    themeOptions.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = currentTheme == mode,
                                    onClick = {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == mode,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        val languageOptions = listOf(
            "AUTO" to stringResource(R.string.language_auto),
            "EN" to stringResource(R.string.language_en),
            "ID" to stringResource(R.string.language_id)
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.dialog_select_language)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    languageOptions.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = currentLanguage == code,
                                    onClick = {
                                        viewModel.setLanguage(code)
                                        showLanguageDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == code,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
