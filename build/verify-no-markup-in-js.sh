#!/usr/bin/env bash
# The JSPs render every element a page can show; the scripts only reveal one and set its text.
# This fails the build if any shipped script goes back to putting markup into the document.
# The reference/*.html prototypes are not packaged and are not checked.
#
# It is a plain text search, so it trips on these names in a comment too - describe the rule in
# words instead, the way the scripts under js/ do.
set -euo pipefail

WEBAPP="${1:?webapp dir}"

PATTERN='document[[:space:]]*\.[[:space:]]*write(ln)?[[:space:]]*\(|\.(inner|outer)HTML|insertAdjacentHTML|createElement|createContextualFragment'

if grep -rnE --include='*.js' --include='*.jsp' --include='*.jspf' "$PATTERN" "$WEBAPP"; then
  echo "[verify-no-markup-in-js] markup built in JavaScript above - render it in the JSP instead." >&2
  exit 1
fi
echo "[verify-no-markup-in-js] clean - no markup built in JavaScript in $WEBAPP"
