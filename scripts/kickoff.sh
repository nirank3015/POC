#!/usr/bin/env bash
# =============================================================================
# Auto Dev Pipeline — Kickoff Script
# Usage:
#   ./scripts/kickoff.sh --md /path/to/requirement.md --type feature
#   ./scripts/kickoff.sh --md /path/to/bugfix-login.md --type bugfix
#   ./scripts/kickoff.sh --md /path/to/requirement.md --type feature --dry-run
#
# Prerequisites:
#   - GITHUB_TOKEN env var set (org-scoped PAT)
#   - GITHUB_REPO env var set (e.g. "your-org/your-repo")
#   - curl and node available on PATH
# =============================================================================

set -euo pipefail

# ── Load .env from repo root (if present) ────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env"
if [[ -f "$ENV_FILE" ]]; then
  set -o allexport
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +o allexport
fi

# ── Colour helpers ────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ── GitHub API helpers (curl + node, no gh CLI or jq required) ───────────────
GH_API="https://api.github.com"

gh_get() {
  local path="$1"
  curl -sf \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "${GH_API}/${path}"
}

gh_post() {
  local path="$1" body="$2"
  curl -sf \
    -X POST \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    -H "Content-Type: application/json" \
    -d "$body" \
    "${GH_API}/${path}"
}

gh_put() {
  local path="$1" body="$2"
  curl -sf \
    -X PUT \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    -H "Content-Type: application/json" \
    -d "$body" \
    "${GH_API}/${path}"
}

# Extract a value from JSON using node (no jq needed)
# Usage: jq_val <json> <js-expression-using-d>  e.g. jq_val "$json" 'd.object.sha'
jq_val() {
  local json="$1" expr="$2"
  node -e "const d=JSON.parse(process.argv[1]); const v=${expr}; console.log(v==null?'':v);" "$json"
}

# ── Defaults ──────────────────────────────────────────────────────────────────
MD_FILE=""
BRANCH_TYPE="feature"
BASE_BRANCH="main"
DRY_RUN=false

# ── Arg parsing ───────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case $1 in
    --md)       MD_FILE="$2";     shift 2 ;;
    --type)     BRANCH_TYPE="$2"; shift 2 ;;
    --base)     BASE_BRANCH="$2"; shift 2 ;;
    --dry-run)  DRY_RUN=true;     shift   ;;
    *)          error "Unknown argument: $1" ;;
  esac
done

# ── Validation ────────────────────────────────────────────────────────────────
[[ -z "$MD_FILE" ]]   && error "--md <path> is required"
[[ ! -f "$MD_FILE" ]] && error "MD file not found: $MD_FILE"
if [[ "$DRY_RUN" == false ]]; then
  [[ -z "${GITHUB_TOKEN:-}" ]] && error "GITHUB_TOKEN env var is not set"
  [[ -z "${GITHUB_REPO:-}" ]]  && error "GITHUB_REPO env var is not set (e.g. org/repo)"
else
  GITHUB_REPO="${GITHUB_REPO:-dry-run/repo}"
  warn "Dry-run mode — no GitHub API calls will be made"
fi

BRANCH_TYPE=$(echo "$BRANCH_TYPE" | tr '[:upper:]' '[:lower:]')
[[ "$BRANCH_TYPE" != "feature" && "$BRANCH_TYPE" != "bugfix" ]] \
  && error "--type must be 'feature' or 'bugfix'"

# ── Derive ticket ID from filename ────────────────────────────────────────────
FILENAME=$(basename "$MD_FILE" .md)
TICKET_ID=$(echo "$FILENAME" | tr ' ' '-' | tr '[:upper:]' '[:lower:]')
BRANCH_NAME="${BRANCH_TYPE}/${TICKET_ID}"

info "MD file   : $MD_FILE"
info "Ticket ID : $TICKET_ID"
info "Branch    : $BRANCH_NAME"
info "Repo      : $GITHUB_REPO"
info "Base      : $BASE_BRANCH"
echo ""

# ── Step 1: Create branch via GitHub API ─────────────────────────────────────
info "Step 1/4 — Creating branch '$BRANCH_NAME' from '$BASE_BRANCH'..."

if [[ "$DRY_RUN" == true ]]; then
  warn "[dry-run] Would create branch '$BRANCH_NAME' from '$BASE_BRANCH' in $GITHUB_REPO"
