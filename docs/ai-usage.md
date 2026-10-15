# AI usage notes

## Tools and what they were used for

| Tool | Used for |
|---|---|
| **Claude Code** (Claude Opus 5.5, in the Claude desktop app) | Reading the exam PDF; drafting the spec and assumptions; writing backend, frontend, tests and docs; running builds and tests; an exploratory test session in the app's built-in browser. |
| **Spring Initializr** and **Angular CLI** (`ng new`) | Project scaffolding. I used deterministic generators for boilerplate instead of AI-written setup code, so the starting point is the framework's own default. |

No other AI tools were used.

## How I worked

I treated the AI as a fast pair programmer and kept the decisions and the review gate with me:

1. **Framing:** I gave Claude Code the exam with three standing instructions: *implement just the required
   functionality; keep changed lines to a minimum; don't commit, give me the commit content and I commit
   manually.* Committing by hand meant I read each phase's diff and test output before it entered the history.
2. **Spec before code:** the first commit is only `docs/spec.md`, `docs/assumptions.md` and `CLAUDE.md`. The
   lifecycle, roles, acceptance criteria and the Must/Should/Could ranking were settled before any code.
3. **Small, test-backed phases:** domain and unit tests → API, security and integration tests → UI and Docker →
   end-to-end test and CI → docs. Each phase ended with a green test run before I committed.
4. **Scope control:** only Musts were built. The AI proposed a ranking with reasons (spec §12), and Should and
   Could items (printable or QR pass, multi-tenancy, retention job) were documented, not built.

## Setup

- **[CLAUDE.md](../CLAUDE.md)** holds the rules every change must follow:
  - State changes go only through `Visit` methods.
  - Every state change is audited in the same transaction.
  - Authorisation is enforced on the server.
  - Seed and test data are fictional.
  - Each behaviour is tested at the cheapest level that proves it.
  - Every new assumption is recorded.
  - Diffs stay minimal, and no secrets are committed.
- **Prompts that worked well:**
  - The three standing instructions above. Asking for minimal changes stopped the AI from adding abstractions nobody asked for.
  - One phase per prompt ("Continue with the backend API, security, notifications and integration tests") kept each step reviewable.
  - Asking for exact commit commands and messages at every checkpoint.

## Guardrails

| Guardrail | What it catches |
|---|---|
| Table-driven lifecycle test (every action from every state) | Any new or accidental path between states, e.g. a pass issued without host confirmation |
| Role-matrix integration test (21 cases) | A security rule loosened in `SecurityConfig` |
| Testcontainers PostgreSQL instead of H2 | Schema and constraint problems that only show on the real database |
| Database constraints (partial unique index on active cards, check constraints, optimistic locking) | Bugs that slip past the code: the database refuses invalid data anyway |
| End-to-end test of the walk-in journey | UI, nginx proxy and API wiring broken together |
| GitHub Actions CI on every push | Regressions before they are merged |
| Exploratory session on the running stack | What scripted tests did not think of (see below) |
| Manual review of every diff before committing | AI output I don't understand or don't want |

Not yet in place, and listed as next steps in the README: ESLint and Checkstyle in CI, and dependency vulnerability
scanning.

## Where AI output was corrected or rejected

1. **A wrong test, not wrong code.** The first overstay unit test failed. Before changing anything I checked which
   side was wrong: the visit in the test was checked in at `NOW`, but the assertions measured four hours from
   `NOW + 15 min`. The domain rule matched the spec, so the test was fixed and the code left alone. A red test is a
   question, not an instruction to change the code until it is green.
2. **Framework version.** The exam suggests Spring Boot 3, but Spring Initializr now only generates 4.x, and 3.5's
   open-source support ended in June 2026. Rather than pin an unsupported version I chose Boot 4.1 and recorded
   the reason as decision D12 in the spec.
3. **Gaps the tests missed.** In the exploratory session (test plan §5) I found that an untouched optional phone
   field was stored as `""`, and that a guard clicking Check in with no card saw "Invalid request content." Both
   were fixed, and the phone case got a unit test.
4. **Builds that only worked on Windows.** The Maven wrapper was committed without the executable bit (the repo is
   developed on Windows), which would break the Linux CI runner and Docker build. The Dockerfile runs `chmod`, and
   CI calls `sh mvnw`.
5. **Process correction.** Early on, the commit content was printed above the session's status summary, and two
   commits ended up with the summary as their message. I changed the working agreement so that exact commit
   commands are always the last thing in each response.

## Security and data

- No secrets, personal data or real names were given to the AI. All seed and test data is fictional
  (`example.edu`, `+63 900 000 xxxx`).
- The only credentials in the repository are local-development values in `docker-compose.yml` and the demo
  password for seeded accounts, both documented as such.
