package com.studyspace.timer.ui.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** Below this measured width, a single-column screen just uses the full width — nothing to cap. */
private val WIDE_LAYOUT_THRESHOLD = 600.dp
private val MAX_CENTERED_CONTENT_WIDTH = 560.dp

/**
 * Given the actual measured width of a screen (from `BoxWithConstraints`'
 * `maxWidth`), returns how wide a single centered content column should be.
 *
 * Below [WIDE_LAYOUT_THRESHOLD] (effectively all portrait phones) this is
 * just `maxWidth` itself — full width, nothing to constrain. At or above it
 * (phone landscape, tablets, split-screen) it caps at 92% of the available
 * width or [MAX_CENTERED_CONTENT_WIDTH], whichever is smaller, so a timer
 * card/tab row/title don't stretch edge-to-edge and look cramped-but-wide
 * on a screen that's suddenly much wider than tall.
 *
 * Plain function, not a composable — `maxWidth` from `BoxWithConstraints`
 * is already a plain [Dp] value by the time it reaches call sites, so this
 * needs no composition context of its own.
 */
fun centeredContentWidth(measuredMaxWidth: Dp): Dp =
    if (measuredMaxWidth > WIDE_LAYOUT_THRESHOLD) {
        min(measuredMaxWidth.value * 0.92f, MAX_CENTERED_CONTENT_WIDTH.value).dp
    } else {
        measuredMaxWidth
    }
