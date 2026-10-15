#!/usr/bin/env bash
# REZIM Gradle bootstrap. Official Gradle wrapper JAR from Gradle's tagged GitHub repository.
# SHA-256 pinned to Gradle's official 8.13 release checksum at https://gradle.org/release-checksums/
set -euo pipefail
cd "$(dirname "$0")"
TARGET='gradle/wrapper/gradle-wrapper.jar'
URL='https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar'
SHA256='81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f'
for command in curl sha256sum; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Chybi prikaz: $command" >&2
    exit 1
  fi
done
if [[ -s "$TARGET" ]]; then
  if printf '%s  %s\n' "$SHA256" "$TARGET" | sha256sum --check --status; then
    echo 'Gradle wrapper JAR existuje a kontrolni soucet souhlasi.'
    exit 0
  fi
  echo 'Existujici Gradle wrapper JAR nema ocekavany kontrolni soucet.' >&2
  exit 1
fi
mkdir -p gradle/wrapper
TMP="${TARGET}.download"
trap 'rm -f "$TMP"' EXIT
printf 'Stahuji oficialni Gradle wrapper 8.13 z GitHubu...\n'
curl --fail --location --retry 3 --silent --show-error "$URL" --output "$TMP"
printf '%s  %s\n' "$SHA256" "$TMP" | sha256sum --check
mv "$TMP" "$TARGET"
echo 'Hotovo. Nyni v Android Studiu spustte Gradle Sync.'
