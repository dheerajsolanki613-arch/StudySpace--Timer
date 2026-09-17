package com.studyspace.timer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * StudySpace Timer is dark-first by design (galaxy aesthetic) — there is no
 * light-mode variant. What Stage 6 adds is [paletteId]: which of
 * [AppPalettes] supplies the actual Material [androidx.compose.material3.ColorScheme],
 * selectable and persisted from the Themes screen. Every screen, card, and
 * button already reads its colors from `MaterialTheme.colorScheme` (or the
 * `Galaxy*` constants that back the default palette), so swapping the scheme
 * here is enough to re-theme the whole app.
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
