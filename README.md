# Spec-first reference service (Spring Boot + OpenAPI + Spec Kit)

A small orders service that shows one way to run **spec-first development with
AI coding agents**: humans own the specification, code is generated from it or
checked against it, and automated gates decide whether a change can merge.

Stack: Java 21 · Spring Boot 4.1 · Maven · openapi-generator 7.22 · GitHub Actions ·
Spectral · oasdiff · jqwik · ArchUnit · PIT · Schemathesis · GitHub Spec Kit.

## The idea in one picture

```
 human-owned spec                  generated / derived              verified by
 ─────────────────                 ───────────────────              ───────────
 specs/001-.../spec.md  (rules R1–R5) ──► jqwik property tests ───► mvn verify, PIT
 api/openapi.yaml       (contract)    ──► OrdersApi interface ────► compiler, ArchUnit
                                      ──► running service ────────► Schemathesis
 .spectral.yaml         (API style)   ──────────────────────────────► Spectral
 previous openapi.yaml  (compat)      ──────────────────────────────► oasdiff
```

An agent can write the controller and domain code, but it cannot change what
"correct" means: the contract, the rules and the gates are read-only for agents
(`AGENTS.md`), owned by people (`.github/CODEOWNERS`) and guarded in CI
(`spec-guard` job).

## Repository layout

```
api/openapi.yaml                    The API contract (OpenAPI 3.0.3)
specs/001-order-management/spec.md  Intent, acceptance criteria, business rules R1–R5
.specify/memory/constitution.md     Spec Kit constitution: the spec-first rules
.spectral.yaml                      API style guide as lint rules
AGENTS.md, CLAUDE.md,               Instructions for coding agents
  .github/copilot-instructions.md
.github/CODEOWNERS                  Who must approve spec and gate changes
.github/workflows/spec-gate.yml     Gate 1: lint, breaking changes, traceability, spec ownership
.github/scripts/check-traceability.sh  Rule tests follow rules; spec changes name a CR
.github/ISSUE_TEMPLATE/             Change-request and bug forms
.github/workflows/build.yml         Gates 2–4: build/tests, mutation, conformance
pom.xml                             Generates OrdersApi + DTOs from the contract
src/main/java/com/example/orders/
  domain/          Business rules, plain Java (no Spring, no DTOs)
  infrastructure/  In-memory repository (swap for a real database)
  web/             Controller implementing the generated OrdersApi, DTO mapping, errors
src/test/java/com/example/orders/
  domain/OrderRulesProperties.java  Property tests, one or more per rule (spec-owned)
  domain/OrderServiceTest.java      Example-based tests
  architecture/ArchitectureTest.java Spec-first architecture rules
```

## How a feature flows

| Step | Who | Spec Kit command | Artifact | Gate |
|---|---|---|---|---|
| 1. Describe intent and rules | Human (AI may draft) | `/speckit.specify`, `/speckit.clarify` | `specs/NNN-*/spec.md` | Review |
| 2. Write the contract | Human (AI may draft) | `/speckit.plan` | `api/openapi.yaml` | Spectral, oasdiff |
| 3. Write rule tests | Human (AI may draft) | — | `*RulesProperties.java` | Review |
| 4. Approve | Spec owners | — | PR labelled `spec-change` | CODEOWNERS, spec-guard |
| 5. Implement | Agent or human | `/speckit.tasks`, `/speckit.implement` | `src/main/**` | Build, ArchUnit, PIT, Schemathesis |

"AI may draft" is fine: the point is that a person reviews and owns steps 1–4,
and that the agent doing step 5 cannot edit their output.

**Ordering in practice.** Because controllers implement generated interfaces with
no default methods, a contract change that adds an operation does not compile
until it is implemented. Use either:

- **One PR, two phases (small changes):** push the spec change first, get spec-owner
  approval, then push the implementation to the same branch. Merge when all gates
  are green.
