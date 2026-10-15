# CLAUDE.md — School Visitor Management

Instructions for AI coding assistants (and humans) working in this repository.

## What this is
A visitor management module for a single K-12 campus. Read `docs/spec.md` first (lifecycle in §4, API in §13),
then `docs/assumptions.md`.

## Layout
- `backend/` — Java 21, Spring Boot 4.1, PostgreSQL, Flyway. Packages by feature: `visit`, `user`, `notification`, `audit`, `config`.
- `frontend/` — Angular 22 (standalone components, signals), served by nginx in Docker, which proxies `/api`.
- `e2e/` — Playwright end-to-end tests against the Docker Compose stack.
- `docs/` — spec, assumptions, test plan, AI usage notes.

## Commands
- Whole app: `docker compose up --build` (UI at http://localhost:4200)
- Backend tests (needs Docker for Testcontainers): `cd backend && sh mvnw verify` (Windows: `mvnw.cmd verify`)
- Backend + throwaway Postgres for UI development: `cd backend && sh mvnw spring-boot:test-run`, then `cd frontend && npm start`
- E2E: `cd e2e && npm ci && npx playwright install chromium && npx playwright test`

## Rules
1. **Visit state changes only through `Visit` methods.** Never set `state` anywhere else. A new transition needs a
   unit test for the allowed path and for at least one rejected path.
2. **Every state change writes an audit event** in the same transaction (`VisitService`).
3. **Authorisation lives on the server:** role rules in `SecurityConfig`, ownership checks in `VisitService`.
   The UI hiding a button is never the protection.
4. **Fictional data only** in seeds and tests: `example.edu` emails, `+63 900 000 xxxx` numbers, made-up names.
5. **Test at the cheapest level that proves the behaviour:** domain rules → unit tests; HTTP, security and
   persistence → `*IT` integration tests (Testcontainers); one walk-in journey → Playwright. Don't duplicate.
6. **Record decisions:** any new assumption or open question goes into `docs/assumptions.md` (reasoning and impact
   if wrong), and the spec is updated if behaviour changes.
7. Keep diffs small and match the surrounding style. No new dependencies without a reason in the spec.
8. Never commit secrets. Credentials in `docker-compose.yml` are for local development only.