else
  BASE_JSON=$(gh_get "repos/${GITHUB_REPO}/git/ref/heads/${BASE_BRANCH}") \
    || error "Could not fetch SHA for base branch '$BASE_BRANCH'"
  BASE_SHA=$(jq_val "$BASE_JSON" 'd.object.sha')
  [[ -z "$BASE_SHA" ]] && error "Empty SHA returned for base branch '$BASE_BRANCH'"

  # Exact-match check — GitHub prefix-matches /git/ref/ so verify the returned ref name
  EXISTING_JSON=$(gh_get "repos/${GITHUB_REPO}/git/ref/heads/${BRANCH_NAME}" 2>/dev/null || echo "{}")
  EXISTING_REF=$(jq_val "$EXISTING_JSON" 'd.ref||""')

  if [[ "$EXISTING_REF" == "refs/heads/${BRANCH_NAME}" ]]; then
    warn "Branch '$BRANCH_NAME' already exists — skipping creation"
  else
    gh_post "repos/${GITHUB_REPO}/git/refs" \
      "{\"ref\":\"refs/heads/${BRANCH_NAME}\",\"sha\":\"${BASE_SHA}\"}" > /dev/null
    success "Branch created: $BRANCH_NAME"
  fi
fi

# ── Step 2: Commit the MD file into the repo ──────────────────────────────────
info "Step 2/4 — Uploading requirement MD to repo..."

DEST_PATH="requirements/${TICKET_ID}.md"

if [[ "$DRY_RUN" == true ]]; then
  warn "[dry-run] Would upload '$MD_FILE' to '$DEST_PATH' on branch '$BRANCH_NAME'"
else
  MD_CONTENT_B64=$(base64 < "$MD_FILE" | tr -d '\n')
  EXISTING_FILE_JSON=$(gh_get \
    "repos/${GITHUB_REPO}/contents/${DEST_PATH}?ref=${BRANCH_NAME}" 2>/dev/null || echo "{}")
  EXISTING_FILE_SHA=$(jq_val "$EXISTING_FILE_JSON" 'd.sha||""')

  if [[ -n "$EXISTING_FILE_SHA" ]]; then
    gh_put "repos/${GITHUB_REPO}/contents/${DEST_PATH}" \
      "{\"message\":\"chore: update requirement for ${TICKET_ID}\",\"content\":\"${MD_CONTENT_B64}\",\"branch\":\"${BRANCH_NAME}\",\"sha\":\"${EXISTING_FILE_SHA}\"}" > /dev/null
  else
    gh_put "repos/${GITHUB_REPO}/contents/${DEST_PATH}" \
      "{\"message\":\"chore: add requirement for ${TICKET_ID}\",\"content\":\"${MD_CONTENT_B64}\",\"branch\":\"${BRANCH_NAME}\"}" > /dev/null
  fi
  success "Requirement uploaded to: $DEST_PATH"
fi

# ── Step 3: Trigger design-generation workflow via repository_dispatch ─────────
info "Step 3/4 — Triggering design-generation workflow..."

if [[ "$DRY_RUN" == true ]]; then
  warn "[dry-run] Would dispatch 'generate-design' event with payload:"
  warn "  ticket_id=$TICKET_ID  branch=$BRANCH_NAME  md_path=$DEST_PATH  type=$BRANCH_TYPE"
else
  gh_post "repos/${GITHUB_REPO}/dispatches" \
    "{\"event_type\":\"generate-design\",\"client_payload\":{\"ticket_id\":\"${TICKET_ID}\",\"branch_name\":\"${BRANCH_NAME}\",\"md_path\":\"${DEST_PATH}\",\"branch_type\":\"${BRANCH_TYPE}\"}}" > /dev/null
  success "Design generation workflow triggered"
fi

# ── Step 4: Summary ───────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN} Pipeline kicked off successfully!${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  Branch     : ${CYAN}${BRANCH_NAME}${NC}"
echo -e "  Requirement: ${CYAN}${DEST_PATH}${NC}"
echo -e "  Next step  : ${YELLOW}Watch for a Design Doc PR to open in your repo${NC}"
echo -e "  Approve    : Comment ${CYAN}/approve-design${NC} on that PR to proceed to code gen"
echo -e "  Revise     : Comment ${CYAN}/revise-design: <your feedback>${NC} to iterate"
echo ""
