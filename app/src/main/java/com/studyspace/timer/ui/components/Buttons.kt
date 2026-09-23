package com.studyspace.timer.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.studyspace.timer.ui.theme.contrastSafeContentColor

/**
 * Primary filled action button (Start, Save, Confirm). Fills the available
 * width by default — matters most in landscape, where a wrap-content
 * button next to a lot of empty horizontal space reads as "cramped/
 * misplaced" rather than deliberate; pass `fillWidth = false` for the rare
 * case a compact inline button is actually wanted.
 *
 * Text/icon color is *computed* against the actual resolved container
 * color via [contrastSafeContentColor] rather than trusted blindly from
 * the palette's declared `onPrimaryContainer` — this is the direct fix for
 * low-contrast text ever landing on a bright accent button (e.g. dark
 * navy text that reads as illegible on a saturated neon pink), since it
 * can't silently drift out of sync the way a manually-paired color could.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    fillWidth: Boolean = true
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = contrastSafeContentColor(
        background = containerColor,
        preferred = MaterialTheme.colorScheme.onPrimaryContainer
    )
    Button(
        onClick = onClick,
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Secondary outlined action button (Pause, Cancel, secondary actions). Also fills width by default. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    fillWidth: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
