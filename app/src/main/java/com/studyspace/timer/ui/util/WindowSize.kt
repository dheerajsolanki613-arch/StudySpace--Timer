package com.studyspace.timer.ui.util

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The app's current [WindowSizeClass], provided once in
 * [com.studyspace.timer.MainActivity] via `calculateWindowSizeClass(this)`.
 * `staticCompositionLocalOf` because there's no sensible default to fall
 * back to silently — reading this without the provider being set up (i.e.
 * outside the app's own composition root) is a programming error we want a
 * loud failure for, not a quietly-wrong layout.
 */
val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("LocalWindowSizeClass not provided — read it from inside StudySpaceTimerApp()")
}

/** True for a phone in landscape or a small/medium tablet — i.e. "give content more horizontal room." */
fun WindowSizeClass.hasExpandedOrMediumWidth(): Boolean =
    widthSizeClass != WindowWidthSizeClass.Compact
