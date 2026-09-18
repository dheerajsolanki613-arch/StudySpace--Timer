package com.studyspace.timer.ui.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * True when the device is currently in landscape orientation. Screens use
 * this to switch between a vertical (portrait) and a wider, side-by-side
 * (landscape) arrangement of the same content/state — never to duplicate a
 * screen or re-fetch data, just to reflow layout.
 *
 * Reads [LocalConfiguration] rather than `Activity.requestedOrientation` or
 * a sensor listener, so it's driven by the same configuration-change
 * mechanism the OS already recomposes the app on for rotation, and works
 * correctly in split-screen/foldable width changes too, not just a literal
 * phone rotation.
 */
@Composable
fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
