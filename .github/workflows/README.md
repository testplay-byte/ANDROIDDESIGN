# GitHub Actions Workflows — ANDROIDDESIGN

> This folder lives at the repo root (NOT inside the `ANDROIDDESIGN/` wrapper folder) because
> GitHub Actions ONLY detects workflows at `<repo-root>/.github/workflows/`. This is a GitHub
> platform constraint, not a choice (CORE_RULES §4).

## Status

**Phase 0 (current):** No workflows yet. The Gradle project doesn't exist yet.

## Planned workflows (Phase 1+)

Once the Gradle project is scaffolded (`ANDROIDDESIGN/APP/ani-design/`), the following workflows will be added:

### `build-apk.yml` (debug build)
- **Triggers**: push to `main`, PRs to `main`, manual dispatch.
- **Steps**:
  1. checkout
  2. `actions/setup-java@v4` (JDK 17)
  3. `android-actions/setup-android@v3` (Android SDK)
  4. `gradle/actions/setup-gradle@v4` (caching)
  5. `./gradlew :app:assembleDebug` (builds all ABIs: arm64-v8a + armeabi-v7a + x86_64)
  6. Detekt (with custom rule forbidding `material3.*` imports in feature code)
  7. "Verify ABIs" step — inspects every APK's `lib/` folder + fails on any unexpected `lib/<abi>/`
  8. Upload APK artifacts (`actions/upload-artifact@v4`)

### `build-release.yml` (release AAB — Phase 5+)
- **Triggers**: tags `v*.*.*`.
- **Steps**: same as debug + `./gradlew :app:bundleRelease` + signing (keystore from GitHub secret) + upload AAB.

### `lint.yml` (optional, may merge into build-apk.yml)
- Detekt + Android Lint + Koin resolvability test.

## Rules (CORE_RULES §8)

- **NEVER build locally.** GitHub Actions only.
- **NEVER install Android SDK / JDK in the local sandbox.**
- **NEVER run `./gradlew` locally.** Find compile errors by: reading carefully → sub-agent review → push to CI → read annotations → iterate.
- **ABIs: `arm64-v8a` + `armeabi-v7a` + `x86_64`** (user approved x86_64 for emulator).
- **App ID:** `com.testplaybyte.anidesign` (placeholder — confirm with user).
- **compileSdk=36, targetSdk=36, minSdk=26.**

## When this folder gets its first workflow

The workflow files will be created during **Phase 1** (project scaffolding), after the user answers the blocking open-questions (app name, package name, etc. — see `ANDROIDDESIGN/AGENT-CONTEXT/memory/open-questions.md`).
