package com.example.re_pro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CategoryGrid(
    onCategoryClick: (String) -> Unit = {}
) {
    val categories = listOf(
        "Calma", "Energía",
        "Concentración", "Fiesta",
        "Dormir", "Gaming",
        "Ejercicio", "Romance"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in categories.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryCard(
                    text = categories[i],
                    modifier = Modifier.weight(1f),
                    onClick = { onCategoryClick(categories[i]) }
                )
                if (i + 1 < categories.size) {
                    CategoryCard(
                        text = categories[i + 1],
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(categories[i + 1]) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
