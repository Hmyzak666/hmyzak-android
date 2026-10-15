#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
file=app/build.gradle.kts
[ -f "$file" ] || { echo "Nenalezen app/build.gradle.kts"; exit 1; }
sed -i 's/compose-bom:2026\.09\.00/compose-bom:2025.08.00/g; s/activity-compose:1\.12\.1/activity-compose:1.11.0/g' "$file"
grep -q 'compose-bom:2025.08.00' "$file"
grep -q 'activity-compose:1.11.0' "$file"
echo "Verze knihoven opraveny. Muzete spustit Gradle Sync."
