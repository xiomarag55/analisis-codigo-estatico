#!/usr/bin/env bash
# =============================================================================
# Valida que los commits de un Pull Request sigan Conventional Commits.
# Es lo que permite calcular la versión automáticamente (next-version.sh).
#
# Uso: scripts/release/check-commits.sh <base-sha> <head-sha>
# =============================================================================
set -euo pipefail

base="$1"
head="$2"
pattern='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9._-]+\))?!?: .{3,}'

invalid=0
while IFS= read -r line; do
  sha="${line%% *}"
  subject="${line#* }"
  [[ "$subject" =~ ^Merge\  ]] && continue
  if [[ ! "$subject" =~ $pattern ]]; then
    echo "❌ ${sha}: '${subject}'"
    invalid=$((invalid + 1))
  else
    echo "✅ ${sha}: '${subject}'"
  fi
done < <(git log --format='%h %s' "${base}..${head}")

if [[ "$invalid" -gt 0 ]]; then
  echo
  echo "Hay $invalid commit(s) que no siguen Conventional Commits."
  echo "Formato: <tipo>(<ámbito opcional>): <descripción>   p.ej. 'feat(tareas): filtrar por prioridad'"
  exit 1
fi
