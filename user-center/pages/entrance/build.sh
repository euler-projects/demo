#!/usr/bin/env bash
set -euo pipefail

# Resolve paths relative to this script so it works from any CWD. The
# Spring resource root is two levels up (pages/entrance -> pages ->
# user-center).
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
res="$here/../../src/main/resources"

npm run build

# Clear only what THIS app owns, never the whole static root: the
# sibling `admin` app keeps its own `assets/admin/` subtree here.
rm -rf "$res/static/assets/entrance"
rm -rf "$res/templates/entrance"

mkdir -p "$res/static/assets" "$res/templates/entrance"
cp -rp "$here/dist/assets/entrance" "$res/static/assets/"
cp -p "$here/dist/index.html" "$res/templates/entrance/index.html"
