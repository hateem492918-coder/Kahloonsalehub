# Copilot instructions for Kahloonsalehub

## Repository status

- This repository is currently a minimal bootstrap: the root contains only `README.md`, `.gitignore`, and licensing metadata. There are no application sources, Gradle modules, package manifests, or CI files checked in yet.
- The `.gitignore` strongly suggests an Android/Gradle project, but the actual Android project files are not present in this snapshot. Treat this as a pre-project or intentionally empty repo until confirmed by future commits.

## Build, test, and lint commands

- There are no verified build, test, or lint commands in the current repository state.
- Do not assume a framework or runtime before project files are present.
- If this repository is later initialized as an Android app, the standard commands to look for are:
  - `./gradlew assembleDebug`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lintDebug`
  - Single test: `./gradlew testDebugUnitTest --tests 'com.example.MyTest'` (or the equivalent package/class path in the app)
- Until the Android project exists, validation should be limited to checking whether the expected project files and Gradle wrapper are present before running any command.

## High-level architecture

- The repo appears to be intended as an Android sales app for Lahore, Pakistan, based on the project name and the Android-specific entries in `.gitignore`.
- The current codebase does not yet contain a meaningful app structure; the "big picture" is therefore uncertain and should be inferred from the eventual Android project layout rather than from existing sources.
- When the real project is added, prefer Android/Gradle conventions over ad hoc project scaffolding: app modules, resource folders, manifests, and Kotlin/Java source layout should match the chosen Android structure.
- Avoid inventing a server, web app, or multi-package architecture unless future files confirm that is the actual stack.

## Key conventions

- Preserve the current minimal repository state; do not add frameworks or generated files without evidence that the repo is intended to use them.
- When Android project files are introduced, keep the structure aligned with standard Android Gradle conventions rather than mixing app, library, and web conventions in the same repo.
- Prefer small, focused changes. This repo is too early-stage to justify broad refactors or framework migrations.
- Use the same naming and filesystem conventions as the final app stack; if this becomes an Android app, keep package names, module names, and resource organization consistent with Android expectations.

## Notes for future Copilot sessions

- Be conservative about assumptions: the repository is currently incomplete, and the correct architecture should be inferred from the files that appear, rather than from the README alone.
- If a new project skeleton is added (for example an Android Gradle app), update these instructions to reflect the actual module layout, package names, and test/lint commands that are present in that project.
