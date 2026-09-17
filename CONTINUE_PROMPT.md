# StudySpace Timer — Continuation Prompt

Paste this at the start of a NEW Claude conversation (possibly in a different
Claude account) to resume work on this project without losing context.

---

## How to use this file

1. Start a new chat.
2. Upload the latest `studyspace-stageN.zip` I gave you (or paste this whole
   file if you don't have the zip yet).
3. Paste the prompt block below as your first message, filling in the
   `CURRENT STAGE` line.
4. Ask Claude to `view` the uploaded/unzipped project before changing anything.

---

## PROMPT TO PASTE

I'm continuing an existing Android project called **StudySpace Timer**,
built across multiple chat sessions (sometimes in different Claude
accounts), so you don't have memory of earlier work. Treat the attached zip
as the current source of truth — inspect it before writing or changing
anything. Do not regenerate files from scratch or assume a blank project.

**What the app is:** A premium Android study-productivity app (Kotlin +
Jetpack Compose + Material 3), with a dark "galaxy/twilight" neon-glass
visual style. Features: Study Timer, Pomodoro, Focus Mode, background/
foreground-service timer with notifications, local study-session tracking
(Room), analytics dashboard, theme/wallpaper picker, and a settings screen.
It must build via **GitHub Actions** (no local Android Studio — I only have
a phone), using Gradle with Kotlin DSL.

**Ground rules that apply to every stage:**
- Inspect existing files before editing; don't remove working code without
  checking what it does.
- No unnecessary permissions, no Accessibility Services, no automation of
  other apps, no hidden background behavior.
- Keep user data local unless a stage explicitly says otherwise.
- Be honest about what's verified vs. not: this environment has no Android
  SDK and no network, so nothing has been compiled or run on a device.
  Never claim a build succeeded, an APK was produced, or tests passed
  unless GitHub Actions actually ran and you saw the result. Say clearly
  when something is "written but unverified."
- At the end of **every stage**, package the whole current project into a
  zip (excluding `.git`, `build/`, `.gradle/`, caches) and give it to me as
  a downloadable file, plus a short stage-completion checklist.
- Update `PROJECT_STATE.md` (in the zip root) with what changed and what
  stage comes next, so the next session/account can pick up seamlessly.

**Current stage:** `<< fill in: e.g. "Stage 3 — Timer Engine, continuing from the zip below" >>`

**Full 10-stage plan** (see `PROJECT_STATE.md` for detailed stage specs and
progress) is:
1. Project setup & Gradle/GitHub Actions foundation
2. Core UI (galaxy dashboard, nav, reusable components)
3. Timer engine (study/pomodoro/focus core countdown logic)
4. Background timer (foreground service + notifications)
5. Study tracking (Room persistence, dashboard stats)
6. Wallpapers & themes (palette system, persistence)
7. Pomodoro + Focus Mode + Analytics (full features)
8. Settings & animations (polish, accessibility)
9. Testing & bug fixing (QA pass, honest verification report)
10. Final build, release docs, and packaged ZIP

---

## Why this file exists

You mentioned this project is being built across **multiple Claude
accounts/sessions**. Claude has no memory between separate conversations
(even within the same account, unless you've enabled memory), so each new
chat starts blind. This file — plus the zip's `PROJECT_STATE.md` and the
actual source code — is what lets a fresh Claude pick up exactly where the
last one left off instead of guessing or restarting.
