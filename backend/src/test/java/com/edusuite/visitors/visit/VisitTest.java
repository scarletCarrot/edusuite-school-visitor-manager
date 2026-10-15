package com.edusuite.visitors.visit;

import static com.edusuite.visitors.visit.VisitState.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.edusuite.visitors.user.AppUser;
import com.edusuite.visitors.user.Role;

class VisitTest {

	static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
	static final Instant NOW = Instant.parse("2026-10-15T01:00:00Z");
	static final Instant LATER = NOW.plus(Duration.ofMinutes(15));
	static final Duration TIMEOUT = Duration.ofMinutes(10);
	static final AppUser HOST = new AppUser("host", "x", "Hana Host", Role.EMPLOYEE, "host@example.edu", "+639000000001");

	/** Every action, the only states it may start from, and the state it leads to (docs/spec.md §4). */
	record Transition(Consumer<Visit> action, Set<VisitState> from, VisitState to) {
	}

	static final Map<String, Transition> TRANSITIONS = Map.of(
			"confirm", new Transition(Visit::confirm, Set.of(AWAITING_HOST), APPROVED),
			"decline", new Transition(Visit::decline, Set.of(AWAITING_HOST), DECLINED),
			"timeOut", new Transition(v -> v.timeOut(LATER, TIMEOUT), Set.of(AWAITING_HOST), TIMED_OUT),
			"checkIn", new Transition(v -> v.checkIn("17", TODAY, LATER), Set.of(EXPECTED, APPROVED), CHECKED_IN),
			"checkOut", new Transition(v -> v.checkOut(LATER), Set.of(CHECKED_IN), CHECKED_OUT));

	static Stream<Arguments> allowed() {
		return combinations(true);
	}

	static Stream<Arguments> rejected() {
		return combinations(false);
	}

	static Stream<Arguments> combinations(boolean allowed) {
		return TRANSITIONS.keySet().stream()
			.flatMap(name -> Arrays.stream(VisitState.values()).map(state -> Arguments.of(state, name)))
			.filter(args -> TRANSITIONS.get(args.get()[1]).from().contains(args.get()[0]) == allowed);
	}

	@ParameterizedTest(name = "{1} from {0} is allowed")
	@MethodSource("allowed")
	void allowedTransitionsReachTheirTargetState(VisitState from, String action) {
		Visit visit = visitIn(from);
		TRANSITIONS.get(action).action().accept(visit);
		assertThat(visit.getState()).isEqualTo(TRANSITIONS.get(action).to());
	}

	@ParameterizedTest(name = "{1} from {0} is rejected")
	@MethodSource("rejected")
	void everyOtherTransitionIsRejectedAndLeavesTheVisitUnchanged(VisitState from, String action) {
		Visit visit = visitIn(from);
		assertThatThrownBy(() -> TRANSITIONS.get(action).action().accept(visit))
			.isInstanceOf(VisitRuleException.class);
		assertThat(visit.getState()).isEqualTo(from);
	}

	@Test
	void preRegistrationIsExpectedOnItsDate() {
		Visit visit = Visit.preRegister("Vic Visitor", null, "Parent meeting", HOST, TODAY.plusDays(1), TODAY, NOW);
		assertThat(visit.getState()).isEqualTo(EXPECTED);
		assertThat(visit.getType()).isEqualTo(VisitType.PRE_REGISTERED);
		assertThat(visit.getVisitDate()).isEqualTo(TODAY.plusDays(1));
		assertThat(visit.getArrivedAt()).isNull();
	}

	@Test
	void preRegistrationInThePastIsRejected() {
		assertThatThrownBy(() -> Visit.preRegister("Vic Visitor", null, "Meeting", HOST, TODAY.minusDays(1), TODAY, NOW))
			.isInstanceOf(VisitRuleException.class);
	}

