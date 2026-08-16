# REFERENCES — External Reference Material

> Read-only external material the agent consults when porting or comparing.
> Do NOT modify vendored repos in-place — fork or patch in the app code.

## Contents

### `core-rules-reference.md`
The CORE_RULES.md from the prior **ANI-KUTA** project. Kept as a structural inspiration for the refined `AGENT-CONTEXT/rules/CORE_RULES.md`. It is NOT authoritative for this project — anything ANI-KUTA-specific (MPV player, SQLDelight, `app.confused.anikuta`, ARM-only ABIs, DASHBOARD zone) is explicitly NOT carried over unless restated in our CORE_RULES.

### `cline/` (local sandbox only — NOT committed to this repo)
A shallow clone of [Cline](https://github.com/cline/cline) (Apache 2.0), kept locally at `/home/z/my-project/android-project/references/cline/` for reference while porting the agent architecture to Kotlin. It is NOT vendored into this GitHub repo (to keep the repo lean). The distilled analysis lives in `../research/R-1-cline-agent.md` + `../AGENT-CONTEXT/knowledge/cline-agent.md`.

If we need Cline vendored as a git submodule in the future (so it travels with the repo), see `AGENT-CONTEXT/memory/open-questions.md` Q-029 (deferred).

## Adding a new reference

1. Place it under `REFERENCES/<name>/` (vendored repo) or `REFERENCES/<name>.md` (single doc).
2. Add a section here explaining what it is + why we reference it + its license.
3. If it's a vendored repo: prefer `git submodule add` so updates are explicit, OR a shallow clone if submodule complexity isn't warranted.
4. Never modify vendored code in-place. If we need a patched version, fork it + add the fork as the submodule.
