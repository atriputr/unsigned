package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LanguagePickerOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    onLanguageSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalPalette.current

    GlassCard(
        modifier = Modifier.heightIn(max = 600.dp),
        onClose = onClose
    ) {
        Text(
            text = LocalStrings.current.language,
            color = palette.onSurface,
            fontSize = 22.sp,
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(Localization.languages.toList()) { (code, name) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.chipBg)
                        .border(1.dp, palette.fieldBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            Haptics.click(context)
                            onLanguageSelect(code)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        color = palette.onSurface,
                        fontSize = 14.sp,
                        fontFamily = contentFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = LocalStrings.current.back,
            color = palette.faint,
            fontSize = 14.sp,
            fontFamily = titleFont,
            modifier = Modifier.clickable { 
                Haptics.click(context)
                onClose() 
            }
        )
    }
}
