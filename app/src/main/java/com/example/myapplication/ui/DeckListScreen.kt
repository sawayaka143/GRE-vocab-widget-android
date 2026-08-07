package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Deck
import com.example.myapplication.data.DeckSelectionStore
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.ui.theme.AppThemeMode
import com.example.myapplication.ui.theme.OledBgPrimary
import com.example.myapplication.ui.theme.OledTextPrimary
import com.example.myapplication.ui.theme.OledTextSecondary
import com.example.myapplication.ui.theme.currentThemeMode
import com.example.myapplication.ui.theme.MagooshGreen

@Composable
fun DeckListScreen(
    decks: List<Deck>,
    progressStore: ProgressStore,
    selectionStore: DeckSelectionStore,
    checkboxesOnRight: Boolean,
    onPracticeDeck: (Deck) -> Unit,
    onOpenSettings: () -> Unit
) {
    // Observe revisions so the list recomposes when progress changes (e.g. from the widget).
    progressStore.revision.intValue
    selectionStore.revision.intValue
    val selectedCount = selectionStore.selectedDeckNames().size
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GRE Vocabulary Flashcards",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp)
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Text(
                text = "Check the decks that feed the home screen widget",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            decks.forEach { deck ->
                DeckCard(
                    deck = deck,
                    progressStore = progressStore,
                    checked = selectionStore.isSelected(deck.name),
                    checkboxesOnRight = checkboxesOnRight,
                    onCheckedChange = { checked -> selectionStore.setSelected(deck.name, checked) },
                    onPractice = { onPracticeDeck(deck) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (selectedCount == 0) "No decks selected — widget will use all decks"
                else "Selected: $selectedCount of ${decks.size} decks feed the widget",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun DeckCard(
    deck: Deck,
    progressStore: ProgressStore,
    checked: Boolean,
    checkboxesOnRight: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onPractice: () -> Unit
) {
    val total = deck.words.size
    val (mastered, _, _) = progressStore.countsFor(deck)
    val fraction = if (total == 0) 0f else mastered.toFloat() / total
    val label = "$mastered of $total words mastered"

    // Inverted OLED checkbox: black body with off-white check; gray outline when unchecked.
    val checkboxColors = if (currentThemeMode() == AppThemeMode.OLED) {
        CheckboxDefaults.colors(
            checkedColor = OledBgPrimary,
            checkmarkColor = OledTextPrimary,
            uncheckedColor = OledTextSecondary
        )
    } else {
        CheckboxDefaults.colors()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (checkboxesOnRight) {
                    Text(
                        text = deck.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    )
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        colors = checkboxColors,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                } else {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        colors = checkboxColors,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        text = deck.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Progress bar: same horizontal padding as the text (16dp), 2.3x taller,
            // square ends (StrokeCap.Butt) so it isn't rounded like a pill.
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(18.4.dp),
                color = MagooshGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Butt
            )

            // White gap between the progress bar and the gray Practice band.
            Spacer(modifier = Modifier.height(8.dp))

            // Gray band with a faint divider, holding the full-width Practice strip.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                Text(
                    text = "Practice this deck →",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onPractice)
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}
