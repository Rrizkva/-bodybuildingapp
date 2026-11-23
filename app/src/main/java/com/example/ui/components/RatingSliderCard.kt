package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.JalaliDateUtil
import kotlin.math.roundToInt

/**
 * Shared 1-to-10 Material 3 diamond Slider card used in:
 * - SessionReviewScreen (interactive, onRatingSelected != null)
 * - SessionSummaryScreen (read-only, onRatingSelected == null)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingSliderCard(
    title: String,
    subtitle: String,
    selectedRating: Int,
    testTagPrefix: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onRatingSelected: ((Int) -> Unit)? = null
) {
    val clampedSelected = selectedRating.coerceIn(1, 10)
    val isInteractive = onRatingSelected != null
    val fraction = ((clampedSelected - 1) / 9f).coerceIn(0f, 1f)
    // Alpha scales from 0.1 (almost invisible at value 1) to 1.0 (fully saturated at value 10)
    val diamondAlpha = 0.10f + fraction * 0.90f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Reserve vertical space for the plain Persian value label floating above the diamond thumb
            Spacer(modifier = Modifier.height(28.dp))

            Slider(
                value = clampedSelected.toFloat(),
                onValueChange = { raw ->
                    if (isInteractive) {
                        onRatingSelected?.invoke(raw.roundToInt().coerceIn(1, 10))
                    }
                },
                valueRange = 1f..10f,
                steps = 8, // 8 intermediate ticks between 1 and 10 -> snaps strictly to whole numbers 1..10
                enabled = isInteractive,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                    disabledThumbColor = accentColor,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent,
                    disabledActiveTickColor = Color.Transparent,
                    disabledInactiveTickColor = Color.Transparent
                ),
                thumb = {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Diamond (rhombus) thumb: rotated square filled with primary/accent color scaled by intensity (0.1..1.0),
                        // with no outline and no color extending outside the diamond itself.
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(45f)
                                .background(accentColor.copy(alpha = diamondAlpha), RoundedCornerShape(3.dp))
                                .testTag("${testTagPrefix}_thumb")
                        )

                        // Plain Persian digit centered horizontally above the diamond, moving with it
                        Text(
                            text = JalaliDateUtil.toPersianDigits(clampedSelected),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .wrapContentSize(unbounded = true)
                                .offset(y = (-26).dp)
                                .testTag("${testTagPrefix}_value_text")
                        )
                    }
                },
                track = {
                    // Empty transparent track: no line, no extra colored area, no outline, and no tick marks
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // End labels: in RTL, Start (first child) aligns with value 1 end, End (second child) aligns with value 10 end
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سبک",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (clampedSelected == 1) FontWeight.Bold else FontWeight.Medium,
                    color = if (clampedSelected == 1) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("${testTagPrefix}_min_label")
                )

                Text(
                    text = "سنگین",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (clampedSelected == 10) FontWeight.Bold else FontWeight.Medium,
                    color = if (clampedSelected == 10) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("${testTagPrefix}_max_label")
                )
            }
        }
    }
}