- **Stacked PRs (larger features):** PR A changes the spec (base: `main`); PR B
  implements it (base: PR A's branch). Merge B into A, then A into `main`.

## Change requests

A change request is a GitHub issue created with the **Change request** form. Its
id is the issue number: issue #17 is `CR-17`. The **Bug report** form first asks
whether the code contradicts the spec, because a bug where the spec itself is wrong
is a change request, not a code fix.

| Type | Changes | Approval | What CI does |
|---|---|---|---|
| Implementation bug | `src/main/**` + a test that would have caught it | Code review | Normal gates |
| Rule change / spec bug | `spec.md` rule + changelog, rule tests, maybe contract | Spec owners | Rule tests fail until the code follows |
| Additive API change | `spec.md` changelog, contract (minor bump) | Spec owners | oasdiff passes; compiler forces the mapping |
| Breaking API change | `spec.md` changelog, contract (major bump) | Spec owners + consumers | oasdiff fails without the major bump |
| New feature | New `specs/NNN-*/spec.md`, contract | Spec owners | As above |

The `Change traceability` job (`.github/scripts/check-traceability.sh`) enforces:

1. A changed `*RulesProperties.java` needs a changed `spec.md`: tests change
   because rules change, never to make code pass.
2. A changed `spec.md` needs a new line naming `CR-<number>` (the changelog row).
3. A changed `api/openapi.yaml` needs a changed `spec.md` and a new `info.version`.

Example, CR-17 "raise the discount cap to €750":

1. Spec PR (label `spec-change`): R3 in `spec.md` says 75,000 with
   *"Changed by CR-17; was 50,000"*, a changelog row `1.1.0 | CR-17 | ...`,
   `discountCents.maximum: 75000` and `version: 1.1.0` in the contract, and the
   cap updated in `r3_discountIsTenPercentCapped` and `r3_boundaries`.
2. CI: Spectral, oasdiff and traceability pass; the build fails on R3, as expected.
3. Implementation (an agent can do this: "implement CR-17"): change
   `DISCOUNT_CAP_CENTS` in `Pricing.java`. All gates go green; merge.

## Run it locally

Prerequisites: JDK 21, Maven 3.9+, Node 22 (for Spectral), Python 3.11+ (for Schemathesis).

```bash
mvn verify                      # generate code from the contract, compile, run all tests
mvn -Pmutation verify           # + mutation testing (report: target/pit-reports/index.html)
mvn spring-boot:run             # start on http://localhost:8080

npx @stoplight/spectral-cli@6.17.0 lint api/openapi.yaml

pip install schemathesis==4.31.1
st run api/openapi.yaml --url http://localhost:8080 --checks all
```

Try it:

```bash
curl -i -X POST localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"customerId":"cust-42","lines":[{"sku":"BOOK-001","quantity":50,"unitPriceCents":2499}]}'
# 201, subtotal 124950, discount 12495 (R3), total 112455
```

To see the generated interface: `target/generated-sources/openapi/src/main/java/com/example/orders/api/OrdersApi.java`.

## Use with GitHub Spec Kit

```bash
uv tool install specify-cli --from git+https://github.com/github/spec-kit.git
specify init . --integration claude        # or: --integration copilot
git diff .specify/memory/constitution.md   # init may replace it with a template
git checkout -- .specify/memory/constitution.md   # keep this repo's constitution
```

The constitution makes every `/speckit.*` command follow the spec-first rules:
contract changes go into `api/openapi.yaml`, `/speckit.implement` never edits
spec files, and every rule gets a property test.

## Turn on the gates in GitHub

The workflows only protect `main` if branch protection requires them:

1. **Settings → Rules → Rulesets (or Branches) → `main`:**
   - Require a pull request, with **Require review from Code Owners**.
   - Require status checks: `Spectral lint`, `Breaking-change check (oasdiff)`,
     `Change traceability`, `Spec ownership guard`, `Build and test`,
     `Mutation testing (PIT)`, `Contract conformance (Schemathesis)`.
2. Replace the `@your-org/...` teams in `.github/CODEOWNERS`.
3. Create the labels `spec-change`, `change-request` and `bug`.
4. Add your agents' GitHub accounts to `AGENT_ACCOUNTS` in `spec-gate.yml`.

## What each gate catches

| Gate | Fails when |
|---|---|
| Spectral | The contract breaks the style guide: non-camelCase operationIds or properties, money not in integer cents, errors not `application/problem+json`, operations without `x-business-rules`, open request schemas |
| oasdiff | A change breaks existing clients without a major version bump |
| Traceability | A rule test changes without its rule, a spec changes without a `CR-<n>` changelog entry, or the contract changes without a version bump |
| spec-guard | An agent account changes spec or gate files, or a human does without the `spec-change` label |
| Compiler | The controller doesn't implement every operation in the contract, or uses wrong types |
| ArchUnit | A controller doesn't implement a generated interface, declares its own mappings, the domain depends on Spring/DTOs, or DTOs leak out of the web layer |
| jqwik | A business rule (R1–R5) is violated for any generated input |
| PIT | Less than 80 % of injected bugs in the domain are caught by the tests |
| Schemathesis | The running service returns 5xx, undocumented status codes or content types, responses that don't match the schema, accepts invalid input, or rejects valid input |

## Verification status of this repository

Built in an environment without access to Maven Central, so **the Maven build has
not been run yet. The first CI run is the real build check.**

| Checked | How |
|---|---|
| Spec passes the Spectral ruleset; each custom rule fires on a deliberately broken copy | Spectral 6.17.0 |
| Workflows are valid, including shell scripts | actionlint 1.7.12 + shellcheck |
| `spec-guard` logic (bot author, missing label, clean PR) | Simulated against a local git repo |
| Traceability check: 12 cases (rule test without rule, spec without CR, contract without bump or CR, valid CRs) | Simulated against a local git repo; shellcheck clean |
| Controller, mapper and error handler type-check against the generated `OrdersApi` and DTOs | openapi-generator 7.22.0 with the pom's options, then javac 21 against minimal Spring/Jakarta stubs |
| Domain and infrastructure code compile and satisfy R2–R5 on sample inputs | javac 21 + smoke run; R3 checked exhaustively up to 600,000 cents |
| Schemathesis loads the contract and finds all 6 operations | Schemathesis 4.31.1 |
| **Not yet run:** Spring wiring, web layer, jqwik/ArchUnit/PIT tests, Schemathesis against the live service | Needs the first CI run |

## Known risks (check on the first build)

- **jqwik is pinned to 1.9.3.** From 1.10 on, jqwik prints an "anti-AI usage"
  notice aimed at coding agents, and 1.10.0 contained a hidden prompt-injection
  line. In a repo where agents read test output, avoid those releases.
  Dependabot is configured to skip them. 1.9.3 predates Spring Boot 4's move to
  JUnit 6; if its engine fails to start on JUnit 6, either override the JUnit BOM
  to 5.x for tests or replace jqwik with another property-testing approach.
- **pitest-junit5-plugin 1.2.3 on JUnit 6:** reported to work with Spring Boot
  4.1, but not documented. If PIT finds 0 tests, check this first.
- **Strict JSON:** `application.yaml` rejects unknown fields and floats for
  integers. Jackson still coerces `"5"` to `5`; Schemathesis' negative tests may
  report that. Treat it as a real finding and decide whether to tighten it.
- **oasdiff image** uses the `latest` tag; pin a release tag.

## Adapting this to your services

1. Copy `.spectral.yaml`, `.specify/memory/constitution.md`, `AGENTS.md`, `CODEOWNERS`
   and the two workflows into a service repository.
2. Take a snapshot of the service's current API (springdoc's `/v3/api-docs.yaml`),
   clean it up by hand and commit it as `api/openapi.yaml`.
3. Add the `openapi-generator-maven-plugin` configuration from `pom.xml` and make
   each controller implement the generated interface (the compiler shows what is missing).
4. Write `spec.md` with rule ids for the service's key business rules, add
   `x-business-rules` to the operations, and add one property test per rule.
5. Start with Spectral and oasdiff as non-required checks; make them required
   once the first services pass.
