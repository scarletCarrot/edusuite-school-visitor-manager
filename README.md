# EduSuite Visitors — School Visitor Management

A visitor management module for a single K-12 campus. Employees pre-register their visitors. The gate registers
walk-ins and the named employee confirms or declines them in the app. Visitors receive a numbered lanyard card
and return it on the way out. The principal can see at any moment who is on campus, and every action is audited.

| Document | What it covers |
|---|---|
| [docs/spec.md](docs/spec.md) | Requirements, user stories with acceptance criteria, lifecycle, data model, API, decisions, risks, scope |
| [docs/assumptions.md](docs/assumptions.md) | Every open question I resolved, with reasoning and impact if wrong |
| [docs/test-plan.md](docs/test-plan.md) | Test levels, traceability, edge cases, what is not automated, exploratory notes |
| [docs/ai-usage.md](docs/ai-usage.md) | How AI tools were used and kept in check |
| [CLAUDE.md](CLAUDE.md) | Rules for anyone (human or AI) changing this code |

## What you need

- **Docker** with Compose v2 (Docker Desktop on Windows/macOS). This is all you need to run the app.
- To run the tests as well: **Java 21** (backend tests; Docker must be running for Testcontainers) and **Node.js 24** (end-to-end tests).
- Port **4200** must be free.

## Run the app

```bash
docker compose up --build
```

Open http://localhost:4200 once the backend has logged `Started VisitorsApplication`. The first build takes a few
minutes.

### Seeded logins (fictional)

Every password is `demo1234`.

| Username | Name | Role | Lands on |
|---|---|---|---|
| `desk` | Gary Guard | FRONT_DESK | Gate screen: walk-ins, cards, check-out, today's list |
| `tina` | Tina Teacher | EMPLOYEE | Host screen: pre-register, confirm/decline, notifications |
| `ben` | Ben Bursar | EMPLOYEE | Host screen |
| `office` | Admin Office | EMPLOYEE | Host screen; used when a walk-in cannot name a person (A5) |
| `admin` | Paula Principal | ADMIN | On-campus view and audit trail (read-only) |

### A two-minute demo

1. Sign in as `desk` in one browser window and `tina` in a private window.
2. As `desk`, register a walk-in to see Tina Teacher. The row shows *Waiting for host* and offers no card yet.
3. As `tina`, the notification appears within 5 seconds. Click **Confirm**.
4. As `desk`, the row turns *Approved*. Type a card number (e.g. `12`) and click **Check in**. The on-campus count goes up.
5. Enter `12` under **Check out**. The visit shows *Left*; checking out `12` again is refused because the pass is no longer valid.
6. Sign in as `admin` to see who is on campus and the audit trail of everything above.

Simulated email and SMS messages are written to the backend log:

```bash
docker compose logs -f backend
```

To see the timeout and overstay rules without waiting 10 minutes or 4 hours, start the stack with shorter limits:

```bash
VISITORS_HOST_RESPONSE_TIMEOUT=PT1M VISITORS_MAX_STAY=PT2M docker compose up --build
```

(PowerShell: `$env:VISITORS_HOST_RESPONSE_TIMEOUT="PT1M"; $env:VISITORS_MAX_STAY="PT2M"; docker compose up --build`)

## Run the tests

| Level | Command | What it proves |
|---|---|---|
| Unit + integration (backend) | `cd backend && sh mvnw verify` (Windows: `mvnw.cmd verify`) | Every lifecycle transition; both flows, timeout, card rules and the role matrix through the real API against PostgreSQL (Testcontainers) |
| End-to-end | `cd e2e && npm ci && npx playwright install chromium && npx playwright test` | A walk-in from arrival to check-out in a real browser, with the guard and the host in separate sessions |

The end-to-end test uses the running stack, or starts `docker compose up --build` itself if nothing answers on
port 4200. CI ([.github/workflows/ci.yml](.github/workflows/ci.yml)) runs both on every push and pull request.

## Develop

```bash
cd backend && sh mvnw spring-boot:test-run
```

This starts the API on http://localhost:8080 with a throwaway PostgreSQL in Testcontainers. In another terminal
run `cd frontend && npm ci && npm start` for the UI with live reload on http://localhost:4200; it proxies `/api` to
port 8080. Stop the Compose stack first, because it also uses port 4200.

```
backend/   Spring Boot 4.1, Java 21. Packages: visit (lifecycle, use cases, API), user, notification, audit, config
frontend/  Angular 22: login, desk, host and admin pages; nginx config for Docker
e2e/       Playwright end-to-end test
docs/      Spec, assumptions, test plan, AI usage
```

## Known limitations

- **Authentication is demo-grade:** HTTP Basic with seeded accounts. The browser keeps credentials in `sessionStorage`, and there is no HTTPS. Production needs the school's SSO (spec D7).
- **Notifications are sent synchronously and simulated.** There is no retry or outbox, so a failing channel is audited but not retried.
- **Screens poll every 5 seconds** instead of receiving pushes.
- **Not built (ranked Should/Could, spec §12):** printable or QR pass, cancelling a pre-registration, the data-retention job, timeout escalation, more than one school, user management.
- A walk-in that is approved but never given a card stays *Approved* (A20).
- When a guard checks out a visitor who left without returning the card, the departure time is when the guard closed the visit (A18).
- The UI is functional rather than polished and was designed for a desktop or tablet at the gate.

## What I would do next

1. The Should items: a printable pass, cancelling a pre-registration, and the retention/anonymisation job.
2. Escalate unanswered walk-ins to the admin office before denying entry, and add reporting on host response times (the success measures in spec §1).
3. Production hardening: SSO, HTTPS, a transactional outbox with retries for real email/SMS providers, and server-sent events instead of polling.
4. More guardrails: Dependabot and a dependency vulnerability scan, ESLint and Checkstyle in CI, and a latch-based test for the concurrency races (test plan §4).
