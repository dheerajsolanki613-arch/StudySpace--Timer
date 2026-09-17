package com.studyspace.timer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.studyspace.timer.ui.theme.GalaxyNeonCyan
import com.studyspace.timer.ui.theme.GalaxyNeonPink
import com.studyspace.timer.ui.theme.GalaxyStarWhite
import com.studyspace.timer.ui.theme.LocalReduceMotion

/**
 * Circular progress indicator used for "today's study time" / Pomodoro ring.
 * [progress] is 0f..1f. Purely presentational — the value comes from real
 * data starting in Stage 5 (Room) / Stage 3 (timer engine).
 *
 * Stage 8: when [animate] is true (the default), the sweep eases to a new
 * [progress] value with a short [tween] rather than snapping instantly, so
 * e.g. the Home dashboard ring doesn't visibly jump when a session
 * finishes. This is the one animation this project adds (Stage 2's "avoid
 * unnecessary animations that reduce performance" rule is otherwise still
 * followed — no screen-transition or decorative motion was added anywhere).
 * It respects [LocalReduceMotion]: when the user has that Settings toggle
 * on, the arc snaps to the new value immediately instead of easing,
 * exactly like the pre-Stage-8 behavior.
 *
 * Pass [animate] = false for a ring driven by an already-live, fast-ticking
 * value — the Pomodoro screen's countdown ring updates every ~200ms while
 * running (see [com.studyspace.timer.timer.TimerEngine]'s tick interval);
 * re-triggering a 450ms ease on every one of those updates would make the
 * displayed sweep visibly lag behind the real countdown instead of
 * smoothing anything. The Home dashboard ring (this component's other use)
 * updates far less often — once when its stats are (re)loaded — which is
 * exactly the kind of discrete jump easing helps with, so it keeps the
 * default.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Int = 120,
    strokeWidth: Int = 10,
    trackColor: Color = Color.White.copy(alpha = 0.12f),
    progressColor: Color = GalaxyNeonCyan,
    centerLabel: String? = null,
    centerSubLabel: String? = null,
    animate: Boolean = true
) {
    val reduceMotion = LocalReduceMotion.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion || !animate) snap() else tween(durationMillis = 450),
        label = "progressRingSweep"
    )
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val stroke = Stroke(width = strokeWidth.dp.toPx())
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = stroke
            )
        }
        if (centerLabel != null) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = centerLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = GalaxyStarWhite
                    )
                    if (centerSubLabel != null) {
                        Text(
                            text = centerSubLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GalaxyStarWhite.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/** Two-tone variant (used for Pomodoro work/break cycles later). */
val ProgressRingWorkColor = GalaxyNeonPink
