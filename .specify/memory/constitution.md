# Project constitution — spec-first services

These principles bind every Spec Kit command (`/speckit.*`) and every human or AI
contributor. When a plan, task or implementation conflicts with them, the
constitution wins and the conflict is reported, not worked around.

## Article I — The contract comes first

1. Every externally visible API change starts as a change to `api/openapi.yaml`.
2. Spec changes are made in a **spec-only pull request** (only `api/**`, `specs/**`
   and `.spectral.yaml` change) and are approved by the spec owners in
   `.github/CODEOWNERS` before implementation starts.
3. Implementation work never edits `api/openapi.yaml`, `specs/**/spec.md`,
   `.spectral.yaml` or this constitution. If the spec looks wrong or incomplete,
   implementation stops and the problem is reported as an open question in the
   feature's `spec.md`.

## Article II — Code is derived from the contract

1. API interfaces and DTOs are **generated** from `api/openapi.yaml` by
   `openapi-generator-maven-plugin` at build time. Generated code is never
   committed and never edited by hand.
2. Every `@RestController` implements a generated `*Api` interface. Hand-written
   request mappings or DTOs that duplicate the contract are not allowed
   (enforced by `ArchitectureTest`).
3. Generated DTOs stay in the web layer. The domain package does not depend on
   web, generated or Spring MVC types.

## Article III — Behaviour is specified as rules, and rules are tested as properties

1. Every business rule has an id (`R1`, `R2`, …) in the feature's `spec.md`.
2. Every OpenAPI operation lists the rules it implements in `x-business-rules`.
3. Every rule has at least one property-based test whose name starts with the rule
   id (for example `r3_discountNeverExceedsCap`). Example-based tests are
   welcome in addition, never instead.
4. Tests that verify a rule are derived from the rule text, not from the
   implementation. Agents that write implementation code do not write or weaken
   rule tests in the same change.

## Article IV — Verification gates decide, not reviewers' memory

A change merges only when all gates pass:

| Gate | Tool | Checks |
|---|---|---|
| Spec style | Spectral (`.spectral.yaml`) | API guidelines |
| Compatibility | oasdiff | No breaking change without a major version bump |
| Build | Maven (`mvn verify`) | Generated interfaces compile, unit and property tests pass |
| Architecture | ArchUnit | Layering and spec-first rules |
| Test strength | PIT | Mutation score ≥ 80 % on the domain |
| Conformance | Schemathesis | Running service matches the contract |
| Traceability | `check-traceability.sh` | Spec changes reference a change request; versions are bumped |

Gates are never disabled, skipped or loosened in the same pull request that
needs them to pass.

## Article V — Simplicity

1. Money is integer cents. No floating point for amounts.
2. Errors are RFC 9457 problem details.
3. Prefer the standard Spring Boot stack; add a dependency only when the plan
   explains why.

## Article VI — Change requests

1. Every change to a spec starts as a change-request issue. Its id is the issue
   number: issue #17 is `CR-17`.
2. Before changing anything, classify the request: rule change, additive API
   change, breaking API change, new feature, spec bug — or an implementation bug,
   where the spec is right and only code changes.
3. The spec changes first: `spec.md` (with a changelog entry naming the CR),
   then `api/openapi.yaml` (with a bumped `info.version`: patch for wording,
   minor for additive, major for breaking), then the rule tests. Code follows.
4. A rule test may only change together with the rule it encodes. A changed
   test without a changed rule means the test was bent to fit the code.
5. Breaking changes are avoided where an additive path exists (add the new field,
   deprecate the old one, remove it in the next major version). Unavoidable
   breaking changes need approval from the affected consumers.
6. Hotfixes follow the same rules; only the review is faster.

Rules 3 and 4 are checked by the `Change traceability` job in CI.

## Governance

Changes to this constitution follow the same process as spec changes (Article I.2)
and need approval from the API guild.

**Version**: 1.1.0 | **Ratified**: 2026-10-17 | **Amended**: 2026-10-17 (Article VI)
