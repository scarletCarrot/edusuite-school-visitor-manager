# Test plan

How the module is tested, why each behaviour is tested at its level, and what is deliberately not automated.
Acceptance criteria IDs (AC-x.y) refer to [spec.md §3](spec.md#3-user-stories-and-acceptance-criteria).

## 1. Strategy

Each behaviour is tested once, at the cheapest level that can prove it.

| Level | Where | Count | What belongs here |
|---|---|---|---|
| **Unit** | `backend/src/test/.../visit/VisitTest.java`, `.../notification/NotifierTest.java` | 46 | Domain rules that need no Spring or database: every lifecycle transition, allowed and rejected; date, timeout and overstay rules; channel failure isolation. |
| **Integration (API)** | `backend/src/test/.../VisitFlowIT.java`, `AccessControlIT.java` | 29 | What only the running system can prove: HTTP status codes, Spring Security rules, ownership checks, JSON shape, audit and notification side effects, and the Flyway schema and constraints on real PostgreSQL 17 (Testcontainers, not H2). |
| **End-to-end** | `e2e/tests/walk-in.spec.ts` | 1 | One walk-in journey through the real UI, nginx, API and database, with the guard and the host in separate browser sessions. |

The rejected-transition test is table-driven. It tries every action (confirm, decline, time out, check in,
check out) from every one of the 7 states: 6 combinations must succeed and the other 29 must throw
`VisitRuleException` and leave the visit unchanged. A new state or action automatically produces new
cases, so an accidental new path into `CHECKED_IN` is caught.

**Run:** `cd backend && sh mvnw verify` (unit, then `*IT`), and `cd e2e && npx playwright test`. CI runs both on every push.

## 2. Traceability

| AC | Behaviour | Unit | Integration | E2E |
|---|---|---|---|---|
| AC-1.1 | Pre-registration creates `EXPECTED`, visible to the desk | `preRegistrationIsExpectedOnItsDate` | `preRegisteredVisitorIsCheckedInAndOutWithALanyardCard` | — |
| AC-1.2 | Past date rejected | `preRegistrationInThePastIsRejected` | `preRegistrationIsBoundToItsDate` | — |
| AC-1.3 | Only employees pre-register | — | `AccessControlIT` (desk, admin → 403) | — |
| AC-2.1 | Check-in issues card, records times, notifies host | `checkInIssuesThePassAndRecordsArrivalForPreRegisteredVisitors` | `preRegisteredVisitor…` (card normalised, host notified) | — |
| AC-2.2 | No check-in on another date | `preRegisteredVisitorCannotCheckInOnAnotherDay` | `preRegistrationIsBoundToItsDate` | — |
| AC-2.3 | Card held on campus cannot be reissued | — | `cardHeldByAVisitorOnCampusCannotBeIssuedAgain` | — |
| AC-3.1 | Walk-in `AWAITING_HOST`, arrival recorded, host notified on 3 channels | `walkInWaitsForTheHostAndRecordsArrival` | `walkInGetsAPassOnlyAfterTheHostConfirms` (in-app message; NOTIFIED audit for IN_APP, EMAIL, SMS) | ✅ |
| AC-3.2 | No card before confirmation | rejected: `checkIn from AWAITING_HOST` | `walkInGetsAPass…` (409) | ✅ (no Check in button) |
| AC-3.3 | Invalid walk-in rejected, nothing saved | — | `invalidWalkInsAreRejectedAndNothingIsSaved` | — |
| AC-4.1 | Host confirms → `APPROVED` | allowed: `confirm from AWAITING_HOST` | `walkInGetsAPass…` | ✅ |
| AC-4.2 | Host declines → no card ever | allowed `decline`; rejected `checkIn from DECLINED` | `declinedWalkInNeverGetsAPass` | — |
| AC-4.3 | Another employee cannot respond | — | `walkInGetsAPass…` (Ben → 403); `AccessControlIT` (desk, admin → 403) | — |
| AC-4.4 | Second decision refused | rejected: confirm/decline from `APPROVED`, `DECLINED`, `TIMED_OUT` | `walkInGetsAPass…` (decline after confirm → 409); `unanswered…` (confirm after timeout → 409) | — |
| AC-5.1 | Timeout after 10 min, actor `system` | `hostCannotBeTimedOutBeforeTheTimeoutHasPassed`; allowed `timeOut` | `unansweredWalkInTimesOutAndEntryIsNotGranted` (not before 10 min; after it `TIMED_OUT`; audit actor) | — |
| AC-6.1 | Check-out records departure; card no longer valid | `checkOutRecordsDeparture` | `preRegisteredVisitor…` (second check-out → 404) | ✅ |
| AC-6.2 | Unknown card → 404 | — | `preRegisteredVisitor…` | ✅ (error shown) |
| AC-7.1 | On-campus list and count | — | `visitorCheckedInLongerThan…` (listed for desk and admin) | ✅ (row *On campus*) |
| AC-7.2 | Overstay after 4 h, including previous days | `visitorIsOverstayedOnlyWhileCheckedInBeyondTheMaximumStay` (boundary) | `visitorCheckedInLongerThan…` (checked in yesterday → still listed, flagged) | — |
| AC-7.3 | Employees cannot see on-campus view | — | `AccessControlIT` (tina → 403) | — |
| AC-8.1 | Every change and notification audited | `failingChannelDoesNotStopTheOthersAndIsAudited` | `assertAudited` in both flows; timeout audit | — |
| AC-8.2 | Only admins read the audit trail; no edit/delete | — | `AccessControlIT` (admin 200, desk/tina 403) | — |
| AC-9.1 | Unauthenticated → 401 | — | `AccessControlIT` (anonymous, wrong password) | — |
| AC-9.2 | Role matrix enforced on the server | — | `AccessControlIT` (21 cases) | — |

AC-8.2's "no edit or delete" is a design property: `AuditEventRepository` exposes only `save` and a read query,
and `SecurityConfig` denies every route it does not list. It is checked in review, not by a test.

## 3. Edge cases and failure scenarios

| Scenario | Expected | Covered by |
|---|---|---|
| Host clicks Confirm twice, or responds after declining or timing out | 409, first decision stands | Unit (rejected matrix), IT |
| Another employee tries to confirm someone else's visitor | 403 | IT |
| Card number typed with spaces or lower case (` p-1 `) | Stored and matched as `P-1` | IT |
| Card still held by someone on campus | 409, visit unchanged | IT; DB partial unique index as a backstop |
| Same card checked out twice | 404 the second time | IT, E2E |
| Pre-registration for yesterday / used on another day | 409 | Unit, IT |
| Timeout job runs before 10 minutes have passed | Visit untouched | Unit, IT |
| Overstay exactly at the 4-hour boundary | Not yet overstayed; one second later it is | Unit |
| Visitor still checked in from yesterday | Still on today's list, overstayed | IT |
| One notification channel throws (e.g. SMS gateway down) | Other channels still sent; failure audited; visit saved | Unit (`NotifierTest`) |
| Blank name, missing host, or host who is not an employee | 400 / 404, nothing saved | IT |
| Blank optional phone | Stored as missing, not `""` | Unit (found in exploratory testing, §5) |
| Not signed in / wrong password | 401, no browser popup | IT; popup suppression checked manually |
| Signed-out user opens `/admin` | Sent to sign-in | Manual (§5) |
| Two guards issue the same card in the same instant | One wins, the other gets 409 | Not automated (§4); DB unique index |
| Host confirms in the same instant the timeout fires | One wins, the other gets 409 | Not automated (§4); optimistic locking |

## 4. What is not automated, and why

| Not automated | Why | How it is covered instead |
|---|---|---|
| Concurrency races (same card, confirm vs timeout) | Reproducing them reliably needs thread orchestration around commit timing; a timing-based test would be flaky. | Enforced by the database (partial unique index, `@Version`) and mapped to 409 in `ApiExceptionHandler`. Next step: a latch-based IT. |
| The 30-second scheduling itself | That is Spring's `@Scheduled`, not our logic. | The IT calls the job method directly after back-dating the visit. |
| Pre-registered flow and admin page in the browser | The exam asks for one E2E journey; both are fully covered at API level, and the UI uses the same components and patterns. More E2E tests would add run time and flakiness without new risk coverage. | IT plus manual check. |
| Frontend unit tests | The UI holds no business rules; every rule is on the server. | E2E for the critical journey, IT for the rules. |
| Real email/SMS delivery | Simulated by design (spec §7). | Channel interface plus `NotifierTest`; messages visible in the backend log. |
| Load and performance | 40–80 visits a day; the expected load is trivial (spec §8). | Listed for production (spec §12). |
| Visual layout, accessibility audit, time-zone edge at midnight Manila | Need human judgement or specialised tooling; low risk for this release. | Manual checks; form fields have labels (the E2E test uses them). |

## 5. Exploratory testing notes

**Session 1: 2026-10-15, about 20 minutes, Docker Compose stack in a browser.**
Charter: *as a guard, push the gate screen through a walk-in, and try to break it with sloppy input.*

| Tried | Found | Outcome |
|---|---|---|
| Registered a walk-in without a mobile number | The visit stored `visitorPhone: ""` instead of no value, which undermines "a record we can trust". | **Fixed:** `Visit` stores a blank phone as missing; unit test added. |
| Clicked **Check in** on an approved walk-in with the card field empty | The server correctly answered 400, but the guard saw "Invalid request content." | **Fixed:** the desk screen now says "Enter the lanyard card number for {visitor}". |
| Checked in with card `12`, checked out by the form, then tried card `12` again | Second attempt: "No visitor on campus holds card …". Correct, and the message is clear. | No change. |
| Checked out from the table row instead of the form | Works; the stale error message cleared on success. | No change. |
| Opened `/admin` after signing out | Redirected to sign-in. | No change. |
| Read the host's notifications and the backend log | In-app message present; `[SIMULATED EMAIL]` and `[SIMULATED SMS]` lines with fictional addresses. | No change. |
| Pressed Enter, and clicked immediately after typing, in the embedded browser pane | Sometimes no submit. The pane was hidden, which throttles rendering, so the button was still disabled. Not reproducible in Playwright, which types and clicks immediately. | Tool artefact, not a product bug. |

Ideas for the next session: two desk screens issuing the same card at once, restarting the backend while screens
are polling, and very long names in the table layout.
