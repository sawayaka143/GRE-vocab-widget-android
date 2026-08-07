package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.MAX_REFRESH_INTERVAL_MINUTES
import com.example.myapplication.data.MIN_BACKGROUND_REFRESH_INTERVAL_MINUTES
import com.example.myapplication.data.MIN_REFRESH_INTERVAL_MINUTES
import com.example.myapplication.data.ThemeMode
import com.example.myapplication.data.WidgetRefreshSettingsStore

@Composable
fun SettingsScreen(
    settingsStore: WidgetRefreshSettingsStore,
    onBack: () -> Unit
) {
    settingsStore.revision.intValue
    var intervalText by remember {
        mutableStateOf(settingsStore.refreshIntervalMinutes().toString())
    }
    val parsedInterval = intervalText.toIntOrNull()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "< Back",
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .clickable(onClick = onBack)
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "Settings",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp
                )
            }

            Text(
                text = "Widget refresh",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 24.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Refresh widgets while away",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Refresh approximately every 30 minutes while the phone is in use.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(
                    checked = settingsStore.refreshWhileAway(),
                    onCheckedChange = settingsStore::setRefreshWhileAway
                )
            }

            OutlinedTextField(
                value = intervalText,
                onValueChange = { value ->
                    if (value.length <= 4 && value.all(Char::isDigit)) intervalText = value
                },
                label = { Text("Refresh interval (minutes)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        parsedInterval?.let {
                            settingsStore.setRefreshIntervalMinutes(it)
                            intervalText = settingsStore.refreshIntervalMinutes().toString()
                        }
                    },
                    enabled = parsedInterval != null &&
                        parsedInterval in MIN_REFRESH_INTERVAL_MINUTES..MAX_REFRESH_INTERVAL_MINUTES &&
                        parsedInterval != settingsStore.refreshIntervalMinutes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Apply interval")
                }
            }

            if (parsedInterval != null &&
                parsedInterval !in MIN_REFRESH_INTERVAL_MINUTES..MAX_REFRESH_INTERVAL_MINUTES
            ) {
                Text(
                    text = "Enter a value from 1 to 1440 minutes.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (parsedInterval != null && parsedInterval < MIN_BACKGROUND_REFRESH_INTERVAL_MINUTES) {
                Text(
                    text = "Android may delay background refreshes below 15 minutes; the background schedule will use 15 minutes.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(top = 24.dp))

            Text(
                text = "Layout",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 20.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Checkboxes on the right side",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Show deck selection checkboxes on the right of the deck names instead of the left.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(
                    checked = settingsStore.checkboxesOnRight(),
                    onCheckedChange = settingsStore::setCheckboxesOnRight
                )
            }

            HorizontalDivider(modifier = Modifier.padding(top = 24.dp))

            Text(
                text = "Appearance",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 20.dp)
            )

            ThemeMode.values().forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsStore.setThemeMode(mode) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settingsStore.themeMode() == mode,
                        onClick = { settingsStore.setThemeMode(mode) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = themeLabel(mode),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Use system theme"
    ThemeMode.LIGHT -> "Light theme"
    ThemeMode.DARK -> "Dark theme"
    ThemeMode.MAGOOSH -> "Ugly Magoosh theme"
}
