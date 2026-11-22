#!/usr/bin/env bash
# Integrate only the Android build and its conformance fixtures; never import
# upstream workflows into the branch entrusted with signing and publishing.
set -euo pipefail
old=$(cat .github/channel/upstream)
git fetch --no-tags https://github.com/tiann/hapi.git refs/heads/main
new=$(git log -1 --format=%H FETCH_HEAD -- android shared/fixtures)
git merge-base --is-ancestor "$old" "$new" || { echo 'Upstream history diverged; inspect before syncing.' >&2; exit 1; }
if [[ $old != "$new" ]]; then
    patch=$(mktemp); trap 'rm -f "$patch"' EXIT
    git diff --binary "$old" "$new" -- android shared/fixtures > "$patch"
    if [[ -s $patch ]]; then git apply --3way --index "$patch"; fi
    echo "$new" > .github/channel/upstream
    git add .github/channel/upstream
    git -c user.name='github-actions[bot]' -c user.email='41898282+github-actions[bot]@users.noreply.github.com' commit -m "Sync HAPI Android through ${new:0:12}"
    git push origin HEAD:refs/heads/android-channel
fi
