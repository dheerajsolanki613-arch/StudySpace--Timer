# PROJECT_STATE.md — StudySpace Timer

Single source of truth for where this project is. Update this file at the
end of every stage. If you're a fresh Claude session picking this up, read
this file fully before touching code.

## Status: PLAN PIVOT. The user introduced a new, much larger "Master Feature Expansion Prompt" (23 phases: goals, subjects, tasks/topics, planner, planned-vs-actual analytics, streaks/achievements, dashboard, export/import, backup/restore, accessibility, smart planning) and chose to **pause the original 10-stage plan** (Stages 9-10 never started) to begin this new plan instead. The original Stage 6-8 unverified-by-CI status below is unchanged and still applies — nothing in this pivot touched or re-verified those stages.

**New expansion plan status: Phases 13 (Data Export/Import), 14 (Backup & Restore) and 15 (Themes & Customization) written, unverified by CI/device** — see the new Session log entry and the "Expansion plan" checklist below. Phases 13/14 add **no migration**; Phase 15 adds **no migration but one new manifest permission** (`VIBRATE`, normal/install-time). File access throughout (13/14) uses the system file picker, no storage permission. One *test-only* Gradle dependency total so far (`org.json:json`, from Phase 13). Phases 11 (migration 5→6), 12 (alarms/receivers/permission) and now 13/14/15 are also still unverified — **five unverified phases are now stacked; the next CI run is the first real check of all of them.** Phase 14 also fixed a pre-existing bug in Phase 2's `StudyGoalDao`/`GoalRepository` (weekly goal edits were silently duplicating rows instead of replacing them) — see Phase 14's own entry for why. Phase 15 corrects a small factual error in Phase 14's own writeup (it said the "Personalize" custom-wallpaper feature wasn't implemented yet; it already was) — see Phase 15's entry.

## Expansion plan (Master Feature Expansion Prompt) — separate from the old Stage 1-10 checklist below
- ✅ Phase 1 — Audit: read the existing project (data layer, nav, Home, Settings) before writing anything. Found `GoalScope`/`PlannedSessionStatus`/`TaskPriority` enums already committed with KDoc anticipating `StudyGoalEntity`/`PlannedSessionEntity`/`TaskEntity` — those tables didn't exist yet; this phase adds the first of them.
- 🔶 Phase 2 — Study Goals: **written, unverified by CI/device.** New `study_goals` Room table (migration 1→2, existing `study_sessions` data untouched), `GoalRepository`, `GoalProgress` (pure, unit-tested), a new Goals screen (daily progress reusing the existing Settings daily-goal value, an editable weekly goal, an honest "coming with Subjects" placeholder instead of a fake subject-goal button), reached from a new Home quick-action tile. Also fixed a latent bug while touching `HomeScreen`'s quick-actions grid: its height was a fixed 300.dp sized for 2 rows, which already clipped the 5th tile's row before this change; it's now computed from the actual row count. **Not yet done:** Subject-scoped goals (needs Phase 3's Subject system first — `subjectId` column exists on `StudyGoalEntity` and is reserved for it), and no CI run has confirmed this compiles yet.
- 🔶 Phase 3 — Subjects: **written, unverified by CI/device.** New `subjects`
  table (migration 2→3), and `study_sessions` rebuilt in the same migration
  to add a nullable `subjectId` foreign key (`SET_NULL` on delete — deleting
  a subject un-attributes its past sessions rather than deleting them).
  `SubjectRepository` (CRUD + icon/color presets), `SubjectStats` (pure,
  unit-tested), a Subjects list screen (create via a shared editor dialog)
  and a Subject Detail screen (colored header, stats grid reusing the now-
  shared `StatMiniCard`, rename/delete via the same dialog), reached from a
  new Home quick-action tile. **Not yet done:** nothing in the timer flows
  (Self-Study, Pomodoro, Focus, etc.) lets the user actually pick a subject
  when starting a session yet — `recordSession()` grew an optional
  `subjectId` parameter that defaults to `null` so every existing call site
  still compiles unchanged, but nothing calls it with a real value yet.
  That's timer integration, explicitly a separate later step in the
  document's own implementation order. Subject-scoped goals (the
  `GoalScope.SUBJECT` case added in Phase 2) also still aren't wired up —
  Subject Detail doesn't offer a "set a goal for this subject" action yet.
