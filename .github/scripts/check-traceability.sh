#!/usr/bin/env bash
# Change traceability check (constitution, Article VI).
#
#   1. A changed rule test (*RulesProperties.java) needs a changed spec.md:
#      tests change because rules change, never on their own.
#   2. A changed spec.md needs a new changelog line that names a change request
#      (CR-<number>).
#   3. A changed api/openapi.yaml needs a changed spec.md (so it is traced to a
#      change request) and a new info.version.
#
# Usage: check-traceability.sh <base-ref>     e.g. origin/main
set -euo pipefail

base="${1:?usage: check-traceability.sh <base-ref>}"
changed=$(git diff --name-only "${base}...HEAD")
failed=0

error() {
  echo "::error::$*"
  failed=1
}

changed_matching() {
  printf '%s\n' "$changed" | grep -E "$1" || true
}

# Reads info.version from an OpenAPI YAML document on stdin.
info_version() {
  awk '
    /^info:/          { in_info = 1; next }
    /^[^[:space:]#]/  { in_info = 0 }
    in_info && /^[[:space:]]+version:/ {
      sub(/^[[:space:]]+version:[[:space:]]*/, "")
      gsub(/["\047]/, "")
      sub(/[[:space:]]+#.*$/, "")
      print
      exit
    }'
}

rule_tests=$(changed_matching '^src/test/java/.*RulesProperties\.java$')
specs=$(changed_matching '^specs/[^/]+/spec\.md$')
contract=$(changed_matching '^api/openapi\.yaml$')

# 1. Rule tests only change together with a rule.
if [ -n "$rule_tests" ] && [ -z "$specs" ]; then
  error "Rule tests changed without a change to any specs/*/spec.md: $(echo "$rule_tests" | tr '\n' ' ')"
  error "Change the rule first (with a CR changelog entry), then the test. If the spec was right, fix the code instead of the test."
fi

# 2. Every changed spec.md gets a changelog line naming a change request.
for spec in $specs; do
  if [ ! -f "$spec" ]; then
    echo "$spec was deleted; skipping changelog check."
    continue
  fi
  if ! git diff "${base}...HEAD" -- "$spec" | grep -E '^\+[^+]' | grep -qE '\bCR-[0-9]+\b'; then
    error "$spec changed without a new changelog line that references a change request (CR-<issue number>)."
  fi
done

# 3. Contract changes are traced to a CR (via a spec.md changelog) and bump info.version.
if [ -n "$contract" ] && [ -z "$specs" ]; then
  error "api/openapi.yaml changed without a change to any specs/*/spec.md. Add a changelog row naming the change request."
fi
if [ -n "$contract" ]; then
  if git cat-file -e "${base}:api/openapi.yaml" 2>/dev/null; then
    old_version=$(git show "${base}:api/openapi.yaml" | info_version)
    new_version=$(info_version < api/openapi.yaml)
    if [ -z "$new_version" ]; then
      error "api/openapi.yaml has no info.version."
    elif [ "$old_version" = "$new_version" ]; then
      error "api/openapi.yaml changed but info.version is still ${old_version}. Bump it: patch for wording, minor for additive changes, major for breaking changes."
    else
      echo "Contract version: ${old_version} -> ${new_version}"
    fi
  else
    echo "api/openapi.yaml is new; nothing to compare."
  fi
fi

if [ "$failed" -eq 0 ]; then
  echo "Change traceability: OK"
fi
exit "$failed"
