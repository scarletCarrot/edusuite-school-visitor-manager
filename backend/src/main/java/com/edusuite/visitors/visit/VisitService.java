package com.edusuite.visitors.visit;

import static com.edusuite.visitors.visit.VisitState.AWAITING_HOST;
import static com.edusuite.visitors.visit.VisitState.CHECKED_IN;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edusuite.visitors.VisitorProperties;
import com.edusuite.visitors.audit.AuditLog;
import com.edusuite.visitors.notification.Notifier;
import com.edusuite.visitors.user.AppUser;
import com.edusuite.visitors.user.AppUserRepository;
import com.edusuite.visitors.user.Role;

/**
 * Use cases of the visit lifecycle. Each one loads the visit, checks ownership, lets {@link Visit} apply the rule,
 * and records the audit event in the same transaction. Role checks live in SecurityConfig.
 */
@Service
@Transactional
public class VisitService {

	private final VisitRepository visits;

	private final AppUserRepository users;

	private final Notifier notifier;

	private final AuditLog audit;

	private final Clock clock;

	private final VisitorProperties properties;

	VisitService(VisitRepository visits, AppUserRepository users, Notifier notifier, AuditLog audit, Clock clock,
			VisitorProperties properties) {
		this.visits = visits;
		this.users = users;
		this.notifier = notifier;
		this.audit = audit;
		this.clock = clock;
		this.properties = properties;
	}

	public VisitView preRegister(String username, String visitorName, String visitorPhone, String purpose,
			LocalDate visitDate) {
		AppUser host = users.findByUsername(username).orElseThrow();
		Visit visit = visits.save(Visit.preRegister(visitorName, visitorPhone, purpose, host, visitDate, currentDate(), clock.instant()));
		audit.record(username, "PRE_REGISTERED", visit, visitorName + " expected on " + visitDate);
		return view(visit);
	}

	public VisitView registerWalkIn(String username, String visitorName, String visitorPhone, String purpose,
			long hostId) {
		AppUser host = users.findById(hostId)
			.filter(user -> user.getRole() == Role.EMPLOYEE)
			.orElseThrow(() -> new NoSuchElementException("Host " + hostId + " is not an employee"));
		Visit visit = visits.save(Visit.walkIn(visitorName, visitorPhone, purpose, host, currentDate(), clock.instant()));
		audit.record(username, "WALK_IN_REGISTERED", visit, visitorName + " to see " + host.getFullName());
		notifier.notify(host, "Walk-in visitor %s is at the main gate to see you (purpose: %s). Please confirm or decline in EduSuite Visitors within %d minutes."
			.formatted(visitorName, purpose, properties.hostResponseTimeout().toMinutes()), visit);
		return view(visit);
	}

	public VisitView respond(String username, long visitId, boolean confirm) {
		Visit visit = visit(visitId);
		if (!visit.getHost().getUsername().equals(username)) {
			throw new AccessDeniedException("Only the host can respond to this visit");
		}
		if (confirm) {
			visit.confirm();
		}
		else {
			visit.decline();
		}
		audit.record(username, confirm ? "CONFIRMED" : "DECLINED", visit, null);
		return view(visit);
	}

	public VisitView checkIn(String username, long visitId, String passNumber) {
		Visit visit = visit(visitId);
		String pass = normalize(passNumber);
		if (visits.existsByPassNumberAndState(pass, CHECKED_IN)) {
			throw new VisitRuleException("Card " + pass + " is already held by a visitor on campus");
		}
		visit.checkIn(pass, currentDate(), clock.instant());
		audit.record(username, "CHECKED_IN", visit, "card " + pass);
		if (visit.getType() == VisitType.PRE_REGISTERED) {
			notifier.notify(visit.getHost(), "Your visitor %s has checked in at the main gate (card %s)."
				.formatted(visit.getVisitorName(), pass), visit);
		}
		return view(visit);
	}

	public VisitView checkOut(String username, String passNumber) {
		String pass = normalize(passNumber);
		Visit visit = visits.findByPassNumberAndState(pass, CHECKED_IN)
			.orElseThrow(() -> new NoSuchElementException("No visitor on campus holds card " + pass));
		visit.checkOut(clock.instant());
		audit.record(username, "CHECKED_OUT", visit, "card " + pass);
		return view(visit);
	}

	/** Gives every unanswered walk-in a defined outcome (docs/spec.md US-5). */
	@Scheduled(fixedDelay = 30_000)
	public void timeOutUnanswered() {
		Instant now = clock.instant();
		for (Visit visit : visits.findByStateAndCreatedAtBefore(AWAITING_HOST, now.minus(properties.hostResponseTimeout()))) {
			visit.timeOut(now, properties.hostResponseTimeout());
			audit.record(AuditLog.SYSTEM, "TIMED_OUT", visit, "no response from " + visit.getHost().getFullName());
		}
	}

	/** Visits dated today, plus everyone still on campus from earlier days. */
	@Transactional(readOnly = true)
	public List<VisitView> forToday() {
		return visits.findByVisitDateOrStateOrderByIdDesc(currentDate(), CHECKED_IN).stream().map(this::view).toList();
	}

	@Transactional(readOnly = true)
	public List<VisitView> hostedBy(String username) {
		return visits.findTop50ByHostUsernameOrderByIdDesc(username).stream().map(this::view).toList();
	}

	private Visit visit(long id) {
		return visits.findById(id).orElseThrow(() -> new NoSuchElementException("Visit " + id + " not found"));
	}

	private VisitView view(Visit visit) {
		return VisitView.of(visit, visit.isOverstayed(clock.instant(), properties.maxStay()));
	}

	private LocalDate currentDate() {
		return LocalDate.now(clock);
	}

	private static String normalize(String passNumber) {
		return passNumber.trim().toUpperCase();
	}

}