	@Test
	void walkInWaitsForTheHostAndRecordsArrival() {
		Visit visit = Visit.walkIn("Wendy Walkin", "+639000000099", "Delivery", HOST, TODAY, NOW);
		assertThat(visit.getState()).isEqualTo(AWAITING_HOST);
		assertThat(visit.getType()).isEqualTo(VisitType.WALK_IN);
		assertThat(visit.getArrivedAt()).isEqualTo(NOW);
		assertThat(visit.getPassNumber()).isNull();
	}

	@Test
	void blankOptionalPhoneIsStoredAsMissing() {
		assertThat(Visit.walkIn("Wendy Walkin", " ", "Delivery", HOST, TODAY, NOW).getVisitorPhone()).isNull();
	}

	@Test
	void preRegisteredVisitorCannotCheckInOnAnotherDay() {
		Visit visit = Visit.preRegister("Vic Visitor", null, "Meeting", HOST, TODAY.plusDays(1), TODAY, NOW);
		assertThatThrownBy(() -> visit.checkIn("17", TODAY, NOW)).isInstanceOf(VisitRuleException.class);
		assertThat(visit.getState()).isEqualTo(EXPECTED);
	}

	@Test
	void checkInIssuesThePassAndRecordsArrivalForPreRegisteredVisitors() {
		Visit visit = visitIn(EXPECTED);
		visit.checkIn("17", TODAY, LATER);
		assertThat(visit.getPassNumber()).isEqualTo("17");
		assertThat(visit.getCheckedInAt()).isEqualTo(LATER);
		assertThat(visit.getArrivedAt()).isEqualTo(LATER);
	}

	@Test
	void checkInKeepsTheWalkInArrivalTime() {
		Visit visit = visitIn(APPROVED);
		visit.checkIn("17", TODAY, LATER);
		assertThat(visit.getArrivedAt()).isEqualTo(NOW);
		assertThat(visit.getCheckedInAt()).isEqualTo(LATER);
	}

	@Test
	void hostCannotBeTimedOutBeforeTheTimeoutHasPassed() {
		Visit visit = visitIn(AWAITING_HOST);
		assertThatThrownBy(() -> visit.timeOut(NOW.plus(Duration.ofMinutes(9)), TIMEOUT))
			.isInstanceOf(VisitRuleException.class);
		assertThat(visit.getState()).isEqualTo(AWAITING_HOST);
	}

	@Test
	void checkOutRecordsDeparture() {
		Visit visit = visitIn(CHECKED_IN);
		Instant departure = LATER.plus(Duration.ofHours(1));
		visit.checkOut(departure);
		assertThat(visit.getCheckedOutAt()).isEqualTo(departure);
	}

	@Test
	void visitorIsOverstayedOnlyWhileCheckedInBeyondTheMaximumStay() {
		Duration maxStay = Duration.ofHours(4);
		Visit visit = visitIn(CHECKED_IN); // checked in at NOW
		assertThat(visit.isOverstayed(NOW.plus(maxStay), maxStay)).isFalse();
		assertThat(visit.isOverstayed(NOW.plus(maxStay).plusSeconds(1), maxStay)).isTrue();
		visit.checkOut(NOW.plus(Duration.ofHours(5)));
		assertThat(visit.isOverstayed(NOW.plus(Duration.ofHours(6)), maxStay)).isFalse();
	}

	/** Builds a visit in the given state by walking the real transitions. */
	static Visit visitIn(VisitState state) {
		if (state == EXPECTED) {
			return Visit.preRegister("Vic Visitor", null, "Meeting", HOST, TODAY, TODAY, NOW);
		}
		Visit visit = Visit.walkIn("Wendy Walkin", null, "Delivery", HOST, TODAY, NOW);
		switch (state) {
			case APPROVED -> visit.confirm();
			case DECLINED -> visit.decline();
			case TIMED_OUT -> visit.timeOut(LATER, TIMEOUT);
			case CHECKED_IN -> {
				visit.confirm();
				visit.checkIn("17", TODAY, NOW);
			}
			case CHECKED_OUT -> {
				visit.confirm();
				visit.checkIn("17", TODAY, NOW);
				visit.checkOut(NOW);
			}
			default -> {
			}
		}
		return visit;
	}

}
