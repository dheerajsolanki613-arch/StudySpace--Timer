package com.studyspace.timer.screens.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.timer.CountdownTimerViewModel
import com.studyspace.timer.timer.StopwatchTimerViewModel
import com.studyspace.timer.timer.TimerRunState
import com.studyspace.timer.timer.formatTimerDuration
import com.studyspace.timer.ui.components.PrimaryButton
import com.studyspace.timer.ui.components.SecondaryButton
import com.studyspace.timer.ui.components.SessionAttributionPicker
import com.studyspace.timer.ui.components.TimerCard
import com.studyspace.timer.ui.components.filteredForSubject
import com.studyspace.timer.ui.util.centeredContentWidth

private val timerModes = listOf("Self-Study", "Online Study", "Normal")

/**
 * Timer screen: Self-Study and Online Study are open-ended stopwatches;
 * Normal is a countdown from a user-selected preset. Each tab keeps its own
 * ViewModel instance (via distinct `viewModel(key = ...)` calls), obtained
 * unconditionally every recomposition regardless of [selectedTab], so a
 * timer keeps counting even while a different tab is showing, switching
 * tabs never resets or shares another mode's progress, and a timer that was
 * running keeps running straight through a rotation (the ViewModel isn't
 * scoped to this composable's on-screen position, so relaying out for
 * landscape doesn't touch it).
 *
 * Responsive layout: the whole screen is wrapped in [BoxWithConstraints] so
 * the layout can react to its own measured size rather than guessing from
 * device orientation alone. Two arrangements:
 *  - **Portrait / narrow** (`maxWidth <= maxHeight`): the original single
 *    centered [Column] — title, tab row, then the selected mode's card and
 *    controls stacked, capped to `centeredContentWidth` and scrollable so
 *    nothing clips on a small screen.
 *  - **Landscape / wide** (`maxWidth > maxHeight`): a [Row] instead, so the
 *    title + mode tabs sit in a left pane and the timer card + controls sit
 *    in a right pane side by side, both vertically centered. This is what
 *    actually fixes cramped/scrolly landscape phones — stacking everything
 *    in one column on a short-but-wide screen was what forced the excessive
 *    scrolling; splitting the two concerns into a row lets each pane use
 *    the screen's spare *width* instead of fighting over its scarce
 *    *height*. The right pane still scrolls internally as a safety net for
 *    unusually short landscape heights (e.g. a phone with the keyboard up),
 *    so content reflows rather than clipping.
 */
@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    initialSubjectId: Long? = null,
    initialTaskId: Long? = null,
    initialTab: Int? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val selfStudyViewModel: StopwatchTimerViewModel = viewModel(key = "self_study_timer")
    val onlineStudyViewModel: StopwatchTimerViewModel = viewModel(key = "online_study_timer")
    val normalViewModel: CountdownTimerViewModel = viewModel(key = "normal_timer")

    // Planner Phase 6: arriving here via Screen.TimerFromPlan pre-selects
    // the plan's subject/task on the Self-Study tab, reusing Phase 5's
    // existing idle-only selectSubject/selectTask (a no-op if either is
    // null, e.g. a plan with no subject/task attached). Keyed on the two
    // ids so this only re-runs if a *different* plan's TimerScreen instance
    // is composed, not on every recomposition.
    LaunchedEffect(initialSubjectId, initialTaskId) {
        if (initialSubjectId != null || initialTaskId != null) {
            selectedTab = 0
            selfStudyViewModel.selectSubject(initialSubjectId)
            selfStudyViewModel.selectTask(initialTaskId)
        }
    }

    // Phase 10 (Daily Dashboard): arriving here via Screen.QuickStartTimer
    // ("Start Custom Timer" jumps straight to the Normal/countdown tab,
    // index 2) lands on a specific tab without touching subject/task
    // selection — a separate effect from the one above since the two
    // triggers are independent (a quick-start tap never carries a
    // subject/task, a planner start never carries an explicit tab).
    LaunchedEffect(initialTab) {
        initialTab?.let { selectedTab = it }
    }

    val timerContent: @Composable () -> Unit = {
        when (selectedTab) {
            0 -> StopwatchTimerContent(
                label = "Self-Study Timer",
                type = SessionType.SELF_STUDY,
                viewModel = selfStudyViewModel
            )
            1 -> StopwatchTimerContent(
                label = "Online Study Timer",
                type = SessionType.ONLINE_STUDY,
                viewModel = onlineStudyViewModel
            )
            else -> CountdownTimerContent(viewModel = normalViewModel)
        }
    }

    val modeTabs: @Composable () -> Unit = {
        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
            timerModes.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label, maxLines = 1) }
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .widthIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Timer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    modeTabs()
                }

                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    timerContent()
                }
            }
        } else {
            val contentWidth: Dp = centeredContentWidth(maxWidth)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .width(contentWidth)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Timer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    modeTabs()
                    timerContent()
                }
            }
        }
    }
}

