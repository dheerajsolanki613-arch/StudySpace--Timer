# StudySpace Timer

A premium Android study-productivity app — Study Timer, Pomodoro, Focus
Mode, progress tracking, analytics, and a dark "galaxy" neon-glass visual
theme. Built with Kotlin + Jetpack Compose + Material 3.

This project is designed to be built **entirely from a phone**, using
GitHub Actions as the build machine — no computer or Android Studio
required.

## Project status

🚧 **Stage 6 of 10 — Wallpapers & Themes (in progress).** Core UI, the
timer engine, background/notifications, Room-based study tracking, and the
wallpaper/palette picker are all implemented. See `PROJECT_STATE.md` for
exactly what's implemented, what's verified vs. unverified, and what's
next.

## How to build from your phone

1. **Create a GitHub repo** (via the GitHub app or mobile browser) and
   upload the contents of this zip to it — either:
   - the GitHub mobile app's "upload files" flow, or
   - a mobile file manager + the GitHub web UI's drag-and-drop uploader.
2. Once the files are pushed to the `main` branch, GitHub Actions will
   automatically start a build (see `.github/workflows/build.yml`).
3. Open your repo → **Actions** tab → the latest **"Build StudySpace
   Timer"** run.
4. When it finishes (green check), scroll to **Artifacts** at the bottom of
   the run page and download `studyspace-timer-debug-apk`.
5. Unzip that artifact on your phone to get the `.apk`, then install it
   (you'll need to allow "install unknown apps" for whichever app you use
   to open the file).

You can also trigger a build manually from the **Actions** tab using
**"Run workflow"** (the workflow has `workflow_dispatch` enabled).

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Gradle (Kotlin DSL), Android Gradle Plugin 8.5.2
- Min SDK 26, target/compile SDK 34
- Room (local session storage) + DataStore Preferences (settings/theme) —
  added in later stages
- GitHub Actions for CI builds (`gradle` invoked directly via
  `gradle/actions/setup-gradle`, not `./gradlew` — see note below)

## Note on the Gradle wrapper

This repo does **not** include a committed `gradle-wrapper.jar` binary,
because it was generated in an environment without network/SDK access to
produce one safely. The CI workflow instead uses
`gradle/actions/setup-gradle` to provision Gradle 8.7 directly. If you add
a real wrapper later (`gradle wrapper` run on a machine with network
access), you can switch the workflow back to `./gradlew` — both work.

## Multi-session / multi-account development

This project is being built in stages, sometimes across different Claude
conversations or accounts. `PROJECT_STATE.md` is the living status file —
read it first in any new session. `CONTINUE_PROMPT.md` has a ready-to-paste
prompt for resuming work.

## Known limitations (Stage 6)

- Analytics charts, real Settings persistence, and Focus Mode/Pomodoro
  polish are not yet built — those are Stages 7–8.
- Nothing in this repo has been compiled or run on a device by Claude; it
  has no Android SDK or network access. First real verification happens
  when GitHub Actions runs the workflow above — see `PROJECT_STATE.md` for
  the specific things to check after this push (wallpaper picker, the
  gallery "Personalize" flow, and the 6 color palettes).
