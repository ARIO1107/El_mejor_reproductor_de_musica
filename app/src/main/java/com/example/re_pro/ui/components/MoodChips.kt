package com.example.re_pro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoodChips(
    selectedMood: String? = null,
    onMoodSelected: (String) -> Unit = {}
) {
    val moods = listOf("Dormir", "Relax", "Triste", "Amor", "Energía")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(moods) { mood ->
            val isSelected = mood == selectedMood
            FilterChip(
                selected = isSelected,
                onClick = { onMoodSelected(mood) },
                label = {
                    Text(
                        text = mood,
                        fontSize = 14.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
