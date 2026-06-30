# Auto Dev Pipeline — Workflow Documentation

## Overview

An end-to-end automation pipeline that takes a plain Markdown requirement file and produces production-ready Spring Boot code via AI, with human review gates at every stage before merging to `main`.

**Technologies:** GitHub Actions · GitHub Models API (GPT-4o, Claude Opus 4.7) · Python · Bash · curl

---

## Pipeline Diagram

```
Developer (local)
      │
      │  bash ./scripts/kickoff.sh --md requirement.md --type feature
      ▼
┌─────────────────────────────────────────────────────────────┐
│  STEP 1 · Kickoff                                           │
│  • Creates feature branch                                   │
│  • Uploads requirement MD to repo                           │
│  • Dispatches generate-design event                         │
└──────────────────────────┬──────────────────────────────────┘
                           │ repository_dispatch
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  WORKFLOW 1 · Generate Design Doc                           │
│  • Reads requirement MD                                     │
│  • Calls GPT-4o → generates technical design doc            │
│  • Commits docs/design/<ticket>.md to feature branch        │
│  • Opens 🎨 Design Review PR                                │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
                  ┌────────────────┐
                  │ Human reviews  │
                  │  design doc    │
                  └───────┬────────┘
                          │
            ┌─────────────┴──────────────┐
            │                            │
   /revise-design                  /approve-design
            │                            │
            ▼                            ▼
┌──────────────────────┐   ┌─────────────────────────────────┐
│ WORKFLOW 2 · Revise  │   │  WORKFLOW 3 · Generate Code     │
│ • GPT-4o revises doc │   │  • Reads design doc + source    │
│ • Commits update     │   │  • Calls GPT-4o → generates     │
│ • Loop back          │   │    Spring Boot Java files        │
└──────────────────────┘   │  • Commits generated code       │
                           │  • Opens 🚀 Code Review PR      │
                           │  • Requests Copilot review      │
                           └──────────────┬──────────────────┘
                                          │
                                          ▼
                                 ┌────────────────┐
                                 │ Copilot reviews│
                                 │ Human reviews  │
                                 └───────┬────────┘
                                         │
                           ┌─────────────┴──────────────┐
                           │                            │
                   /revise-code               Human approves PR
                           │                            │
                           ▼                            ▼
               ┌──────────────────────┐   ┌────────────────────────┐
               │ WORKFLOW 4 · Revise  │   │  Merge to main ✅      │
               │ • GPT-4o revises     │   │  Branch auto-deleted   │
               │   changed files only │   └────────────────────────┘
               │ • Commits update     │
               │ • Loop back          │
               └──────────────────────┘
```

---

## One-Time Setup

| Step | Action | Where |
|------|--------|-------|
| 1 | Add `GH_PAT` repository secret | `Settings → Secrets → Actions` |
| 2 | Run **"0 · Setup Branch Protection"** workflow | `Actions` tab → Run workflow |
| 3 | Enable Copilot code review | `Settings → Copilot` |
| 4 | Enable auto-delete of merged branches | `Settings → General → Pull Requests` |

---

## Step-by-Step Flow

### Step 1 — Kickoff (Local)

Run from your terminal:

```bash
GITHUB_TOKEN="<token>" GITHUB_REPO="<org>/<repo>" \
bash ./scripts/kickoff.sh \
  --md ./product-crud/docs/enhancements/01-search-and-filtering.md \
  --type feature
```

| What happens | Detail |
|---|---|
| Branch created | `feature/01-search-and-filtering` from `main` |
| Requirement uploaded | `requirements/01-search-and-filtering.md` on branch |
| Workflow triggered | `generate-design` dispatch event fired |

---

### Step 2 — Design Generation (Automated)

**Workflow:** `1 · Generate Design Doc`  
**Trigger:** `repository_dispatch → generate-design`

1. Checks out the feature branch
2. Reads the requirement MD + `.github/copilot-instructions.md`
3. Calls **GPT-4o** to generate a structured technical design document with sections: Overview, Scope, API Design, Data Model, Service Layer, Repository Layer, Security, Error Handling, Testing Strategy, Open Questions
4. Commits design doc → `docs/design/<ticket-id>.md`
5. Opens PR: **🎨 [Design Review] \<ticket-id\>**

---

### Step 3 — Design Review (Human Gate)

