#!/usr/bin/env bash
# css/report.css is the reference report's stylesheet, unchanged: the contents of the <style>
# element in reference/report.html, byte for byte. This fails the build if either one drifts - to
# restyle the report, change the reference first and copy it across.
set -euo pipefail

REFERENCE="${1:?reference/report.html}"
CSS="${2:?css/report.css}"

extracted="$(mktemp)"
trap 'rm -f "$extracted"' EXIT
# the lines strictly between <style> and </style>
sed -n '/^<style>$/,/^<\/style>$/{/^<\/\{0,1\}style>$/d;p}' "$REFERENCE" > "$extracted"

if [ ! -s "$extracted" ]; then
  echo "[verify-report-css] no <style> block found in $REFERENCE" >&2
  exit 1
fi
if ! diff -u "$extracted" "$CSS"; then
  echo "[verify-report-css] $CSS differs from the <style> block in $REFERENCE (diff above)." >&2
  exit 1
fi
echo "[verify-report-css] clean - $CSS matches $REFERENCE"
