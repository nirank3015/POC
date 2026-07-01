---
mode: agent
description: Kick off the Auto Dev Pipeline for a requirement markdown file
---

You are triggering the **Auto Dev Pipeline** for this project.

## What this does
Runs `scripts/kickoff.sh` with whatever terminal is active by selecting an available shell command:
1. Creates a feature/bugfix branch on GitHub
2. Uploads the requirement markdown to the repo
3. Triggers the design-generation workflow
4. Opens a Design Doc PR for review

## Instructions

Ask the user for these two things if not already provided:
1. **Path to the requirement markdown file** (e.g. `./product-crud/docs/enhancements/02-pagination-and-sorting.md`)
2. **Branch type**: `feature` or `bugfix` (default: `feature`)

Then run kickoff with a terminal-aware fallback:

```bash
# Preferred (works in bash/zsh and in terminals where bash is on PATH)
bash ./scripts/kickoff.sh --md <md-file-path> --type <feature|bugfix>

# Windows fallback when bash is not on PATH
"C:\Program Files\Git\bin\bash.exe" ./scripts/kickoff.sh --md <md-file-path> --type <feature|bugfix>
```

Credentials (`GITHUB_TOKEN`, `GITHUB_REPO`) are loaded automatically from the `.env` file in the repo root — no need to pass them manually.

## Example

```bash
bash ./scripts/kickoff.sh --md ./product-crud/docs/enhancements/02-pagination-and-sorting.md --type feature
```

## After running
- Watch for a **Design Doc PR** to open in the GitHub repo
- Comment `/approve-design` on that PR to proceed to code generation
- Comment `/revise-design: <feedback>` to iterate on the design

## Troubleshooting
- **401 Bad credentials** — update `GITHUB_TOKEN` in the `.env` file at the repo root
- **Branch already exists** — the script skips creation and continues safely
- **MD file not found** — check the path is relative to the repo root