- 🔶 Phase 4 — Tasks & topics: **written, unverified by CI/device.** New
  `tasks` table (migration 3→4), `study_sessions` rebuilt a second time to
  add a nullable `taskId` foreign key. `TaskRepository` (CRUD + mark
  complete), `TaskDueInfo`/`computeTaskDueInfo` (pure due-date urgency,
  unit-tested), a Tasks list screen (checkbox to complete, tap to edit,
  create/edit via a shared `TaskEditorDialog` with subject/priority/
  deadline-preset/duration-preset chips), reached from a new Home
  quick-action tile. **Scope calls made this phase, both flagged to the
  user in the reply:** (1) "Subject → Chapter → Topic → Task" is modeled as
  two free-text fields (`chapter`, `topic`) on `TaskEntity`, not two more
  full entities with their own CRUD — the spec's own "Task features" list
  only asks for those two as attributes, not as managed things in their own
  right; (2) deadline/estimated-duration use fixed preset chips (same
  pattern as the weekly-goal picker), not a calendar `DatePicker` — this
  project has never used Material3's `DatePicker` (still experimental at
  the pinned material3 1.2.1) and a preset picker is lower-risk while
  covering the same real need. **Not yet done:** no timer screen lets the
  user start a session from a task yet, even though the spec asks for it
  under this same phase — `recordSession()` grew an optional `taskId`
  param (default `null`, same pattern as Phase 3's `subjectId`) so it's
  ready, but actually wiring "start timer from a task" needs the timer
  screens themselves to accept a task/subject, which is explicitly its own
  "Timer integration" step later in the document's own implementation
  order — doing it properly here would mean touching every timer mode in
  the same phase as introducing the Task entity, which is exactly the
  "don't implement every feature in one giant change" the document itself
  warns against. A Subject Detail "tasks for this subject" section was
  considered but not added, to keep this phase's footprint contained —
  `TaskDao.tasksForSubject` already exists for whenever that's wanted.
- 🔶 Phase 5 — Advanced timer system: **written, unverified by CI/device.**
  The bulk of "support Stopwatch/Countdown/Pomodoro/Custom/Focus/breaks"
  already existed from earlier stages; this phase's real additions:
  (1) **subject/task selection wired into every timer mode** — Self-Study,
  Online Study, Normal Timer, Pomodoro, and Focus Mode all now show a
  shared `SessionAttributionPicker` (optional custom label, optional
  subject, optional task — filtered to that subject if one's picked) while
  idle/in setup, and `recordSession()`'s `subjectId`/`taskId` params from
  Phases 3-4 are finally passed real values instead of always `null`. This
  was the single most load-bearing gap: without it, every Subject/Task
  stat screen built in Phases 3-4 would stay at zero forever. Selecting a
  task auto-selects its subject; changing the subject away from a selected
  task's subject clears that task, so the pairing can't go inconsistent.
  For Pomodoro specifically, the choice is made once per cycle (before the
  first Work phase) and carries across every Work/Break/Work transition
  rather than being asked again each phase, and a custom label only
  renames the *saved session's* label, not the live "Pomodoro — Work"
  phase chrome. (2) **Pomodoro's "sessions before a long break" is now a
  user setting** (2/3/4/5/6 preset chips, replacing the old fixed
  `SESSIONS_PER_LONG_BREAK = 4` constant — renamed
  `DEFAULT_SESSIONS_PER_LONG_BREAK` and kept as the default). **Still open
  from this phase's own feature list** (explicitly deferred, not silently
  dropped): auto-start next phase/break toggles (every mode still requires
  an explicit tap for the next phase — this is a real, deliberate
  behavior choice already documented in `PomodoroViewModel`'s class doc,
  not an oversight, but the master prompt does ask for it as a toggle);
  dedicated "optional sound"/"optional vibration" settings distinct from
  the existing "Timer completion alerts" notification toggle (Settings
  currently controls one combined alert, not three separate channels);
  persisting Pomodoro's duration/session-count choices across an app
  restart (still in-memory only, same limitation Stage 7 originally
  documented and this phase didn't revisit); and the deeper
  "notification controls" (per-notification-type on/off) — **now done in
  Phase 12** (separate sound/vibration toggles remain open; spec places
  them in Phase 15).
- 🔶 Phase 6 — Study planner: **written, unverified by CI/device.** New
  `planned_sessions` Room table (migration 4→5 — purely additive, no
  `study_sessions` rebuild, lowest-risk migration so far), `PlannedSessionEntity`/
  `PlannedSessionDao`/`PlannedSessionRepository` (CRUD + `refreshMissedSessions`,
  which flips any `PLANNED` plan whose time window has fully passed to
  `MISSED` — called once from `PlannerViewModel.init`), pure `PlannedSessionTime`
  helpers (`formatMinuteOfDay`/`formatPlannedTimeRange`/`formatPlannedDuration`,
  unit-tested in `PlannedSessionTimeTest.kt`, same pattern as `GoalProgress`/
  `TaskDueInfo`), a Planner screen (today's plan list with Planned/Completed/
  Missed counts, Start/Mark done/Cancel actions per row, an "Upcoming"
  section for future plans) and a shared `PlannedSessionEditorDialog`
  (date/start-time/duration preset chips + the same subject/task chip
  picker pattern `TaskEditorDialog`/`SessionAttributionPicker` use), reached
  from a new Home quick-action tile.

  **"Start directly from the planner"** (explicitly asked for in the spec's
  Phase 6 section) is real: tapping Start on a `PLANNED` row navigates to a
  new `Screen.TimerFromPlan` route (a second route, not optional query args
  bolted onto the existing `Screen.Timer` — chosen specifically so
  `Timer`'s existing bottom-nav navigation/route-matching stays completely
  untouched), which opens the Timer screen's Self-Study tab with the plan's
  subject/task pre-selected via Phase 5's already-existing
  `StopwatchTimerViewModel.selectSubject`/`selectTask`. **Deliberate scope
  decision, flagged rather than silently dropped:** the resulting session
  is *not* auto-detected and linked back to mark that plan `COMPLETED` —
  there's no `linkedSessionId` column and no heuristic matching a saved
  session back to a plan. The user marks a plan "Mark done" themselves from
  the Planner screen after finishing. A real link (an explicit FK column +
  wiring through every timer ViewModel's save path, the same shape Phase
  5's subject/task wiring already took) is a contained follow-up if wanted,
  not a design dead-end — see `PlannedSessionEntity`'s class doc for the
  full reasoning. Missed-detection needed no such link: it's computed
  straightforwardly from whether a `PLANNED` plan's time window has passed,
  which needs nothing from the timer side at all.

  Also scoped down from the spec's own example: date/start-time/duration
  all use fixed preset chips (6 AM–10 PM half-hour start times, 30m–3h
  durations, Today/Tomorrow/+2d/+3d/+1wk dates) rather than a calendar/time
  picker widget — same reasoning as every other date/duration picker this
  project has built (`TaskEditorDialog`'s deadline chips, the weekly-goal
  picker): this project has never used Material3's still-experimental
  `DatePicker`/`TimePicker` at the pinned material3 1.2.1, and a wrong
  guess about an unfamiliar experimental API is a worse risk than a
  slightly less flexible preset picker. A plan whose window crosses
  midnight (a late, long block) is handled honestly in the display
  (`formatPlannedTimeRange` appends "(+1d)" to the end time) rather than
  silently mis-showing it.

  **Verification status: written, not compiled** — same caveat as every
  phase. On the next CI run, pay particular attention to: (1) `MIGRATION_4_5`
  applying cleanly on top of a v4 database and on a fresh install straight
  to v5, (2) the new `Screen.TimerFromPlan` route actually matching and
  extracting `-1L`-sentinel args correctly (untested pattern for this
  project, though it mirrors `SubjectDetail`'s already-used
  path-arg-with-`NavType.LongType` shape closely), (3) a plan created with a
  far-future date offset (`+1 week`) still sorts correctly into "Upcoming"
  and not "Today", (4) `refreshMissedSessions` correctly leaves alone a
  `PLANNED` plan later today that hasn't started yet (only past-window
  plans should flip to `MISSED`).
- 🔶 Phase 7 — Planned vs actual analytics: **written, unverified by
  CI/device.** New pure aggregation `computePlannedVsActual` in
  `data/repository/PlannedVsActual.kt` (`PlannedVsActual`/
  `PlannedVsActualAnalytics` data classes) — same "recompute a small
  already-fetched dataset in plain Kotlin, unit-test the pure function"
  shape as `SessionRepository.computeSubjectStats`/`computeStreak`, unit
  tested in `PlannedVsActualTest.kt` (empty input, today-only bucketing,
  cancelled-plan exclusion, future-dated-plan exclusion, week-boundary
  edge, over-100%-completion not clamped). No new migration: both DAOs
  gained a `sessionsSince(sinceEpochDay)` query mirroring
  `StudySessionDao.sessionsSince`'s existing shape (`PlannedSessionDao`
  didn't have one yet; `StudySessionDao`'s already existed but wasn't
  exposed through `SessionRepository` outside its own internal uses, so
  that repository gained a one-line public wrapper). `AnalyticsViewModel`
  combines both 30-day-window flows into a `plannedVsActual` `StateFlow`,
  and `AnalyticsScreen` gained a new "Planned vs Actual" section — three
  stacked cards (Today / This week / This month), each with Planned /
  Actual / Completion%, in both the portrait and landscape layouts.

  **Deliberate scope decision:** completion percent is **not** capped at
  100% — studying more than was planned is reported honestly as e.g. 200%
  rather than silently clamped to look like "on track", matching the
  spec's own Phase 8 instruction not to make unsupported/misleading claims
  from the data. Every bucket (today/week/month) is bounded through today
  only — a plan dated next week doesn't inflate "this month"'s planned
  total before that day has actually arrived; see `PlannedVsActual.kt`'s
  class doc for the full reasoning.

  **Verification status: written, not compiled.** On the next CI run, pay
  particular attention to: (1) the `combine()` in `AnalyticsViewModel`
  actually re-emitting when either underlying Room flow changes (untested
  combination of two different repositories' flows — every prior
  `combine()` in this codebase, e.g. `SessionRepository.studyStats`,
  combines two flows from the *same* DAO), (2) a month with zero planned
  time still showing "—" rather than a crash or "0%" that could misread as
  "you completed nothing" when really nothing was planned, (3) the
  completion percent display at very large over-100% values (e.g. someone
  who planned 15 minutes and studied 3 hours) doesn't visually break the
  card layout.
- 🔶 Phase 8 — Advanced analytics: **written, unverified by CI/device.**
  New `data/repository/AdvancedAnalytics.kt`: `AnalyticsRange` enum (Today/
  7/30/90 days/All time), `AdvancedStats`/`SubjectTotal`/
  `ProductivityPatterns` data classes, and four pure functions
  (`sessionsInRange`, `computeAdvancedStats`, `computeSubjectTotals`,
  `computeProductivityPatterns`) — same "recompute a small dataset in
  plain Kotlin, unit-test the pure function" shape every prior analytics
  phase used, unit tested in `AdvancedAnalyticsTest.kt` (14 cases: range
  filtering boundaries, all-zero-safe empty input, weekly-average `null`
  below a 7-day window, all-time daily average measured from the earliest
  real session rather than a fixed constant, Pomodoro-completion counting,
  completion-rate excluding stopwatch modes, subject grouping including
  unattributed sessions, most-productive-day ranked by duration not count,
  consistency capped at 100%). One new DAO query,
  `StudySessionDao.allSessions()` (unbounded, exposed via a new
  `SessionRepository.allSessions()` wrapper) — no migration, since it's a
  `SELECT *` with no new column.

  `AnalyticsViewModel` gained a `selectedRange` `StateFlow` (default 7
  days) plus `advancedStats`/`subjectTotals`/`productivityPatterns`, all
  three derived from one shared `filteredSessions` flow so they can never
  disagree about which sessions are "in range." `AnalyticsScreen` gained a
  range-selector chip row and three new sections — Overview (8-stat grid:
  total, sessions, average, longest, completed Pomodoros, completion rate,
  daily average, weekly average), By Subject (per-subject totals including
  an explicit "No subject" bucket), and Productivity Patterns (most
  productive day/time-of-day, study consistency %, session completion
  rate) — in both the portrait and landscape layouts. The pre-existing
  "This Week" chart/breakdown and "Planned vs Actual" sections keep their
  own fixed windows (7 and 30 days) and are **not** affected by the new
  range selector — a deliberate scope boundary, not an oversight, so two
  already-shipped, already-documented sections didn't need retrofitting
  onto a selector built for the three new ones.

  **Deliberate honesty-over-completeness calls, per the spec's own "do not
  make unsupported claims... present statistics as descriptive data"
  instruction for this phase:** weekly average is `null` (rendered as "—"),
  not a misleading extrapolation, for any range under 7 days (i.e.
  "Today", or an "All time" install less than a week old). Completion rate
  only considers modes with an actual target to reach (Normal/Pomodoro/
  Focus), excluding the two open-ended stopwatch modes, which can never
  complete "naturally" by design and would otherwise drag the number down
  for a reason unrelated to actually finishing anything. Most productive
  day/hour rank by total *duration*, not session *count*, so one long
  session can't be out-ranked by several short ones the way a naive count
  would. Every Productivity Patterns field shows "Not enough data yet"
  rather than fabricating a pattern when there's too little history to say
  anything.

  **Verification status: written, not compiled.** On the next CI run, pay
  particular attention to: (1) `AnalyticsViewModel`'s `filteredSessions`
  actually re-emitting for every `selectedRange` chip tap (a `combine()` of
  a Room flow with a plain `MutableStateFlow`, a shape not used exactly
  this way anywhere else in the codebase yet), (2) the 2-hour "most
  productive time" buckets reading device-local hours correctly across a
  timezone boundary (uses `ZoneId.systemDefault()`, same as every other
  local-time computation in this app, but this is the first one bucketing
  by hour-of-day specifically), (3) the Overview grid's 8 cards laying out
  cleanly at 2 columns on a narrow phone without text clipping (the
  longest label, "Completed Pomodoros", is the one to check first), (4)
  `allSessions()` performance on an install with a genuinely large session
  history — this is the first query in the app with no date/limit bound at
  all, and Phase 19 (Performance) is explicitly where this class of
  concern is meant to be revisited, not this phase.
- 🔶 Phase 9 — Streaks & achievements: **written, unverified by CI/device.**
  New `data/repository/Achievements.kt`: `StreakSummary`/`AchievementId`
  (the 8 spec achievements, titles+descriptions defined once so the data
  and UI layers can't drift)/`Achievement` data classes, and four pure
  functions — `computeBestStreak` (longest-ever run, deliberately separate
  from the existing live `SessionRepository.computeStreak` — see its KDoc
  for why a broken streak must not revoke a past 7/30-day badge),
  `computeWeeklyConsistency`, `computeGoalEverMet`, `computeAchievements` —
  unit tested in `AchievementsTest.kt` (16 cases: best-streak found deep in
  history not just the most recent run, a later break not erasing an
  earlier longer run, consistency rounding, goal-met via either a single
  strong day or the current week, all-locked-at-zero-history, and each
  threshold's exact boundary). New `screens/achievements/
  {AchievementsScreen,AchievementsViewModel}.kt`, reached from a new Home
  quick-action tile (Home is now 10 tiles, an even number — 5 clean rows
  at 2 columns).

  **No new Room query, no migration.** Every number this phase needs
  already exists: `SessionRepository.allSessions()` (Phase 8),
  `TaskRepository.allTasks()` (Phase 4), `SettingsRepository.dailyGoalMinutes`
  (pre-expansion Stage 8), `GoalRepository.weeklyGoal()` (Phase 2) — this
  phase is pure composition over four already-shipped flows.

  **Deliberate design call: no persisted "unlocked" table.** Every
  achievement except Goal Completed is monotonic (counts/hours only rise;
  `computeBestStreak` already scans all of history, not just the live
  streak), so recomputing live from real data on every screen open gives
  the exact same *stable* answer a stored unlock flag would — with no
  migration, no write path, and no way for a stored flag to ever disagree
  with the real numbers behind it. Goal Completed is the one exception:
  see `computeGoalEverMet`'s KDoc for the specific, disclosed
  simplification (checks history against the *current* goal target, not
  whatever the target happened to be historically, since past goal
  *targets* aren't stored — only past goal *progress* is).

  **Explicit anti-pressure care, per the spec's own "avoid rewards that
  pressure users to study continuously... do not encourage unhealthy
  over-studying" instruction for this phase:** achievement cards state a
  plain fact (title + one descriptive sentence) with no urgency language,
  no "don't break the chain" framing, no push notification tied to
  streaks, and no visual treatment (badges, confetti, red warning colors
  on an about-to-break streak) that would nudge toward compulsive daily
  use. This was a deliberate check made while writing `AchievementsScreen.kt`,
  not an incidental side effect of keeping the UI simple.

  **Verification status: written, not compiled.** On the next CI run, pay
  particular attention to: (1) `AchievementsViewModel`'s two separate
  `combine()` blocks (`streakSummary`/`achievements`) both compiling
  against the 4-flow `combine()` overload correctly, (2) `weeklyGoalMinutes`
  reading `null` cleanly (no goal set) vs. an actual `Int` without a
  type-inference surprise in the `combine` lambda, (3) the achievement list
  rendering all 8 cards without clipping at typical phone widths, (4) a
  fresh install with zero sessions/tasks showing a sensible all-zero,
  all-locked state rather than a crash from an empty list somewhere in the
  aggregation (covered by `AchievementsTest`'s "all locked with zero
  history" case, but only at the pure-function level, not yet through the
  real ViewModel/Flow/Compose stack).
- 🔶 Phase 10 — Daily dashboard: **written, unverified by CI/device.** Home
  reorganized to match the spec's own section order: Today (existing
  progress ring/card, kept as-is — it already matched), streak (existing
  "Streak" stat card, now shows a 🔥 prefix), new **Quick Start** row (3
  buttons: Stopwatch, Pomodoro, Custom — see below for how "Custom" gets
  its own Timer tab), new **Subject Breakdown** (today's per-subject
  totals), new **Today's Plan** (up to 3 of today's planned blocks), new
  **Tasks** (up to 5 open tasks, soonest deadline first), then the
  pre-existing 10-tile quick-action grid — relabeled "All Features" rather
  than "Quick Actions" now that "Quick Start" exists as a more prominent,
  narrower entry point — and Recent Activity, both kept exactly as before.
  Both portrait and landscape layouts updated; landscape splits Today/
  streak/Quick Start into the left pane and the four new/existing list
  sections into the right pane.

  **Nothing removed, per the spec's own Phase 10 rule "the dashboard
  should remain clean and not overcrowded" balanced against the master
  prompt's Rule 2 ("do not remove existing working features")**: all 10
  existing quick-action tiles are still there and still work exactly as
  before, just under a renamed, lower-priority header beneath four new,
  higher-priority sections — a reorganization and addition, not a
  deletion. Every new number reuses an existing repository query — no new
  migration, no new Room `@Query` at all: `todaySubjectTotals` reuses
  `SessionRepository.sessionsSince` (Phase 7) with today's own epoch day
  as the lower bound (no real session is ever future-dated, so that's
  already just "today's sessions") through Phase 8's `computeSubjectTotals`;
  `todaysPlans` reuses `PlannedSessionRepository.forDate` (Phase 6);
  `remainingTasks` reuses `TaskRepository.allTasks` (Phase 4), filtered/
  sorted client-side.

  **"Start Custom Timer" needed one small, contained navigation addition:**
  a new `Screen.QuickStartTimer` route (`quick_start_timer/{tab}`) and a
  new `TimerScreen` parameter, `initialTab: Int?`, applied via its own
  `LaunchedEffect` — a third, separate route rather than extending
  `Screen.Timer` or `Screen.TimerFromPlan`, same reasoning `TimerFromPlan`
  already documented in Phase 6: touching an existing route's shape risks
  the bottom nav or an existing deep link, a brand new route can't.
  "Start Stopwatch" needed **no** navigation change at all — the Timer
  screen already defaults to tab 0 (Self-Study), which is exactly what
  "Stopwatch" means, so it just calls the existing `onOpenTimer`. "Start
  Pomodoro" also needed nothing new — it's `onOpenPomodoro`, already wired
  since Stage 3.

  **Verification status: written, not compiled.** On the next CI run, pay
  particular attention to: (1) `Screen.QuickStartTimer`'s `{tab}` path arg
  round-tripping correctly (an `Int` path arg is a new shape for this
  app's nav — every prior custom route used `Long` path args or none), (2)
  the reorganized Home screen's total scroll length on a small phone —
  this phase added four sections without removing any, so Home is now
  meaningfully longer than before, worth an actual scroll-through check
  rather than just a glance, (3) the landscape two-pane split still
  feeling balanced with the new sections concentrated in the right pane,
  (4) `TodaysPlanSection`'s "+N more today" line actually appearing only
  when there are more than 3 plans, not off-by-one.
- 🔶 Phase 11 — Daily summary: **written, unverified by CI/device.** New
  pure `data/repository/DailySummary.kt` (`DailySummary` data class,
  `computeDailySummary`, `dailySummaryDateLabel`), a new
  `screens/summary/{DailySummaryScreen,DailySummaryViewModel}.kt`, a new
  `Screen.DailySummary` route, and a new Home quick-action tile ("Daily
  Summary", Home is now 11 tiles). Shows exactly the spec's fields — total
  time, sessions, subjects, goal progress (reusing `GoalProgress`/
  `GoalProgressCard`, so it can't disagree with Home/Goals about "90%"),
  top subject, tasks completed — with previous/next-day arrows (next is
  disabled on today). No motivational text, no scores, no cross-day
  comparison. "Optional" = the user opens it from Home; nothing pops it up
  (a summary *notification* belongs to Phase 12's opt-in reminders).

  **One migration, and why it was unavoidable:** `TaskEntity` had only a
  `completed` boolean, so "N tasks completed *today*" could not be answered
  at all. `MIGRATION_5_6` adds a nullable `tasks.completedAtEpochMillis`
  (plain `ALTER TABLE ADD COLUMN` — nullable, no foreign key, so no table
  rebuild, unlike 2→3/3→4). `TaskRepository.setCompleted` now goes through
  the pure, unit-tested `applyTaskCompletion` (stamps on complete, keeps the
  original stamp on a repeat-complete, clears on re-open). **Tasks that were
  already completed before this update keep `NULL` on purpose** — no
  completion time was ever recorded for them and inventing one would be a
  fabricated statistic. They still count in Achievements' all-time "tasks
  done" but never in any single day's summary. Say this to the user if they
  ask why yesterday's completed tasks show 0.

  **Disclosed simplification:** a past day's goal % is measured against the
  *current* daily goal (past targets aren't stored — same call Phase 9's
  `computeGoalEverMet` already makes). The screen states this on any
  non-today day.

  **Performance note (Phase 19 groundwork):** unlike Analytics/Achievements,
  this screen does **not** read the unbounded `allSessions()`. The ViewModel
  `flatMapLatest`es on the selected date into the existing
  `sessionsSince(date)` query, so today/yesterday load only recent rows.

  **Verification status: written, not compiled.** Watch on the next CI run:
  (1) `MIGRATION_5_6` applying cleanly on a v5 database *and* a fresh
  install straight to v6 (Room validates the column as `INTEGER`, nullable,
  no default — the migration's SQL is written to match that exactly);
  (2) `DailySummaryViewModel`'s `flatMapLatest { combine(...) }` re-emitting
  on both a day change and a session/task write (first `flatMapLatest` over a
  Room flow in this app — `TimerForegroundService` uses it over a plain
  StateFlow); (3) `DailySummaryTest`/`TaskCompletionTest` (23 cases) —
  expectations were hand-checked against the implementation and Python for
  the date/percent/timezone arithmetic, but never run; (4) the 3-across
  `StatMiniCard` row at large font scales (same pattern as Achievements'
  totals row); (5) Home's tile grid — it already uses a fixed 132dp per row
  regardless of font scale, a pre-existing risk now one tile longer, to be
  handled in Phase 16 (Accessibility)/22 (UI QA), not here.
- 🔶 Phase 12 — Notifications & reminders: **written, unverified by CI/device.**
  Spec asks for: planned session, upcoming session, goal progress, optional
  daily reminder, completed session; each individually disable-able;
  respect permissions/platform limits; not excessive. What exists now:
  - **Planned session reminders** (at start time) and **Upcoming session
    heads-up** (`UPCOMING_LEAD_MINUTES` = 10 before) — separate toggles.
  - **Daily study reminder** — optional, time chosen from presets 7 AM–9 PM
    (deliberately nothing later, per the spec's no-sleep-deprivation rule),
    and *skipped on a day the user has already studied*.
  - **Goal reached** — one toggle covering the daily and the weekly goal;
    fires only on the *crossing*, at most once/day (daily) and once/7 days
    (weekly, rolling window — same one Home/Goals call "this week"). No
    "halfway"/"almost there" nudges, on purpose.
  - **Completed session** — already existed (Stage 8's "Timer completion
    alerts", default on, unchanged); now sits alongside the new toggles.
  - **All four new toggles default OFF** so updating never starts notifying
    existing users; opting in is one tap in Settings → Notifications, and
    that tap is also where the Android 13+ `POST_NOTIFICATIONS` request now
    happens (in context, right after the user said they want notifications).
    If notifications are blocked system-wide, a card in Settings says so and
    opens the system notification settings; it re-checks on every resume.
  - Three notification **channels** (Study Reminders = default importance;
    Daily Reminder and Goal Progress = low/silent) so each can also be tuned
    in system settings. Wording is plain and factual; nothing pressuring.

  **How scheduling works (read `reminders/ReminderPlanner.kt`'s header):**
  one `AlarmManager` alarm, always aimed at the *next* reminder, re-derived
  from DB + settings whenever a plan or setting changes
  (`ReminderScheduler.startObserving`, started in `Application.onCreate`),
  after each firing, at app start, and after reboot/app update
  (`BootReceiver`). All the logic that decides *what* and *when* is pure and
  unit-tested (`computeNextReminder`, `remindersDueAt`, `minutesUntil`,
  `shouldNotifyGoalReached`, `describePlannedSession`); the Android classes
  only translate it. The alarm carries its *scheduled* instant and the
  receiver matches every reminder sharing it, so two plans starting at the
  same minute both notify.

  **Deliberate platform choices, flagged not hidden:**
  (1) **Inexact alarms** (`setAndAllowWhileIdle`, no special permission).
  Android's docs: never fires before the trigger time, but on Android 12+ may
  fire up to an hour late under battery saver/Doze. Exact alarms would need
  the user to grant "Alarms & reminders" access (`SCHEDULE_EXACT_ALARM`) — a
  heavier ask for a study reminder — so the Settings screen states plainly
  that reminders can arrive a few minutes late. The receiver copes: the
  heads-up states the *real* minutes remaining and is dropped once the
  session has started; a start reminder is dropped if the whole session
  window has passed. If exact timing is ever wanted, it's a contained change
  in `ReminderScheduler.aimAlarm` plus the permission/UX.
  (2) **New permission `RECEIVE_BOOT_COMPLETED`** — normal/install-time, no
  prompt. The OS clears every alarm on reboot, so without it tomorrow's
  reminder would silently vanish if the phone restarted overnight.
  `BootReceiver` only re-aims the alarm (also on `MY_PACKAGE_REPLACED`); it
  starts no service and posts nothing. This intentionally supersedes the
  Stage 1 note "no boot receiver" (which said the permission belonged in
  the stage that actually needed it — this is that stage).
  (3) A reminder whose time already passed is **dropped, never sent late** —
  e.g. a plan created 3 minutes before it starts skips the 10-minute
  heads-up but still gets the start reminder.

  **Phase 11's deferred "summary notification" is not built**: Phase 11
  left it for this phase, but Phase 12's own spec list (planned session,
  upcoming session, goal progress, daily reminder, completed session)
  doesn't include one, and an extra end-of-day notification would work
  against "do not create excessive notifications". The Daily Summary screen
  stays on-demand; if the user asks for a notification for it, the natural
  shape is a fifth opt-in toggle that reuses the daily-reminder machinery.

  **Not done, deliberately:** separate *sound*/*vibration* toggles (the
  spec lists them under Phase 15, Themes & Customization, and the app still
  doesn't request `VIBRATE`); tapping a notification opens the app, not the
  Planner screen (a deep link would need `MainActivity` intent-extra
  handling in the NavHost — contained follow-up); no re-aim on a timezone/
  clock change (it self-corrects on the next app start or plan/setting
  edit); no per-subject or per-plan custom lead time.

  **Verification status: written, not compiled.** Watch on the next CI run:
  (1) manifest merges and both receivers resolve (`.reminders.*`);
  (2) `LocalLifecycleOwner` — imported from `androidx.compose.ui.platform`,
  correct for the pinned compose BOM 2024.06 (Compose UI 1.6); it moved to
  lifecycle-runtime-compose in Compose 1.7, so this import needs changing
  if the BOM is ever bumped; (3) `ReminderPlannerTest` (30 cases) — expectations
  hand-checked (DST offsets for America/New_York around 2026-03-08, ceil
  minute arithmetic, tie/boundary cases) but never run; (4) on a device:
  enable "Planned session reminders", create a plan ~12 minutes ahead, confirm
  the permission prompt on Android 13+, the notification at start, and that
  toggling it off removes it; force-stop/reboot behavior; deny the permission
  and confirm the Settings card appears; (5) the daily-reminder chip row
  scrolls at large font scales.
- 🔶 Phase 13 — Data export/import: **written, unverified by CI/device.**
  Reached from **Settings → Data → "Export & import data"** (new route
  `Screen.DataManagement`, screen `screens/data/DataManagementScreen.kt`).
  Phase 14 (Backup & restore) now lives on this same screen — see its own entry below.
  - **Export** (spec: CSV/JSON; date, start, end, duration, subject, task,
    session type): **CSV** = one row per session, columns `Date, Start time,
    End time, Duration (seconds), Subject, Task, Session type, Label,
    Completed`, UTF-8 with BOM (so Excel reads accents), CRLF, oldest first.
    **JSON** = full fidelity: exact-millisecond sessions with their stored
    day, plus all subjects and tasks (`format: "studyspace-export"`,
    `version: 1`). Both use the system "save as" picker (Storage Access
    Framework) → **no storage permission**.
  - **Import** (spec: validate before saving; never overwrite without
    confirmation): pick a file → it's parsed and **validated**, then a
    **preview** shows exactly what will be added (new sessions, duplicates
    skipped, subjects/tasks to be created, sessions that will lose a task
    link, and every rejected entry with its row/position and reason) →
    nothing is written until the user taps the confirm button. Format is
    detected from the *content*, not the file name/MIME (phones report CSVs
    as anything).
  - **Import only ever adds.** There is no code path that edits, replaces
    or deletes an existing row — so "never overwrite" is structural, not a
    prompt. Replacing everything with a saved copy is a different operation
    and is Phase 14's job. The write is one Room transaction (all or
    nothing) and re-plans against the database at confirm time so a
    duplicate can't slip in if data changed after the preview.
  - **Duplicate rule:** same start, duration and session type **at second
    precision** (CSV can't carry milliseconds). Re-importing your own
    export adds nothing; duplicates *within* a file are also collapsed.
  - **Subjects** match existing ones by trimmed, case-insensitive name and
    are created (icon/colour from JSON, else 📚 + a default colour) only if
    missing. **Tasks** match by title + the task's subject; a session links
    to a task by that key, falling back to a *unique* title (so CSV, which
    knows only the session's subject, still links when unambiguous) and is
    left unlinked — never guessed — if ambiguous or absent.
  - **Validation** (all reported, none silent): session type must be a real
    `SessionType` (friendly spellings like "Self-Study" accepted); duration
    10 s – 24 h (10 s = the app's own recording minimum); start not before
    2000 or more than 24 h in the future; date/time formats; text lengths;
    JSON date must be within 1 day of the start's UTC day; unknown task
    priority is an error, absent defaults to MEDIUM; whole-file failures
    (empty, missing columns, not JSON, wrong `format`, `version` newer than
    supported, unclosed CSV quote, > 10 MB) show one clear message and
    import nothing.
  - **Security:** CSV export prefixes user text that starts with `= + - @`
    tab or CR with an apostrophe (spreadsheet formula injection guard);
    import strips exactly that guard, so text round-trips.
  - **Imported history does not fire goal notifications** (inserted through
    the DAOs, not `SessionRepository.recordSession`). Achievements/streaks/
    analytics are computed from stored data, so they include imported
    sessions automatically.

  **Deliberate limits, disclosed in the UI or here:** CSV is lossy by
  nature (second precision; the day is derived in the exporting device's
  time zone, so re-importing a CSV on a device in a *different* zone can
  shift times and won't dedupe — the screen recommends JSON for moving
  between devices); CSV can't recreate tasks (only link to existing ones);
  export covers **study history only** — planned sessions, goals,
  achievements and settings are Phase 14's backup; no "replace" import
  mode; no per-row editing of rejected entries.

  **Verification status: written, not compiled.** Watch on the next CI run:
  (1) **`testImplementation("org.json:json:20240303")`** — Android's SDK
  ships only a stub `org.json`, so unit tests need the real library. If
  `StudyDataExportTest`/`StudyDataImportTest` fail with "Method … not
  mocked", the real jar isn't ahead of the SDK stub on the test classpath.
  Do **not** "fix" that with `unitTests.isReturnDefaultValues = true` — it
  makes the stub return nulls, which would make the tests pass while testing
  nothing. Instead put the JSON reading/writing behind a tiny interface
  with a dependency-free implementation, or switch to kotlinx.serialization. (2) Note the real
  library is *stricter* than Android's lenient `org.json` — tests prove the
  strict path; on-device a malformed-but-lenient file could parse where the
  test wouldn't (harmless: every field is still validated). (3)
  `ActivityResultContracts.CreateDocument(String)` (activity 1.9.1 ✓
  constructor exists). (4) `withTransaction` (room-ktx 2.6.1 ✓ present).
  (5) Tests: `CsvCodecTest` (14), `StudyDataExportTest` (13),
  `StudyDataImportTest` (44) — expectations hand-verified line by line
  against the implementation (that pass caught two wrong assertions before
  they were ever run) but never executed. (6) On a device: export CSV and
  open it in a spreadsheet app; export JSON and re-import it (expect "0 new,
  N already in the app"); import a hand-edited file with bad rows and check
  the preview lists them; import into a fresh install and confirm subjects/
  tasks/links come back.
- 🔶 Phase 14 — Backup & restore: **written, unverified by CI/device.**
  Lives on the same **Settings → Data** screen as Phase 13, under a new
  "Backup & restore" section. New files `data/transfer/StudyDataBackup.kt`
  (pure build/parse) and additions to `DataTransferRepository`,
  `DataManagementViewModel`/`Screen`, `SettingsRepository`. **No new Room
  migration** — restore reuses the existing tables as-is (see "Data model"
  below for why that's deliberate).
  - **A backup is a full-fidelity replace artifact**, the opposite of
    Phase 13's always-additive import: it carries subjects, tasks, study
    sessions, planned sessions, the weekly goal, and the DataStore-backed
    settings/theme choices — everything the app stores except achievements
    (computed from the above, never stored, so nothing to carry) and a
    custom "Personalize" gallery photo (a private file already on-device;
    deliberately not base64-inflated into the backup — disclosed in the
    screen's own copy).
  - **File format:** nests a full `studyspace-export`-shaped object (Phase
    13's own format) under a `"studyData"` key inside a `studyspace-backup`
    (`version: 1`) JSON file, alongside `plannedSessions`, `weeklyGoalMinutes`,
    `settings` and `theme`. **Reuses `parseJsonExport` for the nested
    section** (widened from `private` to `internal` for this) rather than
    re-implementing subject/task/session validation — the exact same
    strictness rules from Phase 13 apply there. Cross-format mistakes get a
    specific message in both directions: an export file dropped into
    Restore says "use Import instead"; a backup file dropped into Import
    says "use Restore backup instead".
  - **Create backup / See backup status:** "Create backup" writes the JSON
    via the system "save as" picker (SAF, no storage permission) and
    records the timestamp in a new DataStore key
    (`SettingsRepository.lastBackupEpochMillis`); the screen shows "Last
    backup: <date>" or "No backup yet".
  - **Restore backup** (spec: create/restore/status/cancel safely): pick a
    file → parsed and validated (study data as strictly as Import; planned
    sessions and the weekly goal validated per-entry, bad ones reported and
    skipped; settings/theme read *leniently* — a missing/bad field falls
    back to a safe default rather than failing the whole file, since losing
    one toggle is recoverable and losing years of history to a formatting
    slip is not) → a **preview** states the counts, warns in red if the
    device currently has sessions ("this replaces all of it — not add to
    it"), and lists anything that couldn't be restored → tapping "Restore
    backup…" opens a **second, explicit** "yes, replace everything"
    `AlertDialog` → only *that* confirm calls
    `DataManagementViewModel.confirmRestore`, which is the only thing that
    writes. **Cancel is safe at every point up to that confirm** — the read
    and parse are pure functions that touch nothing, so dismissing the
    preview, the dialog, or an error leaves the database exactly as it was;
    there's no partial/interruptible write state to "cancel out of" because
    the actual write is a single Room transaction (see next point).
  - **Restore replaces, atomically:** `DataTransferRepository.restoreBackup`
    deletes every subject/task/session/planned-session/goal and reinserts
    the backup's rows **in one `withTransaction` block** — either the whole
    restore lands or (on any failure) none of it does and the prior data is
    untouched. It deliberately reuses Phase 13's `planImport` +
    a refactored-out `insertParsedData` helper against an **empty baseline**
    ("restoring into an empty database is exactly 'import everything against
    nothing existing'") rather than writing separate insert logic, so
    subject/task de-duplication-by-name and task-linking-by-key are
    identical code paths to Import, just fed different rows. Settings and
    theme are restored *after* the transaction commits, via the existing
    `SettingsRepository`/`PaletteRepository`/`WallpaperRepository` setters
    (a custom wallpaper photo can't be restored — see above — so it always
    falls back to the default background); `ReminderScheduler.reschedule`
    runs last so alarms reflect the restored plans and reminder settings.
  - **Found and fixed a latent bug while building this:** `StudyGoalDao`'s
    `upsert` (`@Insert(REPLACE)` on the unique `(scope, subjectId)` index)
    never actually replaced the weekly goal row, because SQLite's unique
    index treats two `NULL`s (the weekly goal's `subjectId`) as never equal
    — so every "change your weekly goal" from the Goals screen (Phase 2)
    was silently *adding* a new row instead of replacing the old one, and
    `goalForScope`'s `LIMIT 1` read (no `ORDER BY`) meant which goal actually
    took effect depended on SQLite's undefined tie-break order. Fixed by
    having `GoalRepository.setWeeklyGoalMinutes` delete the scope first
    (`StudyGoalDao.deleteScope`, which already existed) before inserting,
    and adding `ORDER BY createdAtEpochMillis DESC, id DESC` to
    `goalForScope` so any pre-existing duplicate rows (from before this fix,
    on a real device) resolve to the most recently set one instead of an
    arbitrary one. This was **pre-existing since Phase 2**, unrelated to
    backup/restore itself, but restore's own `upsert` call for the goal
    would have hit exactly the same bug on a second restore, which is what
    surfaced it.

  **Deliberate limits, disclosed in the UI or here:** no per-item editing of
  a restore before confirming (all-or-nothing, matching "backup" semantics
  rather than "import"); a custom Personalize photo is never backed up;
  subject-scoped goals aren't backed up because they don't exist yet
  (`StudyGoalEntity.subjectId` is still reserved for a future phase, same as
  Phase 2/3 noted); restoring on a device with a *different* build's newer
  backup format version fails cleanly with a message rather than attempting
  a partial restore.

  **Verification status: written, not compiled.** Watch on the next CI run:
  (1) Everything in Phase 13's own watch-list still applies (same
  `org.json:json` real-vs-stub concern, same "strict test path" caveat) —
  `parseBackupFile` shares that exact dependency. (2) `AlertDialog`/
  `TextButton` (material3 — already used elsewhere in the app, e.g.
  Subjects/Tasks delete confirmations per Phase 3/4, so the import should
  resolve, but this is the first use in `screens/data`). (3) `Room`
  `withTransaction` with five sequential `deleteAll()` calls before
  `insertParsedData` runs, inside one block — confirm this doesn't exceed
  any per-statement transaction limit (it shouldn't; it's just more
  statements in the same pattern Phase 13's `applyImport` already uses).
  (4) The `StudyGoalDao` fix changes `goalForScope`'s query text (adds
  `ORDER BY`) and `upsert`'s doc only, not its signature — should not affect
  any existing caller, but re-run `GoalProgressTest` (unaffected, pure math)
  and manually re-check the Goals screen's weekly-goal editor still saves
  correctly. (5) Tests: `StudyDataBackupTest` (11 cases: build/parse round
  trip, weekly-goal-absent round trip, export-file-into-restore and
  backup-file-into-import cross-messages, garbage input, newer-version
  rejection, bad-planned-session rejection with partial success, unknown
  planned-session status, out-of-range weekly goal dropped-not-fatal,
  missing settings/theme sections falling back to defaults, custom-wallpaper
  never round-tripping as built-in) — hand-verified against the
  implementation, never executed. **No DAO/Repository-level test exists for
  `restoreBackup` itself** (this project has no Room/Robolectric test
  infrastructure set up yet — every existing test, per Phase 2's
  `GoalProgressTest` doc comment, is deliberately Android-dependency-free
  JVM-only); `restoreBackup`'s correctness rests on manual code review plus
  the already-tested `planImport`/`insertParsedData` it reuses. **On a
  device, this phase specifically needs:** create a backup, add more
  sessions/subjects/change settings, restore the backup, and confirm
  everything (including the weekly goal, theme, and reminder toggles) matches
  the backup rather than the newer changes; restore into a completely empty
  install; hand-edit a backup file to remove a field from `settings`/`theme`
  and confirm it still restores with defaults; restore twice in a row and
  confirm the weekly goal doesn't duplicate (this is the regression test for
  the `StudyGoalDao` fix above).
- 🔶 Phase 15 — Themes & customization expansion: **written, unverified by
  CI/device.** Every sub-item audited against what already existed before
  writing anything new — most of this phase turned out to already be done:
  - **Wallpaper selection: already fully implemented** (Default / bundled /
    "Personalize" gallery photo via the system Photo Picker) since an
    earlier phase. **Correction to Phase 14's own writeup:** it called
    Personalize "not even implemented yet" while justifying why a backup
    excludes a custom photo — that claim was wrong (checked properly this
    time, before writing anything); the *exclusion itself* was still the
    right call (a private on-device file doesn't belong base64-inflated
    into a portable JSON backup), just for a different reason than stated.
  - **Reduced-motion option: already implemented** ([LocalReduceMotion],
    Settings → Accessibility). **"Animation toggle" is deliberately not a
    second, separate control:** audited every animation API used anywhere
    in the app (`infiniteRepeatable`/`animateFloatAsState`/`AnimatedVisibility`/
    `Crossfade`/`tween`) and found exactly one call site
    (`ui/components/ProgressRing.kt`), already gated by Reduce Motion. A
    second toggle next to it that gated the exact same thing would be the
    kind of fake/no-op control the master prompt's rule 8 rules out, so
    Reduce Motion **is** this phase's animation toggle; documented here
    rather than silently skipped.
  - **New:** 4 additional palettes — **Deep Space**, **Midnight**,
    **Minimal Dark**, **Light** — purely additive (existing 7 palette IDs
    and their exact colors untouched, matching "preserve visual identity
    unless a feature specifically requires a UI change"). Deliberately
    calmer/lower-saturation than the neon Galaxy family (muted blues, near-
    grayscale, or plain neutral-light) for "don't sacrifice readability for
    visual effects" and to double as a natural Phase 16 (Accessibility)
    starting point. New named constants in `Color.kt` follow the existing
    per-palette-family convention exactly.
  - **New: Accent customization.** A curated swatch row on the Themes
    screen (not a free HSV/color-wheel picker) overrides just the
    primary/secondary/primaryContainer roles of whichever palette is
    selected — background/surface/text/outline are untouched, so a custom
    accent can't accidentally break a palette's readability. `onPrimary`/
    `onSecondary`/`onPrimaryContainer` are recomputed via a WCAG
    contrast-safe helper rather than guessed, since a user-picked hue can't
    be pre-verified for contrast the way a hand-picked palette color can.
    That helper (`contrastSafeContentColor`) was **extracted from
    `Buttons.kt`** (where it was `private`, used only for button text) into
    a new shared `ui/theme/ContrastUtils.kt`, so the button and the new
    accent override share one implementation instead of two that could
    drift. Persisted via a new `PaletteRepository.customAccentArgb`
    (`null` = use the palette's own accent).
  - **New: Sound and Vibration toggles**, backed by real functionality (not
    placeholders — rule 8): a new `service/CompletionFeedback.kt` plays a
    short `ToneGenerator` tone and/or a `Vibrator` pulse the instant a
    timer/focus session/Pomodoro phase finishes, wired into the same three
    `maybeNotifyCompletion` call sites (`CountdownTimerViewModel`,
    `FocusModeViewModel`, `PomodoroViewModel`) that already post the
    completion notification. **Deliberately independent** of "Timer
    completion alerts" (the existing notification toggle) rather than a
    sub-option of it: sound/vibration fire even if the user has turned
    system notifications off but still wants the in-app cue, and the
    notification's own channel sound/vibration is controlled by Android
    per-channel anyway (an app can't override that after the channel
    exists) — two genuinely different mechanisms, so two genuinely
    different toggles. Fixed the "Timer completion alerts" row's own
    subtitle while here too: it said "Play a sound", which was actually
    describing the *system notification channel's* incidental default
    sound and would have read as a duplicate of the new "Sound" toggle;
    reworded to "Show a notification" to match what it actually does.
    New manifest permission `VIBRATE` (normal, install-time, no runtime
    prompt), justified with the same comment style the manifest already
    uses for its other permissions.
  - **Phase 14 extended, not re-opened:** a backup now also carries the two
    new settings and the custom accent color
    (`SettingsSnapshot.soundEnabled`/`vibrationEnabled`,
    `ThemeSnapshot.accentArgb`), parsed leniently with the same
    missing-key-defaults-safely rule Phase 14 already established — a
    backup made before Phase 15 existed has none of these keys and still
    restores correctly (sound/vibration default on, accent defaults to
    "use the palette's own", exactly matching what a fresh install would
    have anyway).

  **Verification status: written, not compiled.** Watch on the next CI
  run: (1) `androidx.compose.material3.ColorScheme.copy(...)` — used by
  `effectiveColorScheme` to override just a few roles; this is a real,
  documented public API, not assumed, but it's the first place this
  codebase calls it. (2) `ToneGenerator`/`Vibrator`/`VibratorManager` —
  standard Android APIs, branched on `Build.VERSION.SDK_INT >= S` for the
  non-deprecated `VibratorManager` path (minSdk 26 covers
  `VibrationEffect.createOneShot` unconditionally, no branch needed there).
  Neither is unit-testable without Robolectric (this project has none, by
  established practice — see Phase 14's own watch-list), so
  `CompletionFeedback` and `effectiveColorScheme`/the new palettes have
  **no automated test**, same category as Phase 12's alarm scheduling;
  correctness rests on manual review only. (3) Tests: 2 more cases added to
  `StudyDataBackupTest.kt` (13 total) — accent-color round trip, and a
  simulated pre-Phase-15 backup (no `soundEnabled`/`vibrationEnabled`/
  `accentArgb` keys) still restoring with correct defaults — hand-verified,
  never executed. **On a device, this phase specifically needs:** try all 4
  new palettes for readability at default font size and at a large
  accessibility font size; set a custom accent, confirm button text stays
  legible on every palette (not just the one active when it was set);
  confirm Sound/Vibration actually fire on a real device for all three
  timer modes and independently of the notification toggle; confirm a
  Phase-14-era backup (made before this phase) still restores cleanly.
- ⬜ Phase 16 — Accessibility pass
- ⬜ Phase 17 — Smart study planning
- ⬜ Phase 18 — Optional AI features (architecture only, no AI API unless explicitly requested)
- ⬜ Phase 19 — Performance pass
- ⬜ Phase 20 — Data model review
- ⬜ Phase 21 — Testing pass
- ⬜ Phase 22 — UI/UX QA
- ⬜ Phase 23 — Build & release, final report

## Original Stage 1-10 plan (paused, not abandoned — resume here if the expansion plan is ever paused instead)

## Environment facts (don't relitigate these each session)
- Built by Claude with **no Android SDK, no emulator, and no internet
  access** in its working container. Claude can write correct source files
  and reason carefully about them, but **cannot compile or run** the app.
  Real verification only happens via GitHub Actions once pushed, or on the
  user's own machine/phone.
- User builds exclusively via **GitHub Actions** (no computer, phone-only
  workflow).
- Never claim "build succeeded" / "APK tested" / "tests passed" without
  actual CI evidence.

## Stage checklist (update ✅/⬜ as stages complete)
- ✅ Stage 1 — Project setup (Gradle, manifest, package structure, CI foundation, README)
- ✅ Stage 2 — Core UI (galaxy dashboard, nav, reusable Compose components)
- ✅ Stage 3 — Timer engine (study/pomodoro/focus countdown logic, start/pause/resume/reset)
- ✅ Stage 4 — Background timer (foreground service, notifications, single-active-session tracking)
- ✅ Stage 5 — Study tracking (Room DB, repository, dashboard stats)
- 🔶 Stage 6 — Wallpapers & themes: wallpaper picker (16 bundled images +
  gallery "Personalize" with explicit remove), 6-palette picker, DataStore
  persistence, and app-wide application are all written —
  **unverified by build/device**. Remaining before calling Stage 6 done:
  confirm it actually compiles via GitHub Actions and device-test the Photo
  Picker + palette switching. **Verification deliberately deferred** — the
  user asked to keep building through the remaining stages first and batch
  all device/CI verification at the end, rather than stopping after each
  stage. Noted here, not silently skipped.
- 🔶 Stage 7 — Pomodoro + Focus Mode + Analytics: real weekly bar chart +
  per-mode breakdown cards (Analytics), Pomodoro duration presets per phase
  (Work/Short Break/Long Break, each independently selectable while idle) —
  all written, **unverified by build/device**, same deferred-verification
  note as Stage 6 above. Focus Mode itself needed no new work this stage —
  it's already a real, honest open-ended timer from Stage 3/5 (no
  distraction-blocking is implemented anywhere in this app, by design — see
  "Decisions locked in").
- 🔶 Stage 8 — Settings & animations: real DataStore-backed settings
  (notification alerts, keep-screen-on, reduce-motion, daily goal),
  a Home-dashboard ring easing animation gated by reduce-motion, and a
  small accessibility fix on `FeatureCard` — all written,
  **unverified by build/device**, same deferred-verification note as
  Stages 6-7 above.
- ⬜ Stage 9 — Testing & bug fixing (QA pass, honest report)
- ⬜ Stage 10 — Final build, release docs, packaged ZIP

## Session log
- **Expansion Phase 15 — Themes & customization (this session):** User said
  "continue" — next unchecked phase, Phase 15.

  Audited first: `AppPalette.kt`/`Color.kt` (found 7 existing palettes,
  richer than the master prompt's own suggested list — decided to *add*
  four more rather than rename/replace any, per "preserve visual identity
  unless a feature specifically requires a UI change"); grepped the whole
  app for `reduceMotion`/sound/vibration APIs before assuming any of them
  needed building — found Reduce Motion already fully wired
  (`LocalReduceMotion`, one consumer: `ProgressRing.kt`) and sound/vibration
  completely absent (no `MediaPlayer`/`ToneGenerator`/`Vibrator` anywhere);
  grepped every Compose animation API (`infiniteRepeatable`,
  `animateFloatAsState`, `AnimatedVisibility`, `Crossfade`, `tween`) and
  found exactly one call site, already gated by Reduce Motion — this is
  what settled "Animation toggle" as *already satisfied*, not a gap; read
  `ThemesScreen.kt`/`ThemesViewModel.kt` in full and discovered
  "Personalize" (custom gallery wallpaper) was already completely
  implemented, which is a **correction to my own Phase 14 writeup** from
  last session, where I'd claimed it "isn't even implemented yet" while
  justifying why backups exclude it — the exclusion decision was still
  right (a private photo shouldn't be base64-inflated into a portable JSON
  backup), I'd just gotten the *reason* wrong by not checking closely
  enough at the time; read `TimerNotifications.kt`'s channel setup closely
  enough to understand *why* a notification channel's sound/vibration can't
  be the same lever as an in-app "Sound"/"Vibration" toggle (Android owns
  channel sound/vibration once the channel exists; the app can't override
  it), which is what justified making the new toggles independent rather
  than folding them into "Timer completion alerts".

  Key decisions: **(a)** four new palettes, purely additive, explicitly
  lower-saturation than the neon Galaxy family, doubling as a Phase 16
  (Accessibility) head start. **(b)** Accent customization as a *curated
  swatch set*, not a free color-wheel/HSV picker — a curated set is
  trivially kept legible (each swatch pre-chosen), where validating an
  arbitrary user-picked hue for contrast is a much harder, riskier problem
  to get right without being able to visually test it. **(c)** Extracted
  `contrastSafeContentColor` out of `Buttons.kt` (where it was `private`)
  into a new shared `ui/theme/ContrastUtils.kt` rather than writing a second
  copy for the accent override — same "one source of truth" reasoning as
  Phase 14 reusing `parseJsonExport`. **(d)** Sound/Vibration built as real
  functionality (`ToneGenerator`/`Vibrator` via a new `CompletionFeedback`),
  never toggles with nothing behind them — rule 8 is unambiguous about this,
  and the master prompt explicitly lists both under "Allow:" for this phase,
  so skipping them silently wasn't an option the way "Animation toggle"
  was. **(e)** Extended Phase 14's backup format (new `SettingsSnapshot`/
  `ThemeSnapshot` fields) rather than starting a second backup format
  version, since Phase 14's settings/theme section was already designed to
  degrade gracefully on missing keys — exactly the situation a pre-Phase-15
  backup puts it in.

- **Expansion Phase 14 — Backup & restore (prior session):** User uploaded
  the checkpoint zip with no further instruction — treated as "continue",
  the next unchecked phase.

  Audited first (per the master prompt's explicit Phase 1 rule, re-applied
  every session, not just once): `PROJECT_STATE.md`'s own Phase 13 write-up
  and its explicit note that Phase 14 belongs on the same Data screen; every
  entity and DAO (`StudySessionEntity`/`SubjectEntity`/`TaskEntity`/
  `StudyGoalEntity`/`PlannedSessionEntity` and their DAOs) to know exactly
  what a full backup needs to cover; `DataTransferRepository.kt` and
  `StudyDataImport.kt` in full, since reusing them was the plan from the
  start; `SettingsRepository`/`ReminderSettings`/`PaletteRepository`/
  `WallpaperRepository` for every DataStore-backed value a backup should
  carry; `AppDatabase.kt`'s migrations (confirmed no new migration would be
  needed before writing any DAO code); `DataManagementScreen.kt`/
  `ViewModel.kt` in full, since the new UI extends them in place rather than
  replacing them.

  Key decisions, each with the alternative rejected:
  **(a) Nest a full Phase-13-shaped export inside the backup file and reuse
  `parseJsonExport`** (widened to `internal`) for that section, instead of
  writing a second, parallel subject/task/session parser. Rejected
  duplicating the validation logic — it already exists, is already
  documented as unit-tested, and any divergence between two parsers for the
  "same" data would be a real bug waiting to happen.
  **(b) Restore = wipe + reuse `planImport`/insert against an empty
  baseline**, instead of writing bespoke replace-logic. "Import into nothing
  existing" and "restore" are the same operation once you see it that way —
  every session/subject/task becomes "new" against an empty database, so
  the exact same de-duplication-by-name and task-linking code paths apply
  with zero new logic for that part. The insert body was pulled out of
  `applyImport` into a shared `insertParsedData` so neither path duplicates
  it.
  **(c) Two-step destructive confirmation** (preview's own warning text,
  then a separate `AlertDialog` "yes, replace everything") before
  `confirmRestore` can run, rather than a single button — matches the
  master prompt's "see backup status... cancel restore safely" line by
  making the point of no return unmistakable and late, not by adding any
  actual rollback machinery (there's nothing to roll back: the write is one
  Room transaction, so it's all-or-nothing by construction, and everything
  before the second confirm is pure read/parse that touches no table).
  **(d) A custom Personalize wallpaper photo is excluded from the backup**,
  rather than base64-encoding it into the JSON. Rejected inflating a
  portable, human-sized JSON file with an arbitrary photo's bytes for a
  feature (Personalize) that isn't even implemented yet per the codebase —
  disclosed plainly in the screen's own copy instead of silently dropping
  it.
  **(e) Settings/theme parsed leniently, study data parsed strictly** — a
  deliberate asymmetry, not an inconsistency: a bad toggle degrades to a
  default and the user can just re-set it, but silently accepting malformed
  study history would corrupt exactly the years of data a backup exists to
  protect.

  **Found while auditing, not asked for:** `StudyGoalDao.upsert`'s
  `@Insert(REPLACE)` on the unique `(scope, subjectId)` index doesn't
  actually replace the weekly goal (`subjectId = NULL`), because SQLite
  never considers two `NULL`s equal in a unique index — confirmed by
  reproducing it directly in a throwaway SQLite session before touching any
  Kotlin, rather than trusting the doc comment's claim that REPLACE already
  handled it. This has been silently duplicating rows since Phase 2 every
  time a user changed their weekly goal; fixed at the root
  (`GoalRepository.setWeeklyGoalMinutes` now deletes the scope first) since
  restore's own goal-`upsert` would otherwise hit the identical bug on a
  second restore. See Phase 14's "Expansion plan" entry above for the full
  writeup and the CI watch-list this adds.

- **Expansion Phase 13 — Data export/import (prior session):** User said
  "continue" — next unchecked phase, Phase 13.

  Audited first: `SessionType`/`StudySessionEntity`/`TaskEntity`/`SubjectEntity`
  (what an export can contain), the three DAOs (insert signatures; no
  one-shot reads, so the repository uses `.first()` on the existing flows),
  `build.gradle.kts` (room-ktx present → `withTransaction`; **no JSON
  library** — only Android's built-in `org.json`), `SessionRepository`
  (`MIN_RECORDABLE_MILLIS`, reused as the import minimum so live recording
  and import agree), and the existing `PrimaryButton`/`SecondaryButton`/
  `GlassCard` components (reused for the new screen).

  Key decisions, each with the alternative rejected:
  **(a) Additive-only import** instead of offering replace/merge modes —
  makes "never overwrite existing data" a property of the code rather than
  of a dialog; restore-everything is Phase 14's distinct, explicitly
  destructive operation.
  **(b) Parse → plan → confirm → apply as separate steps**, with parse and
  plan *pure* and unit-tested; the screen shows the plan before any write,
  and every rejected row is listed rather than silently skipped (the phase
  spec: validate before saving; project rule: don't hide errors).
  **(c) Second-precision duplicate key.** First instinct was exact
  start-millis; tracing a CSV round trip on paper showed it would make
  re-importing your own CSV import everything twice (CSV times are only
  second-precise). Key changed to seconds, with a test that has
  millisecond noise on both sides.
  **(d) `org.json` (built in on Android) + a test-only dependency**, over
  adding kotlinx.serialization (new Gradle plugin, more CI risk while
  nothing has been compiled) or hand-writing a JSON parser (uncompilable
  code I couldn't test). Cost: the real library is stricter than Android's;
  noted above.
  **(e) Storage Access Framework, not file-system paths** — no permission,
  works with Drive/Downloads/USB, and the app can only touch the one file
  the user chose.
  **(f) Formula-injection guard in CSV** — a subject named `=HYPERLINK(...)`
  would otherwise execute when the exported file is opened in a
  spreadsheet; guard + exact inverse on import, both tested, including a
  round trip of a real-looking payload.

  Process note: my first bracket-balance check flagged 3 of the new files;
  they were false positives from a naive string stripper that can't see
  quotes nested inside `${...}` templates. Rather than assume that, I wrote
  a template-aware tokenizer, found *it* had a bug (flagged the old,
  previously-compiled files too — the tell), fixed it, and re-ran: all 124
  `.kt` files balanced. That checker lives only in the session's /tmp, not
  in the repo.

  **Environment:** still no Kotlin compiler/SDK/network. `.github/` again
  absent from the uploaded zip and again not recreated (see Phase 11).
- **Expansion Phase 12 — Notifications & reminders:** User
  said "continue" — proceed to the next unchecked phase, Phase 12.

  Audited first: `TimerNotifications` (existing channels, the
  `notifyCompletion` pattern this phase mirrors: check-then-post, swallow
  `SecurityException`), `MainActivity` (where the permission is currently
  requested — at first timer start), `SettingsRepository`/`SettingsScreen`
  (a "Notifications" section with one toggle already existed), the four
  `recordSession` call sites (all funnel through `SessionRepository`), and
  the planner data layer.

  Two design decisions carried most of the weight:
  **(a) Alarm model.** Weighed one alarm per plan vs one alarm for the
  *next* reminder. Chose the latter: unbounded per-plan alarms would need
  every plan edit to find and cancel its own, whereas "re-derive the single
  next alarm from the database" is trivially consistent and its core is a
  pure function. That function, not the Android glue, is what's tested.
  **(b) Inexact vs exact alarms.** Verified against Android's own scheduling
  docs (web search this session, not memory): inexact alarms never fire
  early but can be up to an hour late on 12+ under battery saving; exact
  ones need a special user grant. Chose inexact + honesty in the UI +
  receiver logic that copes with lateness, over asking users for a special
  access grant.

  Bug caught while designing, before it was written: if the receiver only
  handled the single "next" reminder, two plans starting at the same minute
  would lose the second one (its trigger is already past by the time the
  alarm re-aims). Fixed by having the alarm carry its scheduled instant and
  `remindersDueAt` return *every* reminder sharing that instant; there are
  tests for exactly that (two plans together; a plan and the daily reminder
  together).

  Wellbeing/anti-pressure calls, per the spec: all new toggles default off;
  daily reminder presets stop at 9 PM; daily reminder is skipped if you've
  already studied; goal notices only on the crossing (no progress nudges);
  copy is neutral ("Open StudySpace whenever you're ready to study.").

  `SessionRepository` gained an optional `onSessionRecorded` callback
  (default null, so `SubjectStatsTest`'s `SessionRepository(FakeStudySessionDao())`
  is untouched) — chosen over having ViewModels or the repository know about
  notifications, and over touching all four `recordSession` call sites.

  **Environment:** still no Kotlin compiler/SDK/network for building.
  Checked manually: brace/paren balance over all 115 `.kt` files (all
  balanced); every project-internal import resolves; no duplicate imports;
  Kotlin is 1.9.24 (so `List<Int>.max()` in a test is the non-null form);
  test expectations recomputed by hand. `.github/` was again absent from the
  uploaded zip and again not recreated (see the Phase 11 entry).
- **Expansion Phase 11 — Daily summary:** The user uploaded
  the checkpoint zip plus the Master Feature Expansion Prompt again with no
  further text — read the same way as the earlier bare "continue"/"start":
  proceed to the next unchecked phase, i.e. Phase 11.

  Audited before writing: `PROJECT_STATE.md`, `AppDatabase`, `TaskEntity`/
  `TaskRepository`, `SessionRepository`, `AdvancedAnalytics`, `GoalProgress`,
  Home/Achievements (as the closest existing screen/ViewModel templates).
  The audit found the phase's one real blocker immediately: nothing in the
  data model records *when* a task was completed, so the spec's "Tasks: 8
  completed" line was impossible to produce honestly from what existed.
  Weighed three options — (a) omit the tasks line (drops a spec field),
  (b) count all `completed` tasks regardless of day (a wrong number
  presented as a daily one), (c) add a completion timestamp. Chose (c), the
  smallest possible migration (one nullable column, no rebuild), and chose
  *not* to back-fill existing completed tasks with a guessed date.

  Everything else is composition over existing pieces: `GoalProgress` for
  the goal math, `computeSubjectTotals` for per-subject time, the existing
  `sessionsSince`/`allTasks`/`dailyGoalMinutes` flows. Top subject ranks by
  *time* not session count (Phase 8's reasoning), never returns the
  unattributed bucket, and tie-breaks by subject id so the result can't
  depend on DAO row order — each of those has a test that would fail on the
  naive version.

  Not done, deliberately: no summary notification (Phase 12 owns
  notifications and their per-type opt-outs); no planned-vs-actual line in
  the summary (Phase 7's Analytics section already owns that comparison and
  the spec's Phase 11 field list doesn't include it).

  **Environment:** still no Kotlin compiler/Android SDK in the container
  (`kotlinc` absent, no network), so nothing here has been compiled. The
  zip also arrived **without `.github/`** (the CI workflow) — a zip tool
  dropping dotfolders again, as documented in the Stage 6 entry. It was
  **not** recreated this time, on purpose: the user's real repo already
  has a workflow that has run, and re-adding a reconstructed copy risks
  overwriting a working one. If the repo ever loses it, the Stage 6 entry
  above describes exactly what it contained.
  Checked manually instead: brace/paren balance over all 107 `.kt` files
  (all balanced); every `com.studyspace.timer.*` import in the tree resolves
  to a declared symbol (the only "misses" were the generated `R` class and
  one extension function my regex couldn't see, both confirmed fine);
  no duplicate top-level function names in the changed files; the single
  `HomeScreen(` call site passes the new `onOpenDailySummary`. Date/percent/
  timezone expectations in the new tests were recomputed independently in
  Python (weekday of the fixed test dates, the 74% float rounding, the
  Kolkata-vs-UTC day boundary).
- **Expansion Phase 10 — Daily dashboard (this session):** User said
  "continue" — same bare-continue pattern as the last two handoffs, read
  the same way.

  The real tension this phase had to resolve, more than any prior one:
  the spec's own "keep it clean, not overcrowded" instruction for this
  exact phase, arriving right after Phase 9 had just pushed Home to 10
  quick-action tiles. Reconciled it by treating "clean" as being about
  *hierarchy*, not *inventory* — the master prompt's own Rule 2 ("do not
  remove existing working features") settles the question of whether the
  10 tiles could just be cut down; they couldn't, not without breaking
  working navigation for Study Goals/Subjects/Tasks/Planner/Achievements/
  every timer mode. So the grid stayed complete but got demoted: renamed
  from "Quick Actions" to "All Features," pushed below four new,
  higher-priority sections, with a *new*, narrower "Quick Start" (3
  buttons, not 10 tiles) taking over the "fast, obvious, top-of-screen"
  role the old grid used to play alone. Home is measurably longer now
  (flagged explicitly in the Phase 10 checklist entry above as something
  worth an actual scroll-through on the next CI run, not just a glance),
  but what's *at the top* — Today, streak, Quick Start — is exactly the 3
  things spec calls the most time-sensitive, everything else is one
  labeled tap or scroll away rather than gone.

  Second decision worth recording: Subject Breakdown's empty-subject
  handling deliberately differs from Analytics' Phase 8 "By Subject"
  section — Analytics shows an explicit "No subject" row (a full,
  unfiltered accounting is that screen's whole job), but the Home version
  silently drops unattributed time, on the reasoning that a fast dashboard
  glance showing "No subject: 42m" reads as more confusing than
  informative in that smaller context, while the complete picture is still
  one tap away via "See all." Wrote this reasoning into
  `SubjectBreakdownSection`'s own doc comment specifically so it reads as
  a deliberate inconsistency with Phase 8's choice, not an accidental one.

  Confirmed before writing any navigation code that "Start Stopwatch" and
  "Start Pomodoro" needed zero new plumbing (Timer already defaults to the
  Self-Study tab; Pomodoro already has its own screen and callback) — only
  "Start Custom Timer" (landing specifically on the Normal/countdown tab)
  needed something new, which kept this phase's actual navigation-layer
  footprint to one small, contained addition rather than three.

  **Verification status: written, not compiled** — same caveat as every
  phase. Checked manually: brace/paren balance across the full
  `com/studyspace/timer` tree including every new/changed file this
  session (all balanced, 102 files); every new composable in
  `HomeScreen.kt` appears exactly once (`grep` for duplicate function
  names, the same check Phase 8's session used after several `str_replace`
  edits to one already-large file); confirmed `HomeScreen`'s new
  `onOpenTimerTab` parameter is wired at its one call site in
  `StudySpaceNavHost.kt` (a parameter added to a function signature but
  never wired at the call site is a real Kotlin compile error, not a
  silent bug, but worth confirming by hand rather than assuming the edit
  landed). See the Phase 10 checklist entry above for what to specifically
  watch on the next CI run.
- **Expansion Phase 9 — Streaks & achievements (this session):** User said
  "continue" again — same bare-continue pattern as Phase 7's handoff,
  read the same way: proceed to the next unchecked phase.

  The one real design decision this phase made, made deliberately rather
  than by default: whether achievements need a persisted "unlocked" table
  (the obvious first instinct for a gamification feature — most apps store
  unlock state) or can be recomputed live from existing history every time
  the screen opens. Worked through it by checking which achievements are
  actually monotonic: session counts and total hours only ever increase,
  and — the one piece that took real thought — a 7-day or 30-day streak
  badge must stay earned even after the streak later breaks, which meant
  the achievement needs the *best-ever* streak (scanning all of history for
  the longest run anywhere in it), not the *current* live streak
  (`SessionRepository.computeStreak`, which intentionally drops to 0 the
  day a streak breaks, for the Home screen's "current streak" display).
  Wrote `computeBestStreak` as a deliberately separate function from that
  existing one rather than trying to reuse or parameterize it, and wrote a
  test specifically for the case that would have broken a live-streak-based
  version (`a broken streak does not erase a past longer run`). Once that
  was sorted, every achievement except Goal Completed turned out to be
  monotonic, which meant no persisted table was actually needed — recomputing
  live gives the same stable answer with zero migration risk. Goal
  Completed is flagged as the one real exception in both the code and the
  Phase 9 checklist entry above, not glossed over.

  Re-read the spec's Phase 9 section specifically for its two safety
  instructions ("do not encourage unhealthy over-studying or sleep
  deprivation," "avoid rewards that pressure users to study continuously")
  before writing `AchievementsScreen.kt`, and kept every achievement's
  presentation to a plain stated fact — no streak-anxiety framing, no
  urgency, nothing that would read as encouraging compulsive daily
  check-ins. This is a case where the constitution's own general cautions
  about avoiding reinforcement of unhealthy behavior patterns lined up
  directly with an explicit instruction already in the master prompt for
  this exact phase, so there were two independent reasons pointing the
  same direction rather than one overriding the other.

  Reused every existing repository/flow this phase needed
  (`SessionRepository.allSessions`, `TaskRepository.allTasks`,
  `SettingsRepository.dailyGoalMinutes`, `GoalRepository.weeklyGoal`)
  instead of adding a single new Room query — the third phase in a row
  (7, 8, 9) with zero migrations, since Phase 6 already put the one
  genuinely new table this stretch of phases needed in place.

  **Verification status: written, not compiled** — same caveat as every
  phase. Checked manually: brace/paren balance across the full
  `com/studyspace/timer` tree including every new/changed file this
  session (all balanced, 102 files); caught and fixed one real mistake
  while writing `AchievementsScreen.kt` — an errant
  `import androidx.compose.ui.fillMaxWidth` (that modifier lives in
  `androidx.compose.foundation.layout`, not `androidx.compose.ui`) that
  would have failed to resolve at compile time; removed before it shipped.
  Traced `AchievementsTest.kt`'s two percentage-math cases
  (`computeWeeklyConsistency`'s 4/7 and 1/7 cases) by hand against integer
  division to confirm the expected values (57 and 14) match what the
  function actually computes, not just what looked plausible. See the
  Phase 9 checklist entry above for what to specifically watch on the next
  CI run.
- **Expansion Phase 8 — Advanced analytics (this session):** User said
  "start" — bare, no phase number, arriving right after the Phase 7
  handoff message offered "Phase 8 whenever you want to keep going." Read
  as "start [Phase 8]," continuing the same established sequence.

  Biggest single-phase UI addition since Phase 5: one screen (Analytics)
  gained a range selector and three full new sections. Deliberately kept
  the *data* layer boring and consistent with every prior analytics
  phase — one pure aggregation file, four pure functions, one new
  unbounded DAO query — so the actual new surface area this phase adds is
  almost entirely in `AnalyticsScreen.kt`'s presentation, not in new
  patterns the rest of the codebase has to learn.

  The one real design call: whether the new range selector should also
  reach backward and control the existing "This Week" chart and "Planned
  vs Actual" sections from Phases 7 and earlier, or stay scoped to just
  the three new Phase 8 sections. Chose the latter, explicitly, and said
  so in both the screen's own KDoc and the Phase 8 checklist entry above —
  retrofitting a shared selector onto two already-shipped sections with
  their own already-documented fixed windows felt like exactly the kind of
  quiet behavior change (of *existing* screens) this project's own rules
  caution against ("modify the minimum amount of existing code necessary"),
  for a phase whose brief only asked for new content, not for
  re-plumbing what's already there.

  Getting the "most productive day/hour" honest took a specific decision:
  early draft ranked by session *count* per bucket, which would let five
  disconnected 5-minute check-ins beat one real 3-hour session — reranked
  to total *duration* instead, and wrote a test
  (`most productive day is ranked by total duration, not session count`)
  that would have caught the count-based version, specifically because
  this is the kind of subtly-wrong-but-plausible-looking bug that's easy
  to ship without ever noticing, exactly what Phase 21 (Testing) as a
  whole exists to catch — pulling one instance of it forward into this
  phase's own test file rather than waiting.

  **Verification status: written, not compiled** — same caveat as every
  phase. Checked manually: brace/paren balance across the full
  `com/studyspace/timer` tree including every new/changed file this
  session (all balanced, 98 files); no duplicate imports in the heavily-
  edited `AnalyticsScreen.kt` (grepped and diffed the import block by
  hand after four separate edits to it); every new composable function
  name appears exactly once (`grep -n "^private fun\|^fun "` across the
  file to rule out an accidental duplicate paste, a mistake it's
  specifically easy to make when appending several new composables to an
  already-500-line file across multiple `str_replace` calls in one
  session). See the Phase 8 checklist entry above for what to specifically
  watch on the next CI run — most notably `allSessions()`'s lack of any
  bound at all, flagged for Phase 19 rather than fixed pre-emptively here.
- **Expansion Phase 7 — Planned vs actual analytics (this session):**
  User said "continue" (bare, no phase number this time) — read as
  "continue the established phase sequence," i.e. Phase 7, the next
  unchecked item, consistent with how "continue straight to Phase 6" was
  handled last session.

  Smallest phase so far by file count: one new pure-Kotlin file
  (`PlannedVsActual.kt`), two one-line DAO/repository additions, a
  `combine()` in the ViewModel, and a new section in an existing screen —
  no new entity, no migration. Deliberately reused the exact aggregation
  shape `SessionRepository.computeSubjectStats`/`weeklyAnalytics` already
  established (fetch a wider window with one query per side, bucket/sum in
  plain Kotlin, keep the actual math in a free function covered by a fast
  JUnit test) rather than inventing a new pattern for this one phase —
  Phase 6 flagged this as a Phase 7 job already (`PlannedSessionEntity`'s
  doc mentions planned-vs-actual is a later phase's territory), so the
  interfaces on both sides were already shaped to make this a composition
  exercise rather than new design.

  The one thing this phase's `combine()` does that's new for this
  codebase: it's the first `combine()` across two *different*
  repositories' flows (`PlannedSessionRepository` + `SessionRepository`)
  rather than two flows off the same DAO — flagged explicitly in the Phase
  7 checklist entry above as the one piece of this phase without a close
  precedent elsewhere in the app to lean on, worth extra attention on the
  next CI run.

  Chose *not* to clamp completion percent at 100% after re-reading the
  spec's own Phase 8 instruction ("do not make unsupported claims... present
  statistics as descriptive data") — clamping would have been exactly that
  kind of quiet, misleading massaging of the real number.

  **Verification status: written, not compiled** — same caveat as every
  phase. Checked manually: brace/paren balance across the full
  `com/studyspace/timer` tree including every new/changed file this
  session (all balanced); that `computePlannedVsActual`'s bucket
  boundaries in the implementation match what `PlannedVsActualTest.kt`
  actually asserts (traced each of the 6 test cases by hand against the
  function body rather than just trusting both were written consistently);
  that `AnalyticsViewModel`'s two `sessionsSince(...)` calls use the same
  `LocalDate.now().minusDays(29).toEpochDay()` window on both sides of the
  `combine()` (an off-by-one between the two would silently misalign
  planned vs. actual without either side erroring). See the Phase 7
  checklist entry above for what to specifically watch on the next CI run.
- **Expansion Phase 6 — Study planner (this session):** Continued straight
  from Phase 5 (user said "continue straight to Phase 6" — explicit this
  time, not an inferred "start"). Re-read `PlannedSessionStatus` (already
  committed since early in this expansion plan, anticipating exactly this
  entity) and Phase 5's `SessionAttributionPicker`/`filteredForSubject`
  before writing anything, since the planner's subject/task picker reuses
  that same helper rather than a third copy of the pattern.

  Built the full CRUD path (entity → DAO → migration → repository →
  ViewModel → screen → editor dialog) in one pass, same shape every
  entity phase (Goals/Subjects/Tasks) has followed. The one real design
  decision this phase made and flagged explicitly: whether "start directly
  from the planner" should fully close the loop (auto-link the resulting
  session back to the plan, auto-complete it) or just get the user to the
  right timer screen with the right subject/task already selected. Chose
  the latter — see `PlannedSessionEntity`'s class doc and the Phase 6
  checklist entry above for the full reasoning — because auto-linking needs
  a real FK column plus touching every timer ViewModel's save path again
  (the same scope Phase 5 already spent on subject/task attribution), and
  a plan the user marks done themselves is still honest, working
  functionality, not a placeholder.

  Added `Screen.TimerFromPlan` as a **second** route rather than adding
  optional query args to the existing `Screen.Timer` — deliberately the
  lower-risk choice: `Timer`'s route is read directly by
  `StudySpaceBottomNav`'s `currentRoute == entry.screen.route` selection
  check and by the plain `navController.navigate(screen.route)` call bottom
  nav taps already make, and touching either to support two different call
  shapes (bare tab tap vs. prefilled deep-link) risked a subtle
  route-matching regression on the *existing* five-tab bottom nav — the
  single most-used piece of navigation in the app — for the sake of one new
  entry point. A second route with two `NavType.LongType` path args (`-1L`
  sentinel for "none") mirrors `SubjectDetail`'s already-working shape
  instead. Known minor cosmetic side effect, not a bug: landing on
  `TimerFromPlan` shows no bottom-nav tab highlighted (same as landing on
  `SubjectDetail` shows no tab highlighted today) since it isn't in
  `bottomNavEntries`.

  Home's quick-actions grid is now 9 tiles (odd count, 2 columns → last row
  has one tile) — cosmetically fine (`QuickActionsGrid`'s height is already
  computed from row count, and `LazyVerticalGrid` just leaves the second
  cell of an incomplete row empty), but worth the same note Phase 4's log
  left about 8 tiles: Phase 10 ("Daily Dashboard") explicitly redesigns
  Home and splitting "quick start a timer" from "manage your
  subjects/tasks/goals/plans" into separate sections would probably read
  better than one more tile on a single flat grid, now more than ever at 9.

  **Verification status: written, not compiled** — same caveat as every
  phase (no Android SDK/compiler here). Checked manually: brace/paren
  balance across the full `com/studyspace/timer` tree including every
  new/changed file this session (all balanced), that
  `PlannedSessionRepository.START_TIME_PRESETS`/`DURATION_PRESETS`/
  `DATE_OFFSET_PRESETS` (read by both the editor dialog and — indirectly,
  via the same preset values — nothing else) match the types
  `PlannedSessionEditorDialog` destructures them as, and that every new
  file's imports match the APIs it calls, including re-confirming *not* to
  add `import androidx.compose.foundation.layout.weight` anywhere new (the
  exact bug a past session already hit and documented above) — none of the
  new files import it, and every `Modifier.weight(...)` call site added
  this session sits inside a `Row`/`RowScope`. See the Phase 6 checklist
  entry above for the specific things to watch on the next CI run.
- **Expansion Phase 5 — Advanced timer system (this session):** Continued
  straight from Phase 4 (user said "start" again, same meaning as last
  time: keep going with the plan). Started by re-reading every existing
  timer ViewModel (`Stopwatch`, `Countdown`, `Pomodoro`, `FocusMode`) and
  their screens in full before changing anything, per the project's own
  audit-first rule — this phase touches four ViewModels and three screens
  at once, more files in one phase than any prior one, so getting the
  starting point right mattered more than usual.

  Prioritized **subject/task selection** as this phase's centerpiece over
  everything else on Phase 5's list, and said so explicitly rather than
  trying to do all of it: `recordSession()` already had optional
  `subjectId`/`taskId` params sitting unused since Phases 3-4, and every
  Subject/Task stat screen already built would stay at zero forever
  without something on the timer side actually passing real values. Built
  one shared `SessionAttributionPicker` component (custom label + subject
  chips + task chips, task list pre-filtered to the chosen subject) and
  wired it into all five timer entry points (Self-Study, Online Study,
  Normal Timer, Pomodoro, Focus Mode) rather than five bespoke pickers.
  Selection is guarded to "while idle" (or `FocusStage.SETUP` for Focus
  Mode, which has no bare idle concept) the same way every other
  idle-only setting in this app already works (e.g. duration presets),
  and clears back to none once a session is saved so the next one starts
  blank rather than silently reusing the last attribution. Selecting a
  task also selects that task's subject; changing the subject away from a
  selected task's subject clears the task, so a task can never end up
  paired with a mismatched subject in the saved data.

  Pomodoro needed slightly different handling than the other four:
  attribution is chosen once before the first Work phase and persists
  across every Break/Work transition in that cycle (asking again before
  every single phase would be tedious for what's really one continuous
  study block), and a custom label only renames the *saved* Work
  session's label — the live notification/ring still always shows which
  phase is running, which matters more there than a nickname would.

  Also made "sessions before a long break" configurable (2-6, preset
  chips), replacing the old hardcoded `SESSIONS_PER_LONG_BREAK = 4`
  constant (kept as `DEFAULT_SESSIONS_PER_LONG_BREAK`) — a small, easy
  addition once already inside `PomodoroViewModel` for the bigger change.

  Deliberately did **not** attempt in this same phase, and said so rather
  than quietly dropping them: auto-start-next-phase/break toggles (every
  mode still needs an explicit tap to advance — an intentional, already-
  documented design choice, but the master prompt does ask for a toggle
  to override it); separate sound/vibration settings distinct from the
  existing single "Timer completion alerts" toggle; persisting Pomodoro's
  duration/session-count choices across an app restart (still in-memory
  only); and per-notification-type controls, which Phase 12 of this same
  plan covers on its own. Five ViewModels/screens changed at once was
  already the largest single-phase diff so far — adding DataStore
  persistence or a full notification-settings redesign on top in the same
  pass would have been exactly the "don't implement everything in one
  giant change" the master prompt itself warns against.

  **Verification status: written, not compiled** — same caveat as every
  phase (no Android SDK/compiler here), but this phase carries a
  different *kind* of risk than Phases 2-4: no schema/migration changes
  at all, so a CI failure here would be a pure Kotlin/Compose compile
  error (a bad import, a mismatched lambda type, a missing `by
  collectAsState()`), not a data-loss risk. Reviewed every changed file by
  reading it back in full after editing (not just trusting the edit
  calls succeeded) specifically because four parallel ViewModel rewrites
  is exactly the kind of change where a copy-paste-across-four-files
  mistake is easy to make and easy to miss. On the next CI run, pay
  particular attention to: (1) all four timer screens still building and
  their existing (pre-Phase-5) behavior — start/pause/resume/reset button
  states, Focus Mode's lock — unchanged, (2) a session started with a
  subject/task actually shows up correctly in that Subject's/Task's data
  once Subject Detail (Phase 3) is opened, (3) Pomodoro's per-cycle
  attribution persisting correctly across a Work → Break → Work
  transition rather than resetting mid-cycle.
- **Expansion Phase 4 — Tasks & topics (earlier session):** Continued straight
  from Phase 3 in the same conversation (user said "start" with no further
  direction, read as "keep going with the plan"). Re-read `TaskPriority`
  (already committed, anticipating exactly this) before writing anything.

  Added `TaskEntity`/`TaskDao` (new `tasks` table, optional `subjectId`
  FK, `SET_NULL` on the subject's deletion — deleting a subject never
  deletes a task) and gave `StudySessionEntity` a second optional FK,
  `taskId`. `MIGRATION_3_4` rebuilds `study_sessions` again (SQLite still
  can't `ALTER TABLE ADD COLUMN` an enforced foreign key) — this time
  carrying over existing `subjectId` values instead of resetting them to
  NULL, since by this version real rows may already have one (Phase 2/3's
  rebuild reset it to NULL was correct there only because no row could
  have had a subject yet).

  `TaskRepository` (CRUD + mark complete), `computeTaskDueInfo` (pure
  overdue/due-today/due-in-N-days calculation from two epoch-day longs,
  unit-tested in `TaskDueInfoTest.kt`), a Tasks list screen and a shared
  `TaskEditorDialog` (title, optional chapter/topic text fields, an
  optional subject picked from a chip row, priority chips, and preset
  chips for deadline and estimated duration rather than a calendar widget).
  Reached from a new "Tasks" Home quick-action tile.

  Two scope calls, both worth flagging again to the user next time this
  comes up: the "Subject → Chapter → Topic → Task" hierarchy the spec
  illustrates became two free-text fields on `TaskEntity` rather than two
  more full CRUD entities (the spec's own "Task features" list only asks
  for chapter/topic as optional attributes, not as separately managed
  things); and deadline/duration use fixed preset chips rather than
  Material3's `DatePicker`, which this project has never used and which is
  still marked experimental at the pinned material3 version — a wrong
  guess about an unfamiliar experimental API's exact usage is a worse risk
  than a slightly less flexible preset picker.

  Deliberately did **not** add "start timer from this task" even though
  the spec's Phase 4 section asks for it, and did **not** add a "tasks for
  this subject" section to Subject Detail (low-effort, but skipped to keep
  this phase's footprint contained; `TaskDao.tasksForSubject` already
  exists for it whenever it's wanted). See the Expansion plan checklist
  above for the reasoning on the timer piece specifically.

  Home's quick-actions grid is now 8 tiles (Self-Study, Online Study,
  Normal Timer, Pomodoro, Focus Mode, Study Goals, Subjects, Tasks) — still
  functionally fine (the grid's height is computed from tile count, not
  hardcoded, since the Phase 2 fix), but this is worth watching: Phase 10
  ("Daily Dashboard") in the expansion plan explicitly redesigns Home, and
  splitting "quick start a timer" from "manage your subjects/tasks/goals"
  into separate sections at that point would probably read better than one
  more tile added onto a single flat grid.

  **Build error I introduced and caught mid-session:** an early
  `str_replace` meant to insert `MIGRATION_3_4` after `MIGRATION_2_3`
  matched more text than intended and deleted `MIGRATION_2_3` entirely.
  Caught by viewing the file immediately after the edit (this project's
  own "view before/after every edit" habit) rather than trusting the tool
  call succeeded as intended, and fixed by rewriting the whole file with
  all three migrations before this checkpoint was zipped — so the version
  in this checkpoint is correct, but it's a reminder that a
  `MIGRATION_N_N+1` addition needs a real look at the diff, not just a
  "the call didn't error" check.

  **Verification status: written, not compiled** — same caveat as every
  phase (no Android SDK/compiler here). On the next CI run, pay particular
  attention to: (1) `MIGRATION_3_4` — specifically that `subjectId` values
  really do survive the second `study_sessions` rebuild (this is the exact
  kind of mistake the migration edit above nearly shipped), (2) the app
  opening correctly whether the on-device database is at v1, v2, v3, or a
  fresh install straight to v4, (3) nothing from Phases 2-3 regressed.
- **Expansion Phase 3 — Subjects (earlier session):** Continued straight from
  Phase 2 in the same conversation. Read the existing Phase 2 work and the
  `GoalScope`/`PlannedSessionStatus`/`TaskPriority` enums again before
  writing anything, to keep this consistent with what they already
  anticipated.

  Added `SubjectEntity`/`SubjectDao` (new `subjects` table) and gave
  `StudySessionEntity` a nullable `subjectId` foreign key. SQLite can't add
  an enforced `FOREIGN KEY` to an existing table via a plain
  `ALTER TABLE ADD COLUMN`, so `MIGRATION_2_3` rebuilds `study_sessions`
  properly: create a new table with the full desired schema, copy every
  existing row across with `subjectId = NULL`, drop the old table, rename
  the new one. This migration runs in the *same* Room database as Phase
  2's `study_goals` table (added by `MIGRATION_1_2`) — it only touches
  `study_sessions`, so `study_goals` and anything a user saved into it
  between the two sessions is untouched.

  `SubjectRepository` (create/rename/delete + fixed emoji/color presets,
  no full color picker), `SubjectStats` (pure aggregation — total/today/
  week/month/session count/average/streak per subject — unit-tested in
  `SubjectStatsTest.kt` the same way `GoalProgress` was in Phase 2), a
  Subjects list screen (tap to open detail, "+ Add" via a shared
  `SubjectEditorDialog`), and a Subject Detail screen (colored header card,
  a stats grid, rename/delete through the same dialog). Reached from a new
  "Subjects" Home quick-action tile, same pattern Phase 2 used for "Study
  Goals". Extracted `HomeScreen`'s private `StatMiniCard` into a shared
  `ui/components/StatMiniCard.kt` so Subject Detail's stats grid reuses it
  rather than duplicating those three lines of Compose a second time.

  Scope decisions: deleting a subject unlinks its sessions (`SET_NULL`)
  rather than deleting session history — matches the project's rule that a
  management action must never destroy study data. `recordSession()` grew
  an optional `subjectId` parameter defaulting to `null`, so it's ready for
  timer screens to pass a real value once they gain subject selection, but
  every existing call site keeps compiling with no changes needed right
  now. Deliberately did **not** touch any timer screen to add subject
  selection, and did **not** wire up `GoalScope.SUBJECT` goals on Subject
  Detail — the master prompt calls out "timer integration" as its own
  later step, and a subject goal needs a subject to exist first, which is
  exactly what this phase adds, not what it consumes.

  **Verification status: written, not compiled** — same caveat as every
  phase so far (no Android SDK/compiler in this environment). Manually
  traced every new call site (Room annotations/foreign key syntax,
  StateFlow/combine usage, the new `ViewModelProvider.Factory` for
  `SubjectDetailViewModel`'s runtime `subjectId` arg, Compose imports) but
  this is not a substitute for a real Gradle build. On the next CI run,
  pay particular attention to: (1) `MIGRATION_2_3`'s rebuilt
  `study_sessions` table matching Room's expected schema exactly (column
  order, the foreign key, the index name) — a full-table rebuild migration
  is the riskiest kind to get subtly wrong, (2) the app still opens
  correctly on a device that already has a v2 database (Phase 2's
  build) and correctly on a fresh install (straight to v3), (3) nothing
  Phase 2 built (Goals screen, weekly goal) regressed.
- **Expansion Phase 2 — Study Goals (earlier session):** User uploaded a new
  "Master Feature Expansion Prompt" (23 phases) and chose to pause the
  original Stage 9-10 plan to start it, beginning with Phase 2. Read the
  full existing project first (Phase 1's audit) rather than assuming; found
  `GoalScope`, `PlannedSessionStatus`, `TaskPriority` enums already
  committed (with KDoc explicitly anticipating `StudyGoalEntity`/
  `PlannedSessionEntity`/`TaskEntity`) but no corresponding tables — this
  phase adds the first of them.

  Added: `data/db/StudyGoalEntity.kt` (new `study_goals` table, unique
  index on `(scope, subjectId)`, `subjectId` reserved/unused until Phase 3
  Subjects exist), `data/db/StudyGoalDao.kt`, a hand-written
  `MIGRATION_1_2` in `AppDatabase.kt` (version 1→2 — **not** a destructive
  fallback, so existing `study_sessions` rows survive), `data/repository/
  GoalRepository.kt`, `data/repository/GoalProgress.kt` (pure percent/
  remaining/complete math, no Android dependency, unit-tested in
  `GoalProgressTest.kt` the same way `FocusDuration` is), `ui/components/
  GoalProgressCard.kt` (the "Xh Ym / target, bar, %, remaining" layout the
  spec asked for), and `screens/goals/{GoalsScreen,GoalsViewModel}.kt`.
  Wired into nav as `Screen.Goals` (not a bottom-nav tab — reached from a
  new Home quick-action tile, same pattern as Pomodoro/Focus) via
  `StudySpaceNavHost.kt` and `HomeScreen.kt`.

  Scope decisions: the existing Settings daily goal is reused as-is for
  "Today" rather than duplicated into the new table (matches `GoalScope`'s
  own KDoc, which deliberately has no `DAILY` member for this reason). The
  weekly goal compares against `StudyStats.weekTotalMillis`, which is a
  rolling 7-day window, not a Monday-Sunday calendar week — kept consistent
  with what Home/Analytics already call "this week" rather than
  introducing a second definition. Subject goals are explicitly **not**
  built yet (no Subject entity exists) — the Goals screen shows an honest
  "coming with Subjects" card instead of a disabled/fake control, per the
  project's rule against placeholder buttons that do nothing.

  Incidental fix while editing `HomeScreen.kt`'s quick-actions grid to add
  the new tile: its `LazyVerticalGrid` had a hardcoded `height(300.dp)`
  sized for 2 rows/4 tiles; with 5 tiles (before this session) that already
  silently clipped the 3rd row (Focus Mode) — a pre-existing bug, not
  something this session introduced. Now computed from the actual tile
  count/row count instead of a constant.

  **Verification status: written, not compiled.** No Android SDK/compiler
  here (see "Environment facts" above) — checked manually by tracing every
  new call site against the classes it calls into (Room annotations,
  StateFlow/combine usage, Compose imports), same as every prior session's
  process, but this is not a substitute for an actual Gradle build. Watch
  the next GitHub Actions run, specifically for: (1) the Room schema
  migration actually applying cleanly, (2) no missing/unused imports in
  the new Compose files, (3) `HomeScreen`'s existing behavior unchanged
  aside from the intended grid-height fix and the new 6th tile.
- **Build fix — `compileDebugKotlin` failure on `weight()` (earlier session):**
  GitHub Actions run "Add Focus Mode strict lock system #6" failed at
  `:app:compileDebugKotlin` with `Cannot access 'weight': it is internal in
  'androidx.compose.foundation.layout'` in `AnalyticsScreen.kt`,
  `HomeScreen.kt`, and `SettingsScreen.kt` — unrelated to Focus Mode itself,
  but it blocked the whole module from compiling, so Focus Mode couldn't be
  verified either. Root cause: all three files had
  `import androidx.compose.foundation.layout.weight`. `Modifier.weight()` is
  not a top-level function in that package — it's a member extension
  declared inside `RowScope`/`ColumnScope`, automatically in scope inside a
  `Row { }`/`Column { }` block with **no import needed**. The package does
  contain an internal top-level symbol also named `weight` (an
  implementation detail), which is what that import statement was actually
  resolving to — hence "internal", not "unresolved reference". Fix: removed
  the bad import from all three files; every `Modifier.weight(1f)` call site
  in each file was confirmed to already sit inside a `Row`/`Column` scope
  before removing it, so no new unresolved-reference errors were
  introduced. **Correction to an earlier diagnosis:** `app/build.gradle.kts`
  has a comment (on the explicit `androidx.compose.foundation:foundation`
  dependency) attributing a past version of this exact error to a
  foundation/foundation-layout version mismatch. Given what actually caused
  it this time, that comment's explanation is likely wrong — the errant
  import is a sufficient explanation on its own, in any version. Left the
  dependency declaration in place (harmless either way) but flagging the
  comment's reasoning as unverified/likely incorrect rather than leaving it
  stated as fact.

  Focus Mode files themselves (`FocusScreen.kt`, `BottomNavBar.kt`,
  `StudySpaceNavHost.kt`, `FocusModeViewModel.kt`, `FocusLockController.kt`,
  `FocusDuration.kt`) were separately re-reviewed line-by-line against every
  function/type they call into — no errors found in any of them. This CI
  failure was never actually in the lock system; it just prevented the lock
  system from reaching the compiler at all. **Verification status:** this
  fix removes the specific two-token change that caused the logged failure,
  checked manually (no compiler available in this environment either) by
  confirming every affected call site's scope — still not an actual
  Gradle/Kotlin compile. Watch the next GitHub Actions run to confirm this
  clears `compileDebugKotlin`, and that nothing else was hiding behind it
  (Kotlin's compiler stops listing further errors in a file once one is
  found, so there could in principle be an error later in one of these
  three files that this run's log never reached — unlikely given the rest
  of each file matched every other call site checked, but not something a
  static read can fully rule out the way a real compile would).
- **Strict Focus Mode — wiring the lock into the actual UI/navigation
  (earlier session):** Inspected the repo before writing anything, per the
  working rules, and found a real gap: `timer/FocusModeViewModel.kt`,
  `timer/FocusLockController.kt`, and `timer/FocusDuration.kt` already
  existed (setup/active/completed stage machine, duration validation,
  a lock singleton) but were never actually wired up —
  `screens/focus/FocusScreen.kt` was still the old Stage-3
  `StopwatchTimerViewModel`-based open-ended timer with no picker, no
  lock, no completion screen; `navigation/StudySpaceNavHost.kt` and
  `ui/components/BottomNavBar.kt` never read `FocusLockController.isLocked`
  at all, so nothing actually stopped bottom-nav taps or the system back
  gesture from leaving a "locked" session. This correction, not a rewrite
  from scratch, is what this session's changes are:
  - `screens/focus/FocusScreen.kt` (rewritten) — now drives off
    `FocusModeViewModel.stage`: a SETUP screen (preset chips from
    `FocusDuration.PRESET_MINUTES` + a custom minutes/seconds stepper,
    validation-error text, a confirmation dialog showing the exact
    selected duration before `confirmAndStart()`), an ACTIVE screen
    (locked-state badge, `ProgressRing` countdown, Pause/Resume, an
    "Emergency Exit" button behind its own confirmation dialog), and a
    COMPLETED screen ("Focus Session Completed" + Done). A `BackHandler`
    enabled only in the ACTIVE stage intercepts the system back
    gesture/button and redirects it into the same emergency-exit
    confirmation rather than letting it silently pop the screen.
  - `ui/components/BottomNavBar.kt` — `StudySpaceBottomNav` takes a new
    `locked: Boolean = false` param; every `NavigationBarItem` (including
    the currently-selected one) is disabled while locked, so a bottom-nav
    tap can't pop Focus off the back stack either.
  - `navigation/StudySpaceNavHost.kt` — reads
    `FocusLockController.isLocked` once and passes it into
    `StudySpaceBottomNav`, so both the back gesture (handled in
    `FocusScreen`) and bottom-nav taps (handled here) read the same single
    shared lock state rather than each keeping an independent flag — this
    is the "reliable shared state or session manager, not just visual
    button disabling" requirement.
  - `app/src/test/java/com/studyspace/timer/timer/FocusDurationTest.kt`
    (new) — first test file in the project (`app/src/test` didn't exist
    before this session). Covers `FocusDuration.validate`'s zero/negative/
    over-max/invalid-seconds rejection and correct millisecond conversion
    for both preset and custom minute+second inputs.
  - **Correction to the record:** Stage 7's log entry above says "no
    distraction-blocking is implemented anywhere in this app, by design"
    — that was accurate when written, but the lock/validation classes were
    added in an unlogged session sometime after, without a matching
    `PROJECT_STATE.md` entry or working `FocusScreen`/nav wiring. Leaving
    that Stage 7 text as-is (historical record, not rewritten) but flagging
    here that it no longer reflects the app's actual behavior as of this
    session. What's true now: navigation is locked, but only *within this
    app's own UI* (bottom nav + back gesture) — no Accessibility Service,
    no cross-app automation, no blocking of system functions (home button,
    notification shade, etc.) was added or considered, consistent with the
    "Decisions locked in" section below, which still holds.
  - **Verification status: written but UNVERIFIED**, same limitation as
    every prior stage — no Android SDK/Kotlin compiler in this container.
    Checked manually: brace/paren balance across all changed/new files
    (all balanced), that `FocusModeViewModel`'s existing public API
    (`stage`, `state`, `durationInput`, `validationError`,
    `selectPresetMinutes`, `updateCustomDuration`, `confirmAndStart`,
    `pause`, `resume`, `emergencyExit`, `acknowledgeCompletion`,
    companion `PRESET_MINUTES`) matches every call site the new
    `FocusScreen` makes against it, and that `TimerUiState`'s
    `progress`/`remainingMillis`/`isRunning` properties (used by the new
    `ActiveContent`) exist exactly as read. **Not exercised at all:** an
    actual Gradle/Compose compile, and — the single biggest real risk
    here — on-device behavior of the `BackHandler` + disabled-bottom-nav
    combination together (does a disabled `NavigationBarItem` still
    intercept the tap in a way that reads as "locked" rather than
    "broken"? does the back gesture on gesture-nav devices, not just the
    3-button back key, actually route through `BackHandler`?). Watch the
    next GitHub Actions run for compile errors, then device-test: start a
    short (e.g. 15s custom) Focus session, confirm both the bottom nav and
    back gesture/button are inert while the confirmation dialogs are
    closed, confirm Emergency Exit actually returns control and saves a
    partial session, and confirm natural completion also unlocks and
    re-enables navigation on its own.
- **Stage 8 — Settings, DataStore preferences, accessibility/animation
  polish (this session):** Inspected the Stage-7 zip first — confirmed via
  `PROJECT_STATE.md` and the actual `SettingsScreen.kt` source that Stage 7
  was complete and Settings was still the Stage 2 disabled-placeholder shell
  — before writing anything.

  - `settings/SettingsRepository.kt` (new) — DataStore-backed
    (`app_settings`, a new file, independent of `theme_settings` and
    `wallpaper_settings`) persistence for four values: timer-completion
    alerts (default on), keep-screen-on (default on), reduce-motion
    (default off), and daily goal minutes (default 240 = 4h). Every default
    matches the app's exact pre-Stage-8 behavior, so a user who never opens
    Settings sees zero behavior change from this stage.
  - `screens/settings/SettingsViewModel.kt` (new) — same
    `AndroidViewModel` + `StateFlow` + `WhileSubscribed(5_000)` pattern as
    `ThemesViewModel`/`HomeViewModel`. Read from both `SettingsScreen` and
    `MainActivity` (same Activity-scoped instance), which is how
    keep-screen-on and reduce-motion reach the whole app, not just the
    Settings screen.
  - `screens/settings/SettingsScreen.kt` (rewritten) — every switch is now
    real: checked state and `onCheckedChange` both wired to the ViewModel,
    nothing disabled. Added a "Daily study goal" card using the same
    `FilterChip`-preset-row pattern `PomodoroScreen.kt`/`TimerScreen.kt`
    already use for durations (60/120/180/240/360/480 min), rather than a
    free-entry field, for the same reasons those screens gave. Added an
    "Accessibility" section header + the reduce-motion row (previously the
    "Match system dark/light mode" row, which no longer describes anything
    real now that Stage 6 gave the app actual theme control — replaced
    with a setting that does something, rather than left as a stale
    placeholder label).
  - `service/TimerNotifications.kt` — added a second notification channel
    (`timer_completion_channel`, `IMPORTANCE_DEFAULT`, separate from the
    existing silent `IMPORTANCE_LOW` ongoing-progress channel) plus
    `buildCompletion()` and `notifyCompletion()`. `notifyCompletion()`
    catches `SecurityException` (POST_NOTIFICATIONS denied) and degrades to
    "no alert" rather than crashing — same fallback philosophy Stage 4's
    foreground-service notification already uses.
  - `timer/CountdownTimerViewModel.kt`, `timer/PomodoroViewModel.kt` — both
    now read `settingsRepository.timerCompletionAlertsEnabled` and, if true,
    call `TimerNotifications.notifyCompletion(...)` at the exact point
    completion is already detected (`onTimerCompleted()` /
    `onPhaseCompleted()`). **Deliberate architecture decision, explained in
    `TimerNotifications.notifyCompletion`'s doc comment:** this is called
    directly from the ViewModels rather than observed from
    `TimerForegroundService` watching `ActiveTimerSession` for the
    `COMPLETED` state — that session is cleared in the same call frame a
    completion is recorded, which would make the service's observation of
    the transient `COMPLETED` value a race against `ActiveTimerSession`
    flipping to `null`, not a reliable trigger. Calling it from the exact
    place completion is already known to have happened sidesteps that race
    entirely. Pomodoro alerts on **every** finished phase (work and both
    break types), not just Work — a Pomodoro user needs to know a break
    ended too, since (per a Stage 3 decision already locked in) the app
    never auto-starts the next phase. This is separate from, and doesn't
    change, the existing Work-only session-*recording* rule from Stage 5.
  - `StudySpaceApplication.kt` — added `settingsRepository` lazy holder,
    same pattern as the other three repositories.
  - `MainActivity.kt` — `StudySpaceTimerApp()` now also reads
    `SettingsViewModel` for two app-wide effects: keep-screen-on is applied
    via `LocalView.current.keepScreenOn`, kept true only while a timer is
    both active (`ActiveTimerSession`) *and* the setting is on, cleared the
    moment either stops being true; reduce-motion is provided app-wide via
    a new `LocalReduceMotion` CompositionLocal (`ui/theme/MotionPreferences.kt`)
    so animated composables can read it without threading a parameter
    through every call site.
  - `ui/components/ProgressRing.kt` — the one animation this stage adds:
    the arc now eases to a new `progress` value over 450ms
    (`animateFloatAsState` + `tween`) instead of snapping, for the Home
    dashboard's daily-progress ring. Respects `LocalReduceMotion` (snaps
    instead of easing when the user has that toggle on) and gained a new
    `animate: Boolean = true` parameter — set to `false` at
    `PomodoroScreen.kt`'s call site specifically, because that ring is
    already driven by a live ~200ms-ticking value; re-triggering a 450ms
    ease on every tick would make the displayed sweep visibly lag the real
    countdown instead of smoothing anything. This is still the *only*
    animation added anywhere in the app — Stage 2's "avoid unnecessary
    animations that reduce performance" rule was checked against (grepped
    the whole `com/studyspace/timer` tree for animation APIs before this
    stage; found none) and is otherwise still followed: no screen-transition
    or decorative motion was added.
  - `screens/home/HomeViewModel.kt`, `screens/home/HomeScreen.kt` — the
    fixed `DAILY_GOAL_MILLIS = 4h` constant is gone; `HomeViewModel` now
    exposes `dailyGoalMillis` from `SettingsRepository`, and the "Goal: Xh"
    label and progress-ring fraction both use it. Nothing on the Home
    screen is a static placeholder anymore, Stage 5's original goal for
    this screen.
  - `ui/components/FeatureCard.kt` — two real accessibility/UX fixes found
    during this stage's review, not new decoration: (1) tap ripple had been
    explicitly suppressed (`indication = null`) since Stage 2; switched to
    the default `clickable(onClick = onClick)` overload, which restores the
    standard Material ripple via `LocalIndication`. (2) the icon's
    `contentDescription` duplicated the visible title text directly below
    it (a screen reader would announce the same word twice); the icon is
    now marked decorative (`contentDescription = null`) and the whole card
    carries one merged `"title. subtitle"` description instead
    (`Modifier.semantics(mergeDescendants = true)`).
  - `res/values/strings.xml` — added the two new completion-channel strings
    (`notif_channel_completion`, `notif_channel_completion_desc`).
  - **What Stage 8's roadmap item "Theme preferences" refers to:** already
    real since Stage 6 (wallpaper + palette, DataStore-backed) — nothing
    new needed there this stage; the Settings screen links to the Themes
    tab for it instead of duplicating those controls.
  - **Verification status: written but UNVERIFIED, same as every stage
    since Stage 6.** No Android SDK/Kotlin compiler in this container.
    Checked manually: brace/paren balance across all 47 `.kt` files under
    `com/studyspace/timer` (all balanced, including the 3 new + 7
    rewritten/changed files this session), that every new/changed file's
    imports match the APIs it calls, and specifically re-verified the
    `(application as StudySpaceApplication)` smart-cast pattern used for
    `settingsRepository` in `CountdownTimerViewModel`/`PomodoroViewModel`
    against the identical pattern Stage 6's `ThemesViewModel` already uses
    successfully for `wallpaperRepository`/`paletteRepository`, rather than
    assuming it compiles. **Per the user's standing instruction, CI/device
    verification for Stages 6, 7, and now 8 remains deliberately batched
    until the remaining stages are built** — three stages' worth of
    unverified Kotlin/Compose/DataStore code will need checking together at
    that point. Biggest specific risks to watch in that first build: (a)
    `DataStore<Preferences>` usage itself is not new (Stages 6-7 already use
    it successfully for `theme_settings`/`wallpaper_settings`), so this is
    lower risk than, say, Stage 7's new `Canvas` chart API surface was; (b)
    `NotificationManagerCompat` and a second `NotificationChannel` are new
    API surface for this project, though both are long-stable, widely-used
    APIs, cross-referenced against current usage while writing them; (c) the
    `LocalReduceMotion` CompositionLocal is a new pattern for this codebase
    (Stage 6 threads `paletteId` as an explicit parameter instead) — chosen
    deliberately here so a future animated component doesn't need a
    parameter added to its signature, but worth confirming
    `CompositionLocalProvider` placement in `MainActivity.kt` actually
    covers every screen that reads it (it wraps the full `StudySpaceNavHost()`,
    so it should, but this hasn't been visually confirmed on a build).
- **Stage 7 — Pomodoro custom durations + real Analytics (earlier session):**
  Inspected the current Pomodoro/Focus/Analytics screens and the Stage 5
  Room layer before writing anything, per the working rules.

  - `data/repository/WeeklyAnalytics.kt` (new) — `DayTotal` (one calendar
    day + its summed duration) and `WeeklyAnalytics` (7 `DayTotal`s +
    a `Map<SessionType, Long>` of this-week totals per mode).
  - `data/repository/SessionRepository.kt` — added `weeklyAnalytics()`.
    Deliberately reuses the same `dao.sessionsSince(...)` query
    `studyStats()` already calls (verified/exercised since Stage 5) rather
    than adding a new `@Query` string — all the day/type bucketing happens
    in plain Kotlin over data already flowing through a tested query, so
    this doesn't introduce a second unverified Room/SQL surface. Unknown
    `type` strings (shouldn't occur, but the entity stores it as a plain
    String) are skipped via `runCatching { SessionType.valueOf(...) }`
    rather than crashing.
  - `screens/analytics/AnalyticsViewModel.kt` (new) — exposes
    `weeklyAnalytics` as a `StateFlow`, same `AndroidViewModel` +
    `WhileSubscribed(5_000)` pattern as `HomeViewModel`.
  - `screens/analytics/AnalyticsScreen.kt` (rewritten) — the "This Week"
    card is now a real 7-bar chart drawn on a bare Compose `Canvas` (no
    charting library added — project has stayed dependency-light
    throughout, and 7 rounded rectangles didn't need one). An all-zero week
    draws 7 flat minimum-height bars rather than 7 invisible ones, so a
    fresh install's Analytics tab doesn't look broken. "Session Breakdown"
    now shows all 5 `SessionType`s with real summed durations (0m for a
    type with no sessions this week, not omitted).
  - `timer/PomodoroViewModel.kt` — added `selectDurationMinutes(phase,
    minutes)` and three new `StateFlow<Int>`s (`workMinutes`,
    `shortBreakMinutes`, `longBreakMinutes`, defaulting to 25/5/15 —
    unchanged from Stage 3). Selection only takes effect while idle
    (mirrors `CountdownTimerViewModel.selectDuration`'s idle-only guard)
    and applies from the next time that phase is entered, so adjusting
    e.g. Short Break's length mid-Work-session is safe. Replaced the old
    fixed `WORK_DURATION_MILLIS`/etc. constants with `DEFAULT_*_MINUTES`
    (still used as the in-memory starting values) and three preset lists
    (`WORK_PRESET_MINUTES`, `SHORT_BREAK_PRESET_MINUTES`,
    `LONG_BREAK_PRESET_MINUTES`) for the picker UI. **Explicit, deliberate
    limitation, not an oversight:** these choices are in-memory only and
    reset to the defaults on next app launch — persisting them is Stage 8's
    DataStore-settings job, consistent with how Normal Timer's presets work
    today.
  - `screens/pomodoro/PomodoroScreen.kt` (rewritten) — added a `FilterChip`
    row (same pattern `TimerScreen.kt`'s Normal Timer tab already uses)
    shown only while idle, offering presets for whichever phase is up next.
    The center ring's idle-state label now reflects the *selected* duration
    for the upcoming phase instead of a hardcoded default.
  - Focus Mode: **not touched.** It was already a real, honest open-ended
    timer (Stage 3) recording real sessions (Stage 5); this project
    explicitly never implements distraction-blocking via Accessibility
    Services or cross-app automation (project rule), so there was nothing
    left in Stage 7's scope for it beyond what already exists.

  **Verification status: written but UNVERIFIED, same as every prior
  stage.** No Android SDK/Kotlin compiler in this container. Checked
  manually: brace/paren balance across all 44 `.kt` files under
  `com/studyspace/timer` (all balanced, including the 2 new + 2 rewritten
  files this session), that no leftover references to the removed
  `WORK_DURATION_MILLIS`/`SHORT_BREAK_DURATION_MILLIS`/
  `LONG_BREAK_DURATION_MILLIS` constants remained anywhere in the codebase
  after renaming them, and that every new/changed file's imports match the
  APIs it calls. **Per the user's explicit instruction this session, CI/
  device verification for both Stage 6 and Stage 7 is being deferred until
  all remaining stages are built** — this is a deliberate batching
  decision, not silent skipping, but it does mean two stages' worth of
  unverified Kotlin/Compose/Room code will need checking together at that
  point rather than one stage at a time. The single biggest combined risk
  to watch for in that first build: the `Canvas`-based bar chart in
  `AnalyticsScreen.kt` is the only genuinely new Compose API surface
  introduced since Stage 6 (`drawRoundRect`, `CornerRadius`, `Stroke`) —
  everything else in Stage 7 reuses patterns (idle-gated duration
  selection, `FilterChip` rows, `AndroidViewModel` + `WhileSubscribed`)
  already exercised by earlier, similarly-unverified stages.
- **Stage 6 — CI workflow recovery (earlier session):** The uploaded zip
  (`studyspace-stage6-wip.zip`) was missing `.github/workflows/build.yml`
  entirely, even though this file and `README.md` both referenced it as
  already existing — most likely dropped during a previous zip export
  (some zip tools skip dotfolders unless told not to). Without it, pushing
  to GitHub triggers **no build at all**, which would have silently blocked
  all of Stage 6's remaining verification steps. Recreated it to match the
  spec described in `README.md`'s "Note on the Gradle wrapper" and the file
  list below: `gradle/actions/setup-gradle` pinned to Gradle 8.7 (no
  `./gradlew`, since the wrapper jar isn't committed), JDK 17, runs unit
  tests then assembles the debug APK, uploads both as workflow artifacts,
  triggers on push/PR to `main` plus manual `workflow_dispatch`. Also fixed
  `README.md`'s "Project status" and "Known limitations" sections, which
  still said "Stage 1 of 10 — foundation only" despite Stages 2–5 being
  done and Stage 6 in progress.

  Reviewed all Stage 6 wallpaper/theme source for correctness at the same
  time (the actual reason for the review): palette color constants in
  `Color.kt` match every reference in `AppPalette.kt`; the
  ThemesViewModel → AppBackground → StudySpaceNavHost → MainActivity
  palette/wallpaper wiring is consistent end-to-end; wallpaper drawable
  resource names are valid; brace/paren balance re-checked clean across all
  42 `.kt` files. No code bugs found in the Stage 6 feature itself — the
  missing workflow file was the only real problem.

  **Verification status: workflow file is new and has itself never been
  run — this is now the single most important thing to watch on the next
  push.** If this run fails, the wallpaper/theme code review above doesn't
  matter until the build itself succeeds.
- **Stage 6 — Wallpapers & Themes, part 1 (earlier session):** Done ahead of
  strict stage order at the user's explicit request ("use the pic and also
  add a personalize option where user can add pics from his gallery"),
  alongside 16 wallpaper images the user supplied (`Pins.zip`).

  - `res/drawable-nodpi/wallpaper_01.webp` … `wallpaper_16.webp` — the
    user's own reference images, resized (max dimension 1280px) and
    re-encoded as WebP (quality 78) to keep them small (~1.6 MB total vs.
    ~2.75 MB source). Renamed from hash filenames to valid Android resource
    names; original filenames are not preserved anywhere in code.
  - `wallpaper/WallpaperCatalog.kt` (new) — `BuiltInWallpaper` data class +
    the fixed list of the 16 bundled wallpapers, keyed by a stable string id
    (not the drawable resource id, so DataStore-persisted selections survive
    a drawable being swapped later).
  - `wallpaper/WallpaperRepository.kt` (new) — DataStore-backed
    (`wallpaper_settings`) repository. Modes: default / built-in / custom.
    "Personalize" copies the picked gallery photo's bytes into
    `filesDir/wallpapers/` rather than keeping the picked `content://` Uri —
    the Photo Picker only guarantees that Uri's read access until the next
    reboot, so a private copy is what makes the choice durable. Replacing a
    custom photo deletes the previous file.
  - `screens/themes/ThemesViewModel.kt` (new) — `AndroidViewModel` exposing
    wallpaper selection + palette id as `StateFlow`s, plus
    `selectDefault()/selectBuiltIn()/onGalleryImagePicked()/selectPalette()`.
    Same instance is read from both `ThemesScreen` and `MainActivity`
    (Compose's `viewModel()` returns the same object for the same Activity
    owner), which is how a palette change reaches the whole app's theme.
  - `ui/theme/AppPalette.kt` (new) — `AppPalette` data class (id, label,
    preview gradient colors, Material `ColorScheme`) + `AppPalettes` object
    with three palettes matching the old static preview swatches: Galaxy
    (default), Nebula Blue, Twilight Lavender.
  - `ui/theme/Theme.kt` (rewritten) — `StudySpaceTimerTheme` now takes a
    `paletteId` param and looks up the scheme from `AppPalettes` instead of
    a single hardcoded `darkColorScheme`.
  - `theme/PaletteRepository.kt` (new) — DataStore-backed (`theme_settings`)
    persistence for the selected palette id, independent of the wallpaper
    DataStore file.
  - `StudySpaceApplication.kt` — added `wallpaperRepository` and
    `paletteRepository` lazy holders, same pattern as `sessionRepository`.
  - `ui/components/AppBackground.kt` (new) — paints the app-wide background
    behind the whole nav graph: base palette gradient (Default), a bundled
    wallpaper image, or the user's personalized photo, each with a
    palette-tinted scrim so text/cards stay legible over any photo.
  - `navigation/StudySpaceNavHost.kt` — wraps the existing bottom-nav
    `Scaffold` (now `containerColor = Color.Transparent`) in a `Box` with
    `AppBackground` behind it, reading selection + palette from
    `ThemesViewModel`.
  - `MainActivity.kt` — reads `paletteId` from the same `ThemesViewModel`
    and passes it into `StudySpaceTimerTheme`, so the palette applies to
    every screen's Material colors, not just the background.
  - `screens/themes/ThemesScreen.kt` (rewritten) — new "Wallpaper" section
    (Default tile, 16 bundled thumbnails, "Personalize" tile using
    `ActivityResultContracts.PickVisualMedia` — the system Photo Picker,
    which needs **no gallery/storage permission**) above the existing
    "Palette" section, whose three swatches are now clickable and wired to
    `selectPalette()` instead of being static previews.
  - `app/build.gradle.kts` — added `io.coil-kt:coil-compose:2.6.0`, needed
    to load the user's personalized photo (a file, not a bundled resource)
    into a Compose `Image`. No new manifest permissions were needed — the
    Photo Picker requires none.

  **What this does NOT yet cover from the Stage 6 roadmap:** verifying WebP
  drawables actually decode correctly at runtime (written but unverified);
  and confirming none of this broke Stage 2–5 screens, since none of it has
  been through an actual Gradle build yet.

  **Follow-up in this same session:** added 3 more palettes (Solar Flare,
  Emerald Nova, Crimson Nebula — 6 total now, new accent colors in
  `Color.kt`), and an explicit remove ("×") button on the Personalize tile
  (`WallpaperRepository.clearCustom()` deletes the stored photo file and
  reverts to Default) so removing a personalized photo doesn't require
  picking a replacement first.

  **Verification status: written but unverified.** No Android SDK is
  available in this environment. Push to GitHub and watch Actions for
  compile errors before trusting any of the above; then device-test:
  tapping each wallpaper tile actually changes the background behind every
  tab, "Personalize" opens the system photo picker and the chosen photo
  persists after force-closing the app, the "×" button actually clears it,
  and each of the 6 palette swatches actually re-colors buttons/cards
  app-wide.


  - `data/SessionType.kt` — new enum (`SELF_STUDY`, `ONLINE_STUDY`,
    `NORMAL_TIMER`, `POMODORO`, `FOCUS_MODE`) shared by the timer ViewModels
    and the data layer, stored in the DB as a plain string column rather
    than a Room-generated enum TypeConverter, so old rows stay readable if
    the enum's members ever change.
  - `data/db/StudySessionEntity.kt`, `StudySessionDao.kt`, `AppDatabase.kt`
    — the actual Room layer. One table (`study_sessions`): type, label,
    start time, duration, whether it finished naturally vs. was stopped
    early, and a precomputed `dateEpochDay` column so day-bucketed queries
    (today / this week / streak) don't need date math inside SQL. Version 1,
    no migrations yet (nothing to migrate from).
  - `data/repository/SessionRepository.kt` + `StudyStats.kt` — single access
    point for the DAO. `recordSession(...)` is the write path every timer
    ViewModel now calls; it silently drops anything under 10 seconds
    (`MIN_RECORDABLE_MILLIS`) so an accidental Start-then-Stop tap doesn't
    pollute history or inflate the streak. `studyStats()` combines a
    "last 7 days" query and an "all distinct session days, all-time" query
    into today's total/count, this-week total, and the current streak.
    **Explicit assumption, flagged rather than silently decided:** the
    streak counts today as still "current" even before the user has studied
    yet today (it only breaks once a day is skipped entirely) — see
    `computeStreak`'s doc comment for the exact rule.
  - `StudySpaceApplication.kt` (new, app-package root) — holds the Room DB
    + repository as lazy singletons; registered in `AndroidManifest.xml`
    (`android:name=".StudySpaceApplication"`). No DI framework added (Hilt/
    Koin) — this is the minimum needed for one repository, consistent with
    the project staying dependency-light. ViewModels reach it via
    `(application as StudySpaceApplication)` from inside `AndroidViewModel`;
    Compose's default `viewModel()` factory already supports constructing
    `AndroidViewModel`s automatically (falls back to
    `ViewModelProvider.AndroidViewModelFactory`), so none of the existing
    `viewModel(key = ...)` call sites needed a custom factory added.
  - `StopwatchTimerViewModel.kt`, `CountdownTimerViewModel.kt`,
    `PomodoroViewModel.kt` — all three now extend `AndroidViewModel` and
    call `repository.recordSession(...)` when a session ends with a
    non-zero elapsed time:
    - Stopwatch modes (Self-Study/Online Study/Focus Mode): saved from
      `reset()` only, always `completedNaturally = false` (a stopwatch has
      no target to "reach"). `start()` gained a `type: SessionType` param
      (default `SELF_STUDY`); call sites in `TimerScreen.kt` and
      `FocusScreen.kt` updated to pass the right one per screen.
    - Normal Timer (countdown): saved either from the engine's
      `onCompleted` callback when it reaches zero on its own
      (`completedNaturally = true`), or from `reset()` if stopped early
      with progress made (`completedNaturally = false`). A `reset()` called
      *after* natural completion does not double-save.
    - Pomodoro: **only Work phases are ever recorded** — break time isn't
      study time, so both the phase-completion path and the manual-reset
      path skip saving during Short/Long Break regardless of elapsed time.
      Explicit assumption, not an oversight — flagged here per project
      rules on decisions that get made silently otherwise.
  - `timer/TimeFormat.kt` — added `formatDurationHoursMinutes()` ("Xh Ym",
    dashboard style) and `formatRelativeTime()` ("23m ago" / "Yesterday" /
    etc.) alongside the existing running-clock `formatTimerDuration()`.
  - `screens/home/HomeViewModel.kt` (new) + `HomeScreen.kt` (rewritten) —
    Home's "Today's study time" ring, the Sessions/Streak/This-week stat
    cards, and "Recent Activity" are now real numbers from
    `repository.studyStats()` and `repository.recentSessions()` instead of
    Stage 2's static "0h 0m" / empty-state placeholders. The one remaining
    static piece: the 4-hour daily goal is a fixed constant
    (`DAILY_GOAL_MILLIS`) until Stage 8 adds a real settings-backed goal —
    called out in this file's doc comment, not glossed over.
  - Analytics screen was **not** touched this stage — per the roadmap,
    wiring real charts/statistics there is explicitly Stage 7's job, even
    though the Room data it'll need now exists.
  - **Verification status: written but UNVERIFIED.** Still no Android
    SDK/Kotlin compiler in this container. Checked manually instead:
    brace/paren balance across all 36 `.kt` files under
    `com/studyspace/timer` (all balanced, including the 8 new/rewritten
    files), that every changed/new file's imports match the APIs it calls
    (including removing a newly-unused import found during this pass), and
    that every call site of the three timer ViewModels' `start()`/`reset()`
    was updated consistently with the new signatures. This is a stronger
    check than nothing, but it is **not** a substitute for an actual Gradle/
    KSP/Room-annotation-processor compile — Room's compile-time query
    validation in particular (the DAO's `@Query` strings against the entity
    schema) has not been exercised at all outside manual reading. Watch the
    next GitHub Actions run closely, especially for KSP/Room-compiler
    errors, before trusting that the schema and queries are actually valid.
  - **Known limitation, stated plainly:** `studyStats()`'s "today"/"this
    week" window is computed once when the Flow is built (at ViewModel
    creation), not re-evaluated at midnight — if the Home screen is left
    open across a day boundary, the numbers won't roll over until the
    screen (and its ViewModel) is recreated. Not fixed this stage; noted
    rather than hidden.
- **Stage 4 — Background Timer (earlier session):** Inspected the Stage-3 zip
  (confirmed via `PROJECT_STATE.md` and the actual source that Stage 3 was
  complete, and that Stage 4's target packages — `service/`, plus anything
  Room/DataStore-shaped — were still empty) before writing anything.

  **Architecture decision made explicit before implementing** (flagged to
  the user and approved): the existing Stage 3 `TimerEngine` design has no
  single place a background service could read "what's currently running"
  from — each screen's ViewModel owns a private engine instance. Rather than
  a deeper rewrite (moving engine ownership into a bound service — out of
  scope for this stage) or a notification that silently goes stale off-
  screen, added one small app-wide singleton, `ActiveTimerSession`
  (`timer/ActiveTimerSession.kt`), that whichever `TimerEngine`-backed
  ViewModel is currently running publishes itself to (label + its own
  `StateFlow<TimerUiState>` + pause/resume/stop callbacks). Explicit,
  documented assumption: only **one** timer is tracked for background/
  notification purposes at a time (whichever was most recently started) —
  matches how the app is actually used, and is called out in the class doc
  rather than silently decided.

  - `timer/ActiveTimerSession.kt` — the singleton described above. Session
    ids prevent a screen a user has since left from clobbering a *different*,
    newer active session.
  - `StopwatchTimerViewModel.kt`, `CountdownTimerViewModel.kt`,
    `PomodoroViewModel.kt` — each now calls `ActiveTimerSession.publish(...)`
    in `start()` and clears it in `reset()` / `onCleared()` (Pomodoro also
    clears on every phase completion, since the engine returns to idle
    between phases and the notification shouldn't imply a phase is still
    running when the user hasn't tapped Start for the next one yet).
    `StopwatchTimerViewModel.start()` and `.reset()`/`.pause()`/`.resume()`
    signatures are otherwise unchanged except `start()` now takes an
    optional `label: String` param (defaults to "Study Session") so the
    Timer/Focus screens can pass "Self-Study Timer" / "Online Study Timer" /
    "Focus Mode" for notification + card display. `TimerScreen.kt` and
    `FocusScreen.kt` call sites updated accordingly (Normal Timer and
    Pomodoro labels are computed internally, no screen change needed there).
  - `service/TimerForegroundService.kt` — new `LifecycleService`. Does NOT
    own a `TimerEngine` itself; only observes `ActiveTimerSession.active`
    (`flatMapLatest`'d into that session's own `StateFlow`) and mirrors it
    into an ongoing notification, updated on every tick emission (~200ms
    per the Stage 3 engine's tick interval — acceptable for a low-importance
    silent notification, no debouncing added since `setOnlyAlertOnce(true)`
    already prevents any repeated alert/sound). Handles `ACTION_PAUSE` /
    `ACTION_RESUME` / `ACTION_STOP` intents from the notification's own
    action buttons by calling back into `ActiveTimerSession`'s stored
    callbacks. `START_NOT_STICKY` — deliberately does not auto-restart with
    stale/no data if the OS kills it.
  - `service/TimerNotifications.kt` — channel creation (`IMPORTANCE_LOW`,
    silent, no badge — an ongoing study timer shouldn't ping every tick) and
    notification building, kept separate from the service class itself.
  - `res/drawable/ic_notification_timer.xml` — new single-color vector icon
    for the status bar (notification small icons must be a plain silhouette;
    the existing multi-layer/multi-color launcher icon isn't valid for this).
  - `MainActivity.kt` — `StudySpaceTimerApp()` now observes
    `ActiveTimerSession.active` and, the moment it goes non-null: (a) calls
    `ContextCompat.startForegroundService(...)` for `TimerForegroundService`,
    and (b) on API 33+, requests `POST_NOTIFICATIONS` runtime permission —
    requested at that moment specifically (not on app launch), and only
    once per app session (`notificationPermissionRequested` flag) even if
    denied, to avoid re-prompting on every timer start.
  - `AndroidManifest.xml` — added exactly three permissions
    (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`,
    `POST_NOTIFICATIONS`) and the `<service>` declaration with
    `android:foregroundServiceType="specialUse"` + the required
    `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justification property — verified via
    web search against current Android 14 foreground-service-type docs
    before using this API shape (no built-in type — mediaPlayback, location,
    etc. — fits a study timer, and `specialUse` is the documented fallback
    for that case). No other permissions, no boot receiver, no
    Accessibility Service, no cross-app automation — none of that was added.
  - **Verification status: written but UNVERIFIED.** No Android SDK/Kotlin
    compiler in this container, same limitation as every prior stage.
    Checked manually instead: brace/paren balance across all 28 `.kt` files
    under `com/studyspace/timer` (all balanced), that every new/changed
    file's imports match the APIs it calls, and specifically verified via
    web search (not assumed from training data) that
    `ServiceCompat.startForeground(service, id, notification,
    foregroundServiceType)` is the real androidx-core 1.12+ signature (we're
    on 1.13.1, so it's available) and that the `specialUse` foreground
    service type + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` property is the
    correct, currently-documented API shape rather than a guess. This is
    stronger than nothing but **not** a substitute for an actual build —
    watch the next GitHub Actions run closely, and specifically watch for
    any `MissingForegroundServiceTypeException` or notification-permission-
    related crash on first real device test, since those two areas (new
    Android-14-era APIs) carry the most risk of the whole stage.
  - **Known limitation, stated plainly, not glossed over:** if the OS kills
    the entire app *process* (not just the service) while a timer is
    running, the in-memory timer state is lost — same as before this stage.
    True "resume exact elapsed time after process death" needs the session
    start-timestamp persisted to disk, which needs Room (Stage 5). This
    stage gets you background survival while the process stays alive
    (screen off, other apps in foreground, app swiped to recent-apps but not
    force-killed) — not process-death survival. Don't describe this stage as
    having solved that; it hasn't.
- **Stage 3 — Timer Engine (earlier session):** Inspected the Stage-2 zip
  (confirmed via `PROJECT_STATE.md` that Stage 2 was already complete)
  before writing anything. Added a new `timer/` package with the actual
  countdown/stopwatch logic all five timer modes now use:
  - `TimerEngine.kt` — the shared, drift-resistant core. Uses
    `SystemClock.elapsedRealtime()` (monotonic, immune to wall-clock/timezone
    changes) rather than accumulating fixed tick increments, so the display
    stays accurate even if a tick is delayed. Exactly one ticking coroutine
    `Job` runs at a time — `start`/`resume` always cancel any prior job
    first — so duplicate timer execution can't happen. Exposes
    `TimerUiState` (direction, run state, elapsed/target/remaining millis,
    progress) as a `StateFlow`.
  - `StopwatchTimerViewModel.kt` — open-ended count-up timer, used by
    Self-Study, Online Study, and Focus Mode (each screen gets its own
    instance via `viewModel(key = ...)`).
  - `CountdownTimerViewModel.kt` — countdown for the Normal Timer mode, with
    a fixed preset duration list (5/10/15/25/45/60m) selectable while idle.
  - `PomodoroViewModel.kt` — work/break cycle manager on top of the same
    engine: 25m work / 5m short break / 15m long break every 4th completed
    work session. On phase completion the engine resets to idle for the new
    phase rather than auto-starting it, so a session never silently keeps
    running unattended — the user taps Start for each phase.
  - `TimeFormat.kt` — shared `formatTimerDuration()` (`MM:SS` / `H:MM:SS`)
    used by every screen so display formatting is identical everywhere.
  - Rewrote `TimerScreen.kt`, `PomodoroScreen.kt`, and `FocusScreen.kt` to
    consume these ViewModels instead of the Stage 2 static placeholders —
    Start/Pause/Resume/Reset are now real and state-driven. The Timer
    screen's three tabs (Self-Study/Online Study/Normal) each keep an
    independent ViewModel instance obtained unconditionally every
    recomposition, so a running timer keeps ticking even while a different
    tab is shown, and switching tabs never resets or shares progress.
  - Added an explicit `kotlinx-coroutines-android:1.8.1` dependency to
    `app/build.gradle.kts` — the engine uses coroutines directly now, so
    this is pinned rather than relied on as an undeclared transitive
    dependency of `lifecycle-viewmodel-ktx`.
  - Home, Analytics, Themes, and Settings screens were **not** touched —
    Home's stats are still static placeholders (real numbers need Room in
    Stage 5), and there's nothing in Stage 3's scope for the other three.
  - **Verification status: written but UNVERIFIED.** Still no Android
    SDK/Kotlin compiler in this container. Checked manually instead: brace
    and paren balance across every `.kt` file under `com/studyspace/timer`
    (all balanced, including the 5 new/rewritten files), that
    `FilterChip`'s parameter order (`selected, onClick, label, ...`) matches
    the declared `material3:1.2.1` version via a docs lookup before using
    it, and that every new/changed file's imports match the APIs it calls
    with no stray or missing imports. This is a stronger check than
    nothing, but it is **not** a substitute for an actual Gradle/Kotlin
    compile — watch the next GitHub Actions run closely. The single biggest
    unverified risk: `TimerEngine`'s coroutine-based ticking loop compiles
    correctly by inspection but its runtime behavior (timing accuracy,
    cancellation on pause/reset, correct Pomodoro phase handoff) has not
    been exercised on a device or emulator — Stage 9's QA pass is the first
    point this gets exercised end-to-end, though early manual testing after
    the next CI build would be worthwhile before then.
- **Stage 2 — Core UI (earlier session):** Inspected `MainActivity.kt`, the theme
  files, `app/build.gradle.kts`, the manifest, and the empty package skeleton
  before writing anything, per the working rules. `navigation-compose` and
  `material-icons-extended` were already declared in Stage 1's Gradle file, so
  no dependency changes were needed.
  - Added 7 reusable Compose components under `ui/components/`: `GlassCard`
    (+ `GlassCardAccent`), `SectionHeader`, `FeatureCard`, `TimerCard`,
    `ProgressRing`, `PrimaryButton`/`SecondaryButton` (`Buttons.kt`), and
    `StudySpaceBottomNav`.
  - Added `navigation/Screen.kt` (sealed class of routes) and
    `navigation/StudySpaceNavHost.kt` (Scaffold + bottom nav + NavHost wiring
    all 7 destinations, with `launchSingleTop`/`saveState`/`restoreState` on
    bottom-nav taps so switching tabs doesn't rebuild screens or stack
    duplicate back-stack entries).
  - Added all 7 foundational screens: `screens/home/HomeScreen.kt` (galaxy
    dashboard — greeting, today's-study-time card with a `ProgressRing`,
    3 stat mini-cards, a 2-column quick-action grid for the 5 timer modes,
    and a "recent activity" empty state), `screens/timer/TimerScreen.kt`
    (tabbed Self-Study/Online Study/Normal shell), `screens/pomodoro/`,
    `screens/focus/`, `screens/analytics/`, `screens/themes/` (palette
    preview grid), `screens/settings/` (disabled placeholder switches so
    the UI doesn't imply working persistence).
  - Rewrote `MainActivity.kt` to call `StudySpaceNavHost()` instead of the
    Stage 1 placeholder Composable.
  - Every placeholder/inert control (disabled Start buttons, disabled
    Settings switches, "0h 0m" stats, empty-state copy) says in-UI or in a
    code comment which later stage wires it up for real, so nothing here
    could be mistaken for working timer/analytics/settings logic.
  - **Verification status: written but UNVERIFIED.** No Android SDK/Kotlin
    compiler is available in this working container (confirmed: no `kotlinc`
    on PATH, no network to fetch one), so nothing below has been compiled.
    Checked manually instead: brace/paren balance across all 20 `.kt` files
    under `com/studyspace/timer` (all balanced), that every new file's
    imports match the APIs it calls, and cross-referenced one uncertain API
    (`SecondaryTabRow`, whose minimum material3 version I wasn't sure of) via
    web search before deciding to use the long-stable `TabRow` instead to
    remove the risk. This is a stronger check than nothing, but it is **not**
    a substitute for an actual Gradle build — the first GitHub Actions run
    after this push is the real verification and should be watched closely.
- **Fix pass on Stage 1 (earlier session):** inspected the zip before changing
  anything, as instructed, and found two real problems left over from the
  previous pass:
  1. `AndroidManifest.xml` referenced `@mipmap/ic_launcher` but no launcher
     icon existed anywhere in `res/` — this would have failed CI at the
     resource-linking step (`AAPT2: resource mipmap/ic_launcher not found`),
     before a single Kotlin file even compiled. Added an adaptive icon
     (`res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`,
     backed by `drawable/ic_launcher_background.xml` and
     `ic_launcher_foreground.xml`) in the galaxy palette. minSdk is already
     26, so adaptive-icon-only (no legacy density mipmaps) is sufficient.
  2. The manifest pre-declared `FOREGROUND_SERVICE`,
     `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`,
     `RECEIVE_BOOT_COMPLETED`, and a `<service android:name=".service.TimerForegroundService">`
     pointing at a class that doesn't exist yet. This violates the explicit
     Stage 1 rule ("do not add unnecessary permissions") and doesn't belong
     until Stage 4 actually implements the foreground service. Removed all
     of it; Stage 4 should add the permissions and the service declaration
     together with the real `TimerForegroundService` class, not before.

## Files that exist so far
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
  `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle.kts`, `app/proguard-rules.pro` — unchanged since Stage 1;
  `navigation-compose` and `material-icons-extended` were already present
  and cover all of Stage 2's needs.
- `app/src/main/AndroidManifest.xml` — launcher activity plus (Stage 4)
  `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`,
  `POST_NOTIFICATIONS` permissions and the `TimerForegroundService`
  declaration with `foregroundServiceType="specialUse"`.
- `app/src/main/res/values/strings.xml`, `themes.xml`
- `app/src/main/res/drawable/ic_launcher_background.xml`,
  `ic_launcher_foreground.xml` and
  `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`,
  `ic_launcher_round.xml` — adaptive launcher icon (galaxy gradient +
  cyan/pink timer-ring glyph), safe for minSdk 26+.
- `app/src/main/java/com/studyspace/timer/MainActivity.kt` — renders
  `StudySpaceNavHost()` (Stage 2); now also starts `TimerForegroundService`
  and requests `POST_NOTIFICATIONS` when a timer becomes active (Stage 4).
- `app/src/main/java/com/studyspace/timer/StudySpaceApplication.kt` — holds
  the Room DB + `SessionRepository` as lazy singletons (Stage 5); registered
  in the manifest via `android:name`.
- `app/src/main/java/com/studyspace/timer/data/SessionType.kt`,
  `data/db/{StudySessionEntity,StudySessionDao,AppDatabase}.kt`,
  `data/repository/{SessionRepository,StudyStats}.kt` — the Room persistence
  layer (Stage 5).
- `app/src/main/java/com/studyspace/timer/ui/theme/Color.kt`, `Theme.kt`,
  `Type.kt` — base galaxy dark color scheme + typography (Material 3),
  unchanged since Stage 1.
- `app/src/main/java/com/studyspace/timer/ui/components/` — `GlassCard.kt`,
  `SectionHeader.kt`, `FeatureCard.kt`, `TimerCard.kt`, `ProgressRing.kt`,
  `Buttons.kt`, `BottomNavBar.kt` (Stage 2).
- `app/src/main/java/com/studyspace/timer/navigation/` — `Screen.kt`,
  `StudySpaceNavHost.kt` (Stage 2).
- `app/src/main/java/com/studyspace/timer/screens/{home,timer,pomodoro,
  focus,analytics,themes,settings}/` — one screen each (Stage 2 shells;
  `timer`, `pomodoro`, and `focus` rewritten in Stage 3 to use real timer
  state instead of static placeholders).
- `app/src/main/java/com/studyspace/timer/timer/` — `TimerEngine.kt`,
  `StopwatchTimerViewModel.kt`, `CountdownTimerViewModel.kt`,
  `PomodoroViewModel.kt`, `TimeFormat.kt` (Stage 3); `ActiveTimerSession.kt`
  added Stage 4; the three ViewModels became `AndroidViewModel`s and
  `TimeFormat.kt` gained `formatDurationHoursMinutes`/`formatRelativeTime`
  in Stage 5 to save/display real sessions.
- `app/src/main/java/com/studyspace/timer/screens/home/HomeViewModel.kt`
  (new, Stage 5) — feeds `HomeScreen.kt`'s real stats/recent-activity.
- `app/src/main/java/com/studyspace/timer/data/repository/WeeklyAnalytics.kt`,
  `screens/analytics/AnalyticsViewModel.kt` (new, Stage 7) — feed
  `AnalyticsScreen.kt`'s real weekly chart + per-mode breakdown.
  `theme/PaletteRepository.kt`, `wallpaper/{WallpaperCatalog,
  WallpaperRepository}.kt`, `ui/theme/AppPalette.kt`,
  `ui/components/AppBackground.kt` (Stage 6) — wallpaper/palette
  persistence and app-wide application.
- `app/src/main/java/com/studyspace/timer/service/` — `TimerForegroundService.kt`,
  `TimerNotifications.kt` (Stage 4).
- `app/src/main/res/drawable/ic_notification_timer.xml` — status-bar
  notification icon (Stage 4).
- `.github/workflows/build.yml` — CI workflow: assembles debug APK + runs
  unit tests on push/PR/manual dispatch, uploads APK and test reports as
  build artifacts. Uses `gradle/actions/setup-gradle` (pinned Gradle 8.7)
  instead of `./gradlew`, since no wrapper jar is committed.
- `README.md` — phone-only build instructions (create repo → upload zip
  contents → Actions tab → download APK artifact).
- Still-empty package skeleton: `data/{db,repository}`, `settings/`,
  `analytics/`, `utils/` — populated in later stages.
- **Expansion Phase 2 (Study Goals):** `data/db/{StudyGoalEntity,
  StudyGoalDao}.kt`; `AppDatabase.kt` gained `MIGRATION_1_2` and version 2;
  `data/repository/{GoalProgress,GoalRepository}.kt`; `ui/components/
  GoalProgressCard.kt`; `screens/goals/{GoalsScreen,GoalsViewModel}.kt`;
  `navigation/Screen.kt` gained `Goals`; `StudySpaceApplication.kt` gained
  `goalRepository`; `HomeScreen.kt`/`StudySpaceNavHost.kt` wired the new
  quick-action tile and route; `data/repository/GoalProgressTest.kt` (new
  unit test).
- **Expansion Phase 3 (Subjects):** `data/db/{SubjectEntity,SubjectDao}.kt`
  (new); `data/db/StudySessionEntity.kt` gained `subjectId` + a foreign key
  to `SubjectEntity`; `data/db/StudySessionDao.kt` gained
  `sessionsForSubject`; `AppDatabase.kt` gained `MIGRATION_2_3` and version
  3; `data/repository/{SubjectStats,SubjectRepository}.kt` (new);
  `data/repository/SessionRepository.kt` gained `subjectStats`/
  `computeSubjectStats` and an optional `subjectId` param on
  `recordSession`; `ui/components/StatMiniCard.kt` (new, extracted from
  `HomeScreen.kt`'s former private copy); `screens/subjects/
  {SubjectsScreen,SubjectsViewModel,SubjectDetailScreen,
  SubjectDetailViewModel,SubjectDetailViewModelFactory,
  SubjectEditorDialog}.kt` (new); `navigation/Screen.kt` gained `Subjects`/
  `SubjectDetail`; `StudySpaceApplication.kt` gained `subjectRepository`;
  `HomeScreen.kt`/`StudySpaceNavHost.kt` wired the new quick-action tile
  and both routes; `data/repository/SubjectStatsTest.kt` (new unit test).
- **Expansion Phase 4 (Tasks):** `data/db/{TaskEntity,TaskDao}.kt` (new);
  `data/db/StudySessionEntity.kt` gained `taskId` + a second foreign key;
  `AppDatabase.kt` gained `MIGRATION_3_4` and version 4;
  `data/repository/{TaskRepository,TaskDueInfo}.kt` (new);
  `data/repository/SessionRepository.kt` gained an optional `taskId` param
  on `recordSession`; `screens/tasks/{TasksScreen,TasksViewModel,
  TaskEditorDialog}.kt` (new); `navigation/Screen.kt` gained `Tasks`;
  `StudySpaceApplication.kt` gained `taskRepository`; `HomeScreen.kt`/
  `StudySpaceNavHost.kt` wired the new quick-action tile and route (Home is
  now 8 quick-action tiles — see the Phase 4 session log's note on this);
  `data/repository/TaskDueInfoTest.kt` (new unit test).
- **Expansion Phase 5 (Advanced timer system):** no new/changed data
  layer or migration. `timer/{StopwatchTimerViewModel,
  CountdownTimerViewModel,FocusModeViewModel,PomodoroViewModel}.kt` all
  gained `subjects`/`tasks`/`selectedSubjectId`/`selectedTaskId`/
  `customLabel` state + `selectSubject`/`selectTask`/`setCustomLabel`, and
  their save paths now pass real `subjectId`/`taskId` into
  `recordSession()`; `PomodoroViewModel` also gained
  `sessionsPerLongBreak`/`selectSessionsPerLongBreak` (renamed the old
  fixed constant to `DEFAULT_SESSIONS_PER_LONG_BREAK`). New
  `ui/components/SessionAttributionPicker.kt` (shared label/subject/task
  picker + the `filteredForSubject` helper) used by
  `screens/timer/TimerScreen.kt` (both stopwatch tabs and the countdown
  tab), `screens/pomodoro/PomodoroScreen.kt` (plus a new
  `SessionsPerLongBreakPicker`), and `screens/focus/FocusScreen.kt`'s
  `SetupContent`.
- **Expansion Phase 6 (Study Planner):** `data/db/{PlannedSessionEntity,
  PlannedSessionDao}.kt` (new); `AppDatabase.kt` gained `MIGRATION_4_5` and
  version 5 (purely additive — no `study_sessions` rebuild); `data/
  repository/{PlannedSessionTime,PlannedSessionRepository}.kt` (new);
  `screens/planner/{PlannerScreen,PlannerViewModel,
  PlannedSessionEditorDialog}.kt` (new); `navigation/Screen.kt` gained
  `Planner` and `TimerFromPlan`; `screens/timer/TimerScreen.kt` gained
  optional `initialSubjectId`/`initialTaskId` params + a `LaunchedEffect`
  that pre-selects them on the Self-Study tab; `StudySpaceApplication.kt`
  gained `plannedSessionRepository`; `HomeScreen.kt`/
  `StudySpaceNavHost.kt` wired the new quick-action tile and both new
  routes (Home is now 9 quick-action tiles); `data/repository/
  PlannedSessionTimeTest.kt` (new unit test).
- **Expansion Phase 7 (Planned vs Actual Analytics):** `data/repository/
  PlannedVsActual.kt` (new); `data/db/PlannedSessionDao.kt` and
  `data/repository/PlannedSessionRepository.kt` gained `sessionsSince`;
  `data/db/StudySessionDao.kt`'s existing `sessionsSince` is now also
  exposed via a new one-line wrapper on `SessionRepository`;
  `screens/analytics/AnalyticsViewModel.kt` gained `plannedVsActual`;
  `screens/analytics/AnalyticsScreen.kt` gained the "Planned vs Actual"
  section (`PlannedVsActualSection`/`PlannedVsActualCard`/
  `PlannedVsActualStat`) in both layouts; `data/repository/
  PlannedVsActualTest.kt` (new unit test). No migration this phase.
- **Expansion Phase 8 (Advanced Analytics):** `data/repository/
  AdvancedAnalytics.kt` (new); `data/db/StudySessionDao.kt` gained
  `allSessions()`; `data/repository/SessionRepository.kt` gained a
  one-line `allSessions()` wrapper; `screens/analytics/AnalyticsViewModel.kt`
  gained `selectedRange`/`selectRange`/`subjects`/`advancedStats`/
  `subjectTotals`/`productivityPatterns`; `screens/analytics/
  AnalyticsScreen.kt` gained the range selector and three new sections
  (`RangeSelectorRow`, `OverviewStatsGrid`, `SubjectBreakdownList`,
  `ProductivityPatternsCard`/`ProductivityPatternRow`) in both layouts;
  `data/repository/AdvancedAnalyticsTest.kt` (new unit test, 14 cases). No
  migration this phase.
- **Expansion Phase 9 (Streaks & Achievements):** `data/repository/
  Achievements.kt` (new: `StreakSummary`/`AchievementId`/`Achievement`,
  `computeBestStreak`/`computeWeeklyConsistency`/`computeGoalEverMet`/
  `computeAchievements`); `screens/achievements/{AchievementsScreen,
  AchievementsViewModel}.kt` (new); `navigation/Screen.kt` gained
  `Achievements`; `HomeScreen.kt`/`StudySpaceNavHost.kt` wired the new
  quick-action tile and route (Home is now 10 quick-action tiles);
  `data/repository/AchievementsTest.kt` (new unit test, 16 cases). No new
  Room query, no migration — pure composition over four already-existing
  flows.
- **Expansion Phase 10 (Daily Dashboard):** `screens/home/HomeViewModel.kt`
  gained `todaySubjectTotals`/`subjects`/`todaysPlans`/`remainingTasks`;
  `screens/home/HomeScreen.kt` reorganized (new `QuickStartRow`,
  `SubjectBreakdownSection`, `TodaysPlanSection`, `RemainingTasksSection`;
  "Quick Actions" header renamed "All Features"; streak stat gained a 🔥
  prefix) in both layouts; `navigation/Screen.kt` gained
  `QuickStartTimer`; `screens/timer/TimerScreen.kt` gained an `initialTab`
  param; `navigation/StudySpaceNavHost.kt` wired the new route and
  `onOpenTimerTab` callback. No new Room query, no migration — every new
  number reuses an existing Phase 4/6/7/8 repository flow.

- **Expansion Phase 11 (Daily Summary):** `data/db/TaskEntity.kt` gained
  nullable `completedAtEpochMillis`; `data/db/AppDatabase.kt` gained
  `MIGRATION_5_6` and version 6; `data/repository/TaskRepository.kt`
  gained the pure `applyTaskCompletion` and `setCompleted` now uses it;
  `data/repository/DailySummary.kt` (new); `screens/summary/
  {DailySummaryScreen,DailySummaryViewModel}.kt` (new); `navigation/
  Screen.kt` gained `DailySummary`; `StudySpaceNavHost.kt`/`HomeScreen.kt`
  wired the new route and tile (Home is now 11 quick-action tiles);
  `data/repository/{DailySummaryTest,TaskCompletionTest}.kt` (new unit
  tests, 23 cases).

- **Expansion Phase 12 (Notifications & Reminders):** new package
  `reminders/` — `ReminderPlanner.kt` (pure: `ReminderKind`,
  `ScheduledReminder`, `computeNextReminder`, `remindersDueAt`,
  `minutesUntil`, `describePlannedSession`, `shouldNotifyGoalReached`),
  `ReminderScheduler.kt` (single-alarm scheduling + `handleAlarm`),
  `ReminderReceiver.kt`, `BootReceiver.kt`, `ReminderNotifications.kt`
  (3 channels + posting), `GoalNotifier.kt`; `settings/ReminderSettings.kt`
  (new, pure); `settings/SettingsRepository.kt` gained `reminderSettings`,
  the setters, and `lastGoalNotifiedDay`/`setLastGoalNotifiedDay`;
  `screens/settings/{SettingsViewModel,SettingsScreen}.kt` gained the
  reminder toggles, daily-time chips, permission request and blocked-state
  card; `data/repository/SessionRepository.kt` gained the optional
  `onSessionRecorded` hook; `StudySpaceApplication.kt` gained
  `applicationScope`, the hook wiring, and `onCreate()` (channels +
  `ReminderScheduler.startObserving`); `AndroidManifest.xml` gained
  `RECEIVE_BOOT_COMPLETED` and the two receivers; `res/values/strings.xml`
  gained the 3 channel names/descriptions;
  `test/.../reminders/ReminderPlannerTest.kt` (new, 30 cases). No migration.

- **Expansion Phase 13 (Data export/import):** new package `data/transfer/`
  — `CsvCodec.kt` (escape, formula guard, RFC 4180 parser),
  `StudyDataExport.kt` (`TransferFormat`, `CsvColumns`, `ExportBundle`,
  `buildCsvExport`, `buildJsonExport`, `exportFileName`),
  `StudyDataImport.kt` (`parseStudyDataFile`, validation, `planImport`,
  `ImportPlan`, `TaskKeyResolver`, `ImportLimits`),
  `DataTransferRepository.kt` (DB reads + transactional insert-only apply,
  `ImportResult`); new `screens/data/{DataManagementScreen,
  DataManagementViewModel}.kt`; `navigation/Screen.kt` (+`DataManagement`),
  `StudySpaceNavHost.kt` (route + Settings callback);
  `screens/settings/SettingsScreen.kt` (new "Data" section + `onOpenDataManagement`
  param, default `{}` so nothing else breaks); `StudySpaceApplication.kt`
  (+`dataTransferRepository`); `app/build.gradle.kts` (+test-only
  `org.json:json`); tests `data/transfer/{CsvCodecTest,StudyDataExportTest,
  StudyDataImportTest}.kt` (71 cases). No migration, no manifest change.

- **Expansion Phase 14 (Backup & restore):** new
  `data/transfer/StudyDataBackup.kt` (`BackupContent`, `SettingsSnapshot`,
  `ThemeSnapshot`, `ImportedPlannedSession`, `buildBackupJson`,
  `parseBackupFile`, `backupFileName`); `StudyDataImport.kt`'s
  `parseJsonExport` widened `private` → `internal` (reused by the backup
  parser) and given a cross-message when a backup file is fed to it;
  `DataTransferRepository.kt` gained `BackupExportBundle`, `RestoreResult`,
  `loadBackupExportBundle`, `restoreBackup`, and a refactored-out
  `insertParsedData` (shared by `applyImport` and `restoreBackup`);
  `DataManagementViewModel.kt`/`Screen.kt` gained the backup/restore state
  machines and a "Backup & restore" UI section (create/restore, preview,
  a second explicit replace-confirmation dialog, done/failed cards);
  `SettingsRepository.kt` gained `lastBackupEpochMillis`; DAOs
  (`StudySessionDao`, `SubjectDao`, `TaskDao`, `StudyGoalDao`,
  `PlannedSessionDao`) each gained `deleteAll()`, and `PlannedSessionDao`
  also gained `allPlannedSessions()`. **Also fixed, not new:**
  `StudyGoalDao.upsert`'s doc + `goalForScope`'s `ORDER BY`, and
  `GoalRepository.setWeeklyGoalMinutes` now deletes the scope before
  inserting (see Phase 14's own entry above for the bug this fixes). Test:
  `data/transfer/StudyDataBackupTest.kt` (13 cases, 2 added in Phase 15 —
  see below). No migration, no manifest change.

- **Expansion Phase 15 (Themes & customization):** new
  `ui/theme/ContrastUtils.kt` (`contrastSafeContentColor`, moved here from
  `Buttons.kt` where it was `private`; `Buttons.kt` now imports it instead);
  `ui/theme/Color.kt` gained the `DeepSpace*`/`Midnight*`/`MinimalDark*`/
  `MinimalLight*` constants; `ui/theme/AppPalette.kt` gained the `deepSpace`/
  `midnight`/`minimalDark`/`minimalLight` palettes (11 total now) and the
  `AppPalette.effectiveColorScheme(accentArgb: Int?)` extension; `Theme.kt`'s
  `StudySpaceTimerTheme` gained an `accentArgb` parameter;
  `theme/PaletteRepository.kt` gained `customAccentArgb`/`setCustomAccent`/
  `clearCustomAccent`; `screens/themes/ThemesViewModel.kt` gained
  `accentArgb`/`setAccent`/`clearAccent`; `ThemesScreen.kt` gained an
  "Accent color" section (curated swatch row + `AccentSwatch` composable);
  `MainActivity.kt` reads and passes through the new accent state. New
  `service/CompletionFeedback.kt` (`ToneGenerator` + `Vibrator`/
  `VibratorManager`); `SettingsRepository.kt` gained `soundEnabled`/
  `vibrationEnabled`; `SettingsViewModel.kt`/`SettingsScreen.kt` gained the
  "Sound"/"Vibration" rows (and reworded "Timer completion alerts"'s
  subtitle, which had been describing the notification channel's own
  incidental sound); `CountdownTimerViewModel`/`FocusModeViewModel`/
  `PomodoroViewModel`'s `maybeNotifyCompletion` each gained a
  `CompletionFeedback.play(...)` call alongside the existing notification
  call. `AndroidManifest.xml` gained the `VIBRATE` permission (normal,
  install-time). `data/transfer/StudyDataBackup.kt`'s `SettingsSnapshot`/
  `ThemeSnapshot` extended with the new fields (defaulted/nullable, so a
  pre-Phase-15 backup still parses); `DataManagementViewModel.kt`'s
  snapshot/restore functions updated to match. Test: 2 cases added to
  `StudyDataBackupTest.kt` (accent round-trip, pre-Phase-15-backup
  defaulting). **No automated test for `CompletionFeedback` or
  `effectiveColorScheme`/the new palettes** — no Robolectric in this
  project, same limitation as Phase 12's alarm scheduling. No migration.
  One new manifest permission (`VIBRATE`).

## Known gaps / next actions
- Background survival now works while the app *process* stays alive
  (screen off, other apps foregrounded, service running) — Stage 4. It does
  **not** survive the OS killing the whole process; that needs the session
  start-timestamp persisted to disk, which is Stage 5's job (Room). Don't
  describe process-death survival as solved.
- Background tracking is single-session by design (`ActiveTimerSession`
  holds one active timer at a time, whichever was started most recently) —
  documented assumption, not an oversight, but worth knowing if a future
  stage wants simultaneous multi-timer background tracking.
- Room DB now exists and Home's stats/recent-activity are real (Stage 5).
  Analytics and every Settings switch are still static/disabled placeholders
  — Analytics' real charts are explicitly Stage 7's job (the data already
  exists for it); Settings persistence is Stage 8 (DataStore).
- Home's daily goal (4h) is a fixed constant, not user-configurable yet —
  Stage 8.
- `studyStats()`'s today/this-week window doesn't roll over at midnight if
  the Home screen is left open across the boundary — see the Stage 5
  session log entry above.
- Process death still loses in-progress (unsaved) elapsed time, same as
  before — a session is only written to Room when it *ends* (stopped or
  completed), not incrementally while running. If the app process is killed
  mid-session, that session's progress is lost, same limitation described
  under Stage 4 above.
- Stage 4's foreground-service/notification code is written but unverified
  by an actual build or device — see the Session log entry above for
  exactly what was and wasn't checked. `MissingForegroundServiceTypeException`
  and notification-permission behavior are the biggest real risks; watch
  the next GitHub Actions run and test on a real device before trusting
  the background behavior itself.
- Pomodoro/Normal Timer durations are fixed presets, not arbitrary
  user-entered values — deferred to Stage 7/8 alongside DataStore settings.
- Stage 3 timer code is **written but unverified by an actual build or a
  device** — see the Session log entry above for exactly what was and
  wasn't checked. The ticking coroutine's runtime behavior in particular
  has not been exercised; watch the next GitHub Actions run for compile
  errors, and manually test start/pause/resume/reset on a device before
  treating the timing behavior itself as trustworthy.
- CI workflow itself also still unverified from Stage 1 (no run has been
  observed by Claude) — the launcher-icon fix in particular needs
  confirming.
- Gradle wrapper `.jar` binary is intentionally NOT committed (see README
  "Note on the Gradle wrapper").

## Decisions locked in (don't re-decide silently)
- Kotlin + Jetpack Compose + Material 3, min SDK 26, target/compile SDK 34.
- Namespace / applicationId: `com.studyspace.timer`.
- Persistence: Room (sessions) + DataStore Preferences (settings/theme).
- No Accessibility Services, no cross-app automation, no unneeded permissions.
  `RECEIVE_BOOT_COMPLETED` (Phase 12) is the one permission added after Stage 4
  and is needed, not incidental — see the Phase 12 entry. Reminders are
  inexact-alarm-based by design; no `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`.
- Background timer tracking (Stage 4) is single-active-session by design —
  `ActiveTimerSession` tracks one timer at a time, not one per screen/tab.
- Session recording (Stage 5): sessions under 10s aren't saved; only
  Pomodoro **Work** phases are recorded as study sessions (breaks are not);
  a session is written once, when it ends — no incremental/partial writes
  while running.
- **Phase 14 backup/restore semantics (don't re-decide):** a *backup* is a
  full-fidelity **replace** artifact (opposite of Phase 13's always-additive
  *export/import*) — restoring deletes and reinserts every subject, task,
  session, planned session and goal in one transaction. A backup does
  **not** include a custom Personalize wallpaper photo (private on-device
  file; not base64-inflated into the JSON) or achievements (computed, never
  stored). Restore only ever runs after an explicit second "yes, replace
  everything" confirmation beyond the preview itself — never wire a restore
  path that writes on a single tap. `StudyGoalDao.upsert`'s `REPLACE`
  strategy does **not** collide on a `NULL` `subjectId` (SQLite treats two
  `NULL`s as distinct in a unique index) — any code setting the weekly goal
  must `deleteScope` first, same as `GoalRepository.setWeeklyGoalMinutes`
  and `DataTransferRepository.restoreBackup` both already do.
