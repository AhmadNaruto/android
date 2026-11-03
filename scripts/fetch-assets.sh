#!/bin/sh
# Downloads the data files the app starts with (it refreshes them itself later). They are not stored in the repository:
#   assets/fmhy-video.md, assets/fmhy-nonenglish.md   FMHY's site lists (https://github.com/fmhy/edit)
#   assets/blocklist.txt                              HaGeZi's domain blocklists (https://github.com/hagezi/dns-blocklists)
# Files that already exist are kept; run with -f to download them again.
set -e
cd "$(dirname "$0")/.."
FMHY=https://raw.githubusercontent.com/fmhy/edit/main/docs
HAGEZI=https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard
FORCE=0; [ "$1" = -f ] && FORCE=1
need() { [ "$FORCE" = 1 ] || [ ! -s "$1" ]; }   # true when the file has to be downloaded
mkdir -p assets
for f in video non-english; do
  out=assets/fmhy-$( [ $f = video ] && echo video || echo nonenglish ).md
  if need "$out"; then curl -fsSL --retry 3 "$FMHY/$f.md" -o "$out.tmp" && mv "$out.tmp" "$out" && echo "fetched $out"; fi
done
if need assets/blocklist.txt; then
  { curl -fsSL --retry 3 "$HAGEZI/pro.mini-onlydomains.txt"; curl -fsSL --retry 3 "$HAGEZI/popupads-onlydomains.txt"; } \
    | grep -v '^#' | grep -v '^$' | sort -u > assets/blocklist.txt.tmp
  mv assets/blocklist.txt.tmp assets/blocklist.txt && echo "fetched assets/blocklist.txt"
fi
