#!/usr/bin/env bash
# Zip the files a reviewer needs: sources, poms, README, reference pages and build checks.
# Uses git's file list, so target/, logs and db.properties (real credentials) never go in,
# and leaves out IDE and Maven-wrapper files (.idea, .mvn, mvnw, mvnw.cmd).
# Usage: build/zip-for-review.sh [output.zip]   (default: ../donation-review.zip)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$ROOT/../donation-review.zip}"
OUT="$(cd "$(dirname "$OUT")" && pwd)/$(basename "$OUT")"

cd "$ROOT"
rm -f "$OUT"
git ls-files --cached --others --exclude-standard \
  | grep -vE '^(\.idea/|\.mvn/|mvnw(\.cmd)?$|\.gitignore$)' \
  | grep -vE '(^|/)db\.properties$|\.(zip|log|war|jar)$' \
  | while read -r f; do [ -f "$f" ] && printf '%s\n' "$f"; done \
  | zip -q "$OUT" -@

echo "[zip-for-review] $(unzip -Z1 "$OUT" | wc -l) files -> $OUT"
