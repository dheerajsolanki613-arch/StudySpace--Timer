package com.studyspace.timer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * StudySpace Timer supports both a dark "galaxy" aesthetic and a light
 * "Kawaii Pastel" one — [paletteId] selects which [AppPalettes] entry
 * supplies the actual Material [androidx.compose.material3.ColorScheme],
 * selectable and persisted from the Themes screen. Every screen, card, and
 * button reads its colors from `MaterialTheme.colorScheme` (no more fixed
 * `Galaxy*` constants outside of [AppPalettes] itself), so swapping the
 * scheme here re-themes the whole app, backgrounds included.
 */
@Composable
fun StudySpaceTimerTheme(
    paletteId: String = AppPalettes.galaxy.id,
    content: @Composable () -> Unit
) {
    val palette = AppPalettes.byId(paletteId)
    MaterialTheme(
        colorScheme = palette.colorScheme,
        typography = StudySpaceTypography,
        content = content
    )
}