**Where:** Design Review PR on GitHub

| Comment | Action |
|---|---|
| `/approve-design` | Proceeds to code generation |
| `/revise-design: <feedback>` | GPT-4o revises the design doc and commits update |

> Revision loop can repeat as many times as needed before approving.

---

### Step 4 — Code Generation (Automated)

**Workflow:** `3 · Generate Code`  
**Trigger:** `repository_dispatch → generate-code`

1. Checks out the feature branch
2. Collects context:
   - Design doc (from branch or GitHub API)
   - `copilot-instructions.md` (coding conventions)
   - Existing Java source files (Entities, Repositories, Services, Controllers)
   - `pom.xml` (dependency awareness)
3. Calls **Claude Opus 4.7** to generate production-ready Spring Boot code
4. Parses response and writes each file to its correct path
5. Commits all generated files to the feature branch
6. Closes the Design Review PR
7. Opens PR: **🚀 [Code Review] \<ticket-id\>**
8. Requests **GitHub Copilot** as a reviewer

---

### Step 5 — Code Review (AI + Human Gate)

**Where:** Code Review PR on GitHub

**Copilot review:** Automatically triggered — leaves inline comments on the generated code.

| Comment | Action |
|---|---|
| `/revise-code: <feedback>` | GPT-4o revises only the affected files and commits |
| Approve PR | Unlocks merge (1 approval required by branch protection) |

> Revision loop can repeat as many times as needed before approving.

---

### Step 6 — Merge & Cleanup

1. Once **1 human approval** is given, the **Merge pull request** button unlocks
2. Reviewer merges the PR into `main`
3. Feature branch is automatically deleted

---

## Workflows Reference

| # | Workflow File | Trigger | Purpose |
|---|---|---|---|
| 0 | `setup-branch-protection.yml` | Manual | Enforce 1 approval required on `main` |
| 1 | `generate-design.yml` | `repository_dispatch` | Generate technical design doc via GPT-4o |
| 2 | `design-review-handler.yml` | `issue_comment` | Handle `/approve-design` and `/revise-design` |
| 3 | `generate-code.yml` | `repository_dispatch` | Generate Spring Boot code via GPT-4o |
| 4 | `code-review-handler.yml` | `issue_comment` | Handle `/revise-code` |
| 5 | `delete-merged-branch.yml` | `pull_request` closed | Delete feature/bugfix branch after merge |

---

## Scripts Reference

| Script | Called By | Purpose |
|---|---|---|
| `kickoff.sh` | Developer (local) | Start the pipeline for a requirement |
| `generate_design.py` | Workflow 1 | Call GPT-4o to generate design doc |
| `revise_design.py` | Workflow 2 | Call GPT-4o to revise design doc |
| `generate_code.py` | Workflow 3 | Call GPT-4o to generate Java code |
| `revise_code.py` | Workflow 4 | Call GPT-4o to revise generated code |

---

## Repository Structure

```
.
├── .github/
│   ├── copilot-instructions.md          # Code conventions fed to GPT-4o
│   └── workflows/
│       ├── setup-branch-protection.yml  # Workflow 0
│       ├── generate-design.yml          # Workflow 1
│       ├── design-review-handler.yml    # Workflow 2
│       ├── generate-code.yml            # Workflow 3
│       ├── code-review-handler.yml      # Workflow 4
│       └── delete-merged-branch.yml     # Workflow 5
├── scripts/
│   ├── kickoff.sh                       # Local pipeline kickoff
│   ├── generate_design.py               # GPT-4o design generation
│   ├── revise_design.py                 # GPT-4o design revision
│   ├── generate_code.py                 # GPT-4o code generation
│   └── revise_code.py                   # GPT-4o code revision
├── requirements/
│   └── <ticket-id>.md                   # Uploaded requirement files
└── docs/
    ├── design/
    │   └── <ticket-id>.md               # AI-generated design docs
    ├── automation-workflow.md           # This document
    └── automation-workflow.txt          # Plain text version
```

---

## AI Model Details

| Property | Value |
|---|---|
| Provider | GitHub Models API |
| Model | GPT-4o for design/revision; Claude Opus 4.7 for code generation |
| Endpoint | `https://models.inference.ai.azure.com/chat/completions` |
| Auth | `GH_PAT` (no separate API key needed) |
| Max tokens | 8000 per call |
