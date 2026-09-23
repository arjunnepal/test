#!/usr/bin/env bash
# Precompile the JSPs with Jasper so a scriptlet error fails the build.
# Usage: build/verify-jsps.sh <webapp-src-dir> <packaged-WEB-INF-dir> <work-dir>
# The packaged WEB-INF (web/target/ROOT/WEB-INF) supplies the module jars the JSPs import.
set -euo pipefail

WEBAPP="${1:?webapp dir}"
WEBINF="${2:?packaged WEB-INF dir}"
WORKDIR="${3:?scratch output dir}"

CATALINA_HOME="${CATALINA_HOME:-${TOMCAT_HOME:-}}"
if [ -z "$CATALINA_HOME" ] && command -v catalina.sh >/dev/null 2>&1; then
  CATALINA_HOME="$(cd "$(dirname "$(command -v catalina.sh)")/.." && pwd)"
fi

if [ -z "$CATALINA_HOME" ] || [ ! -d "$CATALINA_HOME/lib" ]; then
  echo "[verify-jsps] CATALINA_HOME not set or has no lib/ - skipping JSP precompile."
  echo "[verify-jsps] Set CATALINA_HOME to a Tomcat install to enable this check."
  exit 0
fi

REPO="${MAVEN_REPO:-$HOME/.m2/repository}"
ANT_JAR="$(find "$REPO/org/apache/ant/ant" -name 'ant-*.jar' 2>/dev/null | sort | tail -1 || true)"
if [ -z "$ANT_JAR" ]; then
  echo "[verify-jsps] ant jar not found under $REPO - skipping JSP precompile."
  exit 0
fi

if [ ! -d "$WEBINF/lib" ]; then
  echo "[verify-jsps] $WEBINF/lib not found - run the package phase first." >&2
  exit 1
fi

CP="$CATALINA_HOME/lib/*:$CATALINA_HOME/bin/*:$WEBINF/classes:$WEBINF/lib/*:$ANT_JAR"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"

echo "[verify-jsps] Jasper precompile via $CATALINA_HOME"

# JspC prints compile failures but still exits 0, so its own status proves nothing -
# scan the output as well, or a broken JSP sails through the build.
LOG="$WORKDIR/jspc.log"
set +e
java -cp "$CP" org.apache.jasper.JspC \
  -uriroot "$WEBAPP" \
  -d "$WORKDIR" \
  -compile \
  -failFast 2>&1 | tee "$LOG"
STATUS=${PIPESTATUS[0]}
set -e

if [ "$STATUS" -ne 0 ] || grep -qE "An error occurred at line|JasperException" "$LOG"; then
  echo "[verify-jsps] JSP compilation failed - see the errors above." >&2
  exit 1
fi
echo "[verify-jsps] all JSPs compiled cleanly"
