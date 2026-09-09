#!/usr/bin/env bash
set -euo pipefail

# Resolve paths relative to this script so it works from any CWD. The
# Spring resource root is two levels up (pages/admin -> pages -> user-center).
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
res="$here/../../src/main/resources"

npm run build

# Clear only what THIS app owns, never the whole static root: the
# sibling `entrance` app keeps its own `assets/entrance/` subtree here.
rm -rf "$res/static/assets/admin"
rm -rf "$res/templates/admin"

mkdir -p "$res/static/assets" "$res/templates/admin"
cp -rp "$here/dist/assets/admin" "$res/static/assets/"
cp -p "$here/dist/index.html" "$res/templates/admin/index.html"
