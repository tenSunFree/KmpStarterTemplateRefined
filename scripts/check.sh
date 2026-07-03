#!/bin/sh
# scripts/check.sh
#
# Local CI checks — mirrors what runs in GitHub Actions
# ("Lint, Test & Build Android Debug").
#
# Usage: bash scripts/check.sh

set -e

cd "$(git rev-parse --show-toplevel)"

echo ""
echo "==> [1/3] Running Android Lint..."
./gradlew :androidApp:lint --stacktrace

echo ""
echo "==> [2/3] Running unit tests..."
./gradlew :androidApp:testDebugUnitTest --stacktrace

echo ""
echo "==> [3/3] Building debug APK..."
./gradlew :androidApp:assembleDebug --stacktrace

echo ""
echo "All local checks passed."
