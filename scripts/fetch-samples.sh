#!/usr/bin/env bash
# Downloads the Fountain sample scripts (fountain.io) that the golden tests
# compare against, into samples/. They're copyrighted, so they stay out of git.
# Each file is checked against scripts/samples.sha256. Safe to re-run.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
dest="$repo_root/samples"
mkdir -p "$dest"
cd "$dest"

while read -r sum name; do
  if [[ -f "$name" ]] && echo "$sum  $name" | sha256sum --check --status; then
    continue
  fi
  url="https://fountain.io/_downloads/$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1]))' "$name")"
  for attempt in 1 2 3 4; do
    if curl -sSfL --max-time 120 -o "$name" "$url" && echo "$sum  $name" | sha256sum --check --status; then
      break
    fi
    if [[ $attempt == 4 ]]; then
      echo "Could not download $name" >&2
      exit 1
    fi
    sleep $((attempt * 2))
  done
done < "$repo_root/scripts/samples.sha256"

echo "Samples ready in $dest"
