#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -s 'gradle/wrapper/gradle-wrapper.jar' ]]; then
  bash ./prepare-gradle-wrapper.sh
fi
exec java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
