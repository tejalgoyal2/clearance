# Clearance: open-source licence compliance checker

> **Paste this into Claude Code to start:**
> Read SPEC.md and build this project phase by phase. For each phase, write the tests first, run them and watch them fail, then implement until they pass. Stop after every phase, run the full test suite, and report what passed and what didn't. Keep the code plain and readable: I will study every file for a Java code-review interview.

## Why this exists

I'm applying to SAP's Global Technology Legal Compliance (GTLC) team. GTLC governs third-party IP in SAP's products (open-source licence compliance, software supply chain, AI governance) and builds in-house automation for it on Java (Spring or Grails) and Angular. This project is a small, real version of that work. It must be correct, tested, readable, and something I can explain line by line.

## What it does, in one sentence

Upload a project's dependency file and get every dependency's licence, a verdict against a licence policy, and a reviewer workflow with an audit trail.

## Done means (evidence, not claims)

- `docker compose up` starts Postgres, the API and the UI with no other setup.
- Uploading `samples/pom.xml` and `samples/package-lock.json` each produces at least one APPROVED, REVIEW, UNKNOWN and DENIED component.
- A reviewer can accept or reject a flagged component with a comment, and the audit view shows who decided what, and when.
- The CSV report downloads.
- Backend tests, frontend tests and the Playwright end-to-end test all pass locally and in CI.
- The README explains the project in under a minute.

## Stack

- **Backend:** Java 21 or 25 (LTS), current stable Spring Boot (Web, Validation, Data JPA), Maven.
- **Database:** PostgreSQL in Docker.
- **Frontend:** current stable Angular, standalone components, Angular Router, HttpClient. Plain CSS or Angular Material; keep it clean.
- **Tests:** JUnit 5, Mockito, Spring MockMvc, Testcontainers (Postgres), Angular's default test runner, Playwright for one end-to-end journey.
- **Docker:** multi-stage images for backend and frontend, plus `docker-compose.yml` with postgres, backend and frontend and healthchecks.
- **CI:** GitHub Actions running backend tests, frontend build and tests, and the Docker build. Pin actions to commit SHAs.
- **Licence data:** the deps.dev (Open Source Insights) public API. Check its current docs for the exact endpoint before coding. Fallback: the `license` field in npm lockfiles.
- **Repo:** public, named `clearance`.

## Domain model

- **Scan:** id, projectName, ecosystem (MAVEN or NPM), createdAt, counts per status.
- **Component:** id, scan, name, version, rawLicense (as found), spdxExpression (normalized), policyStatus (APPROVED, REVIEW, UNKNOWN, DENIED), decision (NONE, ACCEPTED, REJECTED), decidedBy, decidedAt, decisionComment.
- **AuditEvent:** id, scan, component (nullable), actor, action (SCAN_CREATED, DECISION_MADE, REPORT_EXPORTED), detail, timestamp. Append-only: no update or delete anywhere.

## Licence policy

Lives in `policy.yml`, not in code:

- **approved:** MIT, Apache-2.0, BSD-2-Clause, BSD-3-Clause, ISC, 0BSD, Unlicense
- **review:** LGPL-2.1-only, LGPL-3.0-only, MPL-2.0, EPL-2.0, CDDL-1.0
- **denied:** GPL-2.0-only, GPL-3.0-only, AGPL-3.0-only, SSPL-1.0
- **aliases:** e.g. "Apache 2.0" and "The Apache Software License, Version 2.0" map to Apache-2.0
- **exceptions:** full `X WITH Y` expressions with their own verdict, e.g. "GPL-2.0-only WITH Classpath-exception-2.0" is review

Rules, deny-first:

1. Severity order: APPROVED (0) < REVIEW (1) < UNKNOWN (2) < DENIED (3).
2. `A OR B` takes the least severe side (you may choose either licence). `A AND B` takes the most severe side (you must meet both).
3. `X WITH Y` looks up the full expression in `exceptions` first, otherwise falls back to X.
4. An identifier the policy doesn't know is UNKNOWN.
5. A malformed expression is UNKNOWN, stored with the parse error. A bad licence never fails the whole scan.
6. A reviewer's decision never changes policyStatus. It records an exception with who and why.
7. The same licence in two lists is a config error: fail at startup, loudly.

## SPDX expressions: the data-structures piece

Tokenize, parse into a tree with recursive descent, then evaluate the tree against the policy. Precedence follows the SPDX spec: WITH binds tightest, then AND, then OR; parentheses override. The parser gets its own class and its own tests.

| Expression | Verdict |
|---|---|
| `MIT` | APPROVED |
| `MIT OR GPL-3.0-only` | APPROVED |
| `Apache-2.0 AND GPL-3.0-only` | DENIED |
| `(MIT OR GPL-3.0-only) AND LGPL-2.1-only` | REVIEW |
| `MIT AND` | UNKNOWN (malformed) |

## Dependency parsers: the OOP piece

