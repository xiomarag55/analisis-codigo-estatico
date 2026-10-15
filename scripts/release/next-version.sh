#!/usr/bin/env bash
# =============================================================================
# VERSIONADO SEMÁNTICO AUTOMÁTICO a partir de Conventional Commits.
#
# Lee los commits desde el último tag vX.Y.Z y decide el siguiente número:
#   BREAKING CHANGE o "tipo!:"  -> MAJOR  (1.4.2 -> 2.0.0)
#   feat:                       -> MINOR  (1.4.2 -> 1.5.0)
#   cualquier otro (fix, ...)   -> PATCH  (1.4.2 -> 1.4.3)
# Sin tags previos la primera versión es 1.0.0.
#
# Uso: scripts/release/next-version.sh        -> imprime p.ej. 1.5.0
# =============================================================================
set -euo pipefail

last_tag=$(git describe --tags --abbrev=0 --match 'v[0-9]*.[0-9]*.[0-9]*' 2>/dev/null || true)

if [[ -z "$last_tag" ]]; then
  echo "1.0.0"
  exit 0
fi

IFS=. read -r major minor patch <<<"${last_tag#v}"
commits=$(git log "${last_tag}..HEAD" --format='%s%n%b')

if [[ -z "$commits" ]]; then
  # Nada nuevo desde el último release: se reutiliza la versión
  echo "${major}.${minor}.${patch}"
elif grep -qE '^[a-z]+(\([^)]*\))?!:|BREAKING CHANGE' <<<"$commits"; then
  echo "$((major + 1)).0.0"
elif grep -qE '^feat(\([^)]*\))?:' <<<"$commits"; then
  echo "${major}.$((minor + 1)).0"
else
  echo "${major}.${minor}.$((patch + 1))"
fi