@Composable
private fun StopwatchTimerContent(
    label: String,
    type: SessionType,
    viewModel: StopwatchTimerViewModel
) {
    val state by viewModel.state.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()
    val customLabel by viewModel.customLabel.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.isIdle) {
            SessionAttributionPicker(
                subjects = subjects,
                tasks = tasks.filteredForSubject(selectedSubjectId),
                selectedSubjectId = selectedSubjectId,
                selectedTaskId = selectedTaskId,
                customLabel = customLabel,
                onSubjectSelected = viewModel::selectSubject,
                onTaskSelected = viewModel::selectTask,
                onLabelChange = viewModel::setCustomLabel
            )
        }

        TimerCard(
            label = label,
            timeText = formatTimerDuration(state.elapsedMillis),
            statusText = when (state.runState) {
                TimerRunState.IDLE -> "Ready to start"
                TimerRunState.RUNNING -> "Studying…"
                TimerRunState.PAUSED -> "Paused"
                TimerRunState.COMPLETED -> "Completed"
            },
            actions = {
                when (state.runState) {
                    TimerRunState.IDLE, TimerRunState.COMPLETED -> PrimaryButton(
                        text = "Start",
                        onClick = { viewModel.start(label, type) },
                        icon = Icons.Filled.PlayArrow
                    )
                    TimerRunState.RUNNING -> PrimaryButton(
                        text = "Pause",
                        onClick = { viewModel.pause() },
                        icon = Icons.Filled.Pause
                    )
                    TimerRunState.PAUSED -> PrimaryButton(
                        text = "Resume",
                        onClick = { viewModel.resume() },
                        icon = Icons.Filled.PlayArrow
                    )
                }
            }
        )

        SecondaryButton(
            text = "Stop & Reset",
            onClick = { viewModel.reset() },
            icon = Icons.Filled.Refresh,
            enabled = state.runState != TimerRunState.IDLE
        )
    }
}

@Composable
private fun CountdownTimerContent(viewModel: CountdownTimerViewModel) {
    val state by viewModel.state.collectAsState()
    val selectedDuration by viewModel.selectedDurationMillis.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()
    val customLabel by viewModel.customLabel.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.isIdle) {
            Text(
                text = "Duration",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CountdownTimerViewModel.PRESET_MINUTES) { minutes ->
                    val millis = minutes * 60_000L
                    FilterChip(
                        selected = selectedDuration == millis,
                        onClick = { viewModel.selectDuration(millis) },
                        label = { Text("${minutes}m") }
                    )
                }
            }

            SessionAttributionPicker(
                subjects = subjects,
                tasks = tasks.filteredForSubject(selectedSubjectId),
                selectedSubjectId = selectedSubjectId,
                selectedTaskId = selectedTaskId,
                customLabel = customLabel,
                onSubjectSelected = viewModel::selectSubject,
                onTaskSelected = viewModel::selectTask,
                onLabelChange = viewModel::setCustomLabel
            )
        }

        TimerCard(
            label = "Normal Timer",
            timeText = formatTimerDuration(
                if (state.isIdle) selectedDuration else state.remainingMillis
            ),
            statusText = when (state.runState) {
                TimerRunState.IDLE -> "Ready to start"
                TimerRunState.RUNNING -> "Counting down…"
                TimerRunState.PAUSED -> "Paused"
                TimerRunState.COMPLETED -> "Time's up!"
            },
            actions = {
                when (state.runState) {
                    TimerRunState.IDLE -> PrimaryButton(
                        text = "Start",
                        onClick = { viewModel.start() },
                        icon = Icons.Filled.PlayArrow
                    )
                    TimerRunState.RUNNING -> PrimaryButton(
                        text = "Pause",
                        onClick = { viewModel.pause() },
                        icon = Icons.Filled.Pause
                    )
                    TimerRunState.PAUSED -> PrimaryButton(
                        text = "Resume",
                        onClick = { viewModel.resume() },
                        icon = Icons.Filled.PlayArrow
                    )
                    TimerRunState.COMPLETED -> PrimaryButton(
                        text = "Reset",
                        onClick = { viewModel.reset() },
                        icon = Icons.Filled.Refresh
                    )
                }
            }
        )

        SecondaryButton(
            text = "Reset",
            onClick = { viewModel.reset() },
            icon = Icons.Filled.Refresh,
            enabled = state.runState != TimerRunState.IDLE
        )
    }
}