- Interface `DependencyFileParser` with `supports(filename)` and `parse(InputStream)`, returning a list of dependencies.
- Implementations `MavenPomParser` and `NpmLockfileParser`. A registry picks one by filename: the strategy pattern.
- **Maven:** direct dependencies from `pom.xml`. Resolve `${property}` versions from `<properties>`. Skip `test` scope by default, because test dependencies don't ship, and say so in the README.
- **npm:** `package-lock.json` v2/v3 `packages` entries, including transitive ones, with their `license` field.
- De-duplicate identical name+version pairs.

## Licence resolver

- Interface `LicenseResolver`. `LockfileLicenseResolver` comes first, then `DepsDevLicenseResolver` (HTTP with a timeout and an in-memory cache keyed by ecosystem+name+version), then UNKNOWN.
- Timeouts, 404s and network errors become UNKNOWN with a reason. Never a failed scan.

## REST API

- `POST /api/scans` (multipart: file, projectName) returns 201 with the scan summary. 400 for an unsupported or malformed file, 413 over 2 MB.
- `GET /api/scans`, `GET /api/scans/{id}`, `GET /api/scans/{id}/components?status=DENIED`
- `POST /api/components/{id}/decision` with `{decision, reviewer, comment}` returns 200. 400 if the comment is missing, 404 for an unknown id, 409 if already decided.
- `GET /api/scans/{id}/audit`
- `GET /api/scans/{id}/report.csv`
- `GET /api/policy`
- One `@ControllerAdvice` gives every error the same JSON shape.

## Angular UI

- **Scans:** the list, plus a New scan button.
- **New scan:** file upload and project name.
- **Scan detail:** counts per status, filter chips, the components table, and a decision dialog.
- **Audit:** a timeline per scan.
- **Policy:** read-only lists.

Show each licence expression as written, next to its verdict. Use colour only for meaning (approved, review, unknown, denied).

## Test plan (write each test first and watch it fail)

**Unit**
- SPDX parser: a single id, OR, AND, AND-before-OR precedence, parentheses, WITH. Ambushes: empty string, dangling operator, unbalanced parentheses, lowercase `mit`, extra spaces, unknown id.
- Policy engine: OR takes the best side, AND takes the worst, alias normalization, WITH lookup, unknown is UNKNOWN. Ambush: the same licence in two lists fails config loading.
- Parsers: pom with property versions; test scope skipped; missing version; duplicates removed; malformed XML gives a clear error; npm lockfile v3 with nested packages; missing `license` field.
- Resolver: a cache hit makes no second call; timeout is UNKNOWN; 404 is UNKNOWN.

**Integration (Testcontainers Postgres)**
- Create a scan, check components persist with the right statuses, make a decision, check one audit event was appended.

**Controller (MockMvc)**
- 201 on a valid upload, 400 on an unsupported file, 404 on an unknown component, 409 on a second decision.

**End to end (Playwright)**
- Upload `samples/pom.xml`, find a DENIED component, accept it with a comment, and see the decision in the audit view.

## Samples

`samples/pom.xml` and `samples/package-lock.json` with real packages that cover every verdict. `com.mysql:mysql-connector-j` (GPL-2.0 with the Universal FOSS Exception) makes a good WITH example. Check each package's licence on deps.dev before writing expected results into tests.

## Phases and time boxes

1. **Engine (1.5 h):** SPDX parser, policy engine, both parsers and the resolver, with unit tests.
2. **API and persistence (1 h):** entities, repositories, services, controllers, the Testcontainers test.
3. **UI (1.5 h):** the five pages, wired to the API.
4. **Ship (1 h):** Dockerfiles, compose, CI, the README with a screenshot and a Mermaid diagram, and the Playwright test.

If time runs short, ship phases 1, 2 and 4 with the scan-detail page only.

## Not doing, on purpose

No login (single-user demo; the reviewer name is a field), no transitive Maven resolution, no source-code scanning, no licence-text matching. Not legal advice, and the README says so.

## README must include

- One line on what it is and why.
- A screenshot.
- How to run it: `docker compose up`.
- The policy and its rules.
- Design decisions with reasons: strategy-pattern parsers, SPDX tree evaluation, deny-first, append-only audit, and why failures become UNKNOWN instead of crashing.
- Test summary, limitations, and what's next.

## For Tejal: be able to explain all of this

- **OOP:** the parser interface and why it's a strategy pattern; records for value objects; enums with a severity order.
- **Spring:** dependency injection; controller, service and repository layers; JPA relations (Scan 1–N Component, Scan 1–N AuditEvent); `@Transactional` on the decision; validation; `@ControllerAdvice`.
- **Data structures:** the SPDX expression tree and recursive-descent parsing; the cache, and HashMap versus ConcurrentHashMap.
- **REST and database design:** the status codes; why the audit trail is append-only; an index on (scan_id, policy_status).
- **Testing:** unit versus integration versus end to end; why Testcontainers instead of H2; one test you watched fail first.
- **Docker:** multi-stage builds, compose networking, healthchecks.
- **Angular:** components, services, HttpClient observables, routing.
- **Java code-review traps:** `==` versus `equals`, the equals/hashCode contract, null handling and Optional, try-with-resources, changing a list while iterating it, shared mutable state across threads, swallowed exceptions.

## Report back

- The repo URL (public).
- Which "Done means" items work.
- Test counts: backend, frontend and end to end.
- One screenshot.
- Anything you cut.
