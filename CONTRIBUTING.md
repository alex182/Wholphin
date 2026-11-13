# Contributing to Wholphin

We appreciate your interest in contributing to Wholphin!

## Code of Conduct

We follow Jellyfin's example by adhering to their [Code of Conduct](https://jellyfin.org/docs/general/community-standards/#code-of-conduct).

## How to contribute

You can contribute in several ways:
* File bug reports
* Request new features
* Propose code changes via pull request
* Submit translations to https://translate.codeberg.org/engage/wholphin/

In all cases, be sure to fully explain the issue.

### Code change proposals

You acknowledge that, if applicable, submitting and subsequent acceptance of any pull request you, the code contributor of the pull request, agree and acknowledge that the code will be licensed under [GPLv2](LICENSE).

Please be thoughtful when contributing code and consider the long term maintenance/support aspect for any new features.

See the [developer's guide](./DEVELOPMENT.md) for more information.


#### Policy for AI or LLMs

Wholphin has adopted the [Jellyfin LLM/"AI" Development Policy](https://jellyfin.org/docs/general/contributing/llm-policies/). AI/LLM assisted code contributions are permitted under the policy, but the author must undertand and be able to explain the code changes in their own words.

At a minimum, you must disclose the usage any of AI/LLM assistance and you must write the pull request description _without_ AI assistance.

## Accessibility fork

This repository is a fork focused on visual accessibility. See [ACCESSIBILITY_ROADMAP.md](ACCESSIBILITY_ROADMAP.md) for the tickets and their status.

- `main` tracks upstream (`damontecres/Wholphin`). All fork work is on `accessible`. Bring upstream changes in by merging `main` into `accessible`.
- Build, test and format only inside the dev container: `docker/dev.sh ./gradlew assembleDefaultDebug testDefaultDebugUnitTest` and `docker/dev.sh ktlint -F "app/src/**/*.kt"`.
- To look at changes on an Android TV emulator: `docker/emu.sh up`, then `install`, `login`, `shot <name>` (needs `/dev/kvm`).
- Every user-facing change needs a setting under Settings > Accessibility whose default is upstream behaviour. New settings go in `app/src/main/proto/WholphinAccessibility.proto` and `preferences/AccessibilityPreference.kt`; new strings go in `app/src/main/res/values/a11y_strings.xml`.
- Keep edits to upstream files small. Prefer one app-wide hook (as the focus outline, zoom and reduce motion do) over changing many screens.
