# Instructions for AI coding agents

This repository is **spec-first**. Read `.specify/memory/constitution.md` before
doing anything. The short version:

## Read-only for agents

Do not create, edit, rename or delete these files during implementation work:

- `api/openapi.yaml` (the API contract)
- `specs/**/spec.md` (intent and business rules)
- `.spectral.yaml` (API style rules)
- `.specify/memory/constitution.md`
- `.github/workflows/**` and `.github/CODEOWNERS` (the verification gates)

If the contract or a rule seems wrong, incomplete or contradictory: **stop**,
explain the problem, and propose the change as text in your reply. A human makes
spec changes in a separate pull request.

## How to implement a feature

1. Run `mvn generate-sources`. The API interfaces and DTOs appear under
   `target/generated-sources/openapi` (package `com.example.orders.api`).
2. Implement the generated `*Api` interface in a `@RestController` in
   `com.example.orders.web`. Do not add `@RequestMapping`/`@GetMapping`/... to
   controllers; the mappings come from the interface.
3. Put business logic in `com.example.orders.domain`. It must not import web,
   generated or Spring MVC types.
4. Map between generated DTOs and domain types in the web layer only.
5. Run `mvn verify` and fix failures in your implementation, not in the tests or
   gates.

## Change requests

You will often be asked to "implement CR-<n>". The spec owners have already
changed `spec.md`, `api/openapi.yaml` and the rule tests for it. Your job is to
make the code satisfy them:

1. Read the CR issue and the `spec.md` changelog row for `CR-<n>`.
2. Run `mvn verify`; the failing rule tests and compile errors show what to change.
3. Change only `src/main/**` (and add tests if useful). Mention `CR-<n>` in commit messages.

If you are asked to fix a bug, first decide whether the code contradicts the spec.
If it does not (the spec itself is wrong), stop and say so: that is a change
request for a human, not a code fix.

## Tests

- Property tests for business rules (`*RulesProperties.java`) are owned by humans.
  You may add new tests. Do not delete, weaken or `@Disabled` existing ones.
- Never lower thresholds (PIT `mutationThreshold`, Schemathesis checks, Spectral
  severities) to make a build pass.

## Commands

| What | Command |
|---|---|
| Generate API code | `mvn generate-sources` |
| Build and test | `mvn verify` |
| Mutation testing | `mvn -Pmutation verify` |
| Lint the spec | `npx @stoplight/spectral-cli@6.17.0 lint api/openapi.yaml` |
| Run locally | `mvn spring-boot:run` |
