package com.studyspace.timer.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the user has turned on "Reduce motion" in Settings (Stage 8).
 * Provided once, app-wide, in [com.studyspace.timer.MainActivity] from
 * [com.studyspace.timer.screens.settings.SettingsViewModel] — components
 * that animate (currently just [com.studyspace.timer.ui.components.ProgressRing])
 * read it via `LocalReduceMotion.current` instead of taking it as a
 * parameter, so adding a new animated component later doesn't require
 * threading a boolean through every call site.
 *
 * Defaults to `false` (animations on) so a composable previewed or tested
 * without the provider still behaves like the shipped default.
 */
val LocalReduceMotion = compositionLocalOf { false }
