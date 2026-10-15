package com.edusuite.visitors.visit;

import static com.edusuite.visitors.visit.VisitState.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.edusuite.visitors.user.AppUser;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

/**
 * A visit and its lifecycle (docs/spec.md §4). State only changes through the methods below, each of which
 * rejects the transition with {@link VisitRuleException} unless the visit is in an allowed state.
 */
@Entity
public class Visit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private VisitType type;

	@Enumerated(EnumType.STRING)
	private VisitState state;

	private String visitorName;

	private String visitorPhone;

	private String purpose;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private AppUser host;

	private LocalDate visitDate;

	private String passNumber;

	private Instant createdAt;

	private Instant arrivedAt;

	private Instant checkedInAt;

	private Instant checkedOutAt;

	@Version
	private long version;

	protected Visit() {
	}

	private Visit(VisitType type, VisitState state, String visitorName, String visitorPhone, String purpose,
			AppUser host, LocalDate visitDate, Instant now) {
		this.type = type;
		this.state = state;
		this.visitorName = visitorName;
		this.visitorPhone = visitorPhone;
		this.purpose = purpose;
		this.host = host;
		this.visitDate = visitDate;
		this.createdAt = now;
	}

	public static Visit preRegister(String visitorName, String visitorPhone, String purpose, AppUser host,
			LocalDate visitDate, LocalDate today, Instant now) {
		if (visitDate.isBefore(today)) {
			throw new VisitRuleException("Visit date cannot be in the past");
		}
		return new Visit(VisitType.PRE_REGISTERED, EXPECTED, visitorName, visitorPhone, purpose, host, visitDate, now);
	}

	public static Visit walkIn(String visitorName, String visitorPhone, String purpose, AppUser host,
			LocalDate today, Instant now) {
		Visit visit = new Visit(VisitType.WALK_IN, AWAITING_HOST, visitorName, visitorPhone, purpose, host, today, now);
		visit.arrivedAt = now;
		return visit;
	}

	public void confirm() {
		require("confirm", AWAITING_HOST);
		state = APPROVED;
	}

	public void decline() {
		require("decline", AWAITING_HOST);
		state = DECLINED;
	}

	public void timeOut(Instant now, Duration timeout) {
		require("time out", AWAITING_HOST);
		if (createdAt.plus(timeout).isAfter(now)) {
			throw new VisitRuleException("The host still has time to respond");
		}
		state = TIMED_OUT;
	}

	public void checkIn(String passNumber, LocalDate today, Instant now) {
		require("check in", EXPECTED, APPROVED);
		if (state == EXPECTED && !visitDate.equals(today)) {
			throw new VisitRuleException("This visit is expected on " + visitDate + "; register a walk-in instead");
		}
		this.passNumber = passNumber;
		this.checkedInAt = now;
		if (arrivedAt == null) {
			arrivedAt = now;
		}
		state = CHECKED_IN;
	}

	public void checkOut(Instant now) {
		require("check out", CHECKED_IN);
		checkedOutAt = now;
		state = CHECKED_OUT;
	}

	public boolean isOverstayed(Instant now, Duration maxStay) {
		return state == CHECKED_IN && checkedInAt.plus(maxStay).isBefore(now);
	}

	private void require(String action, VisitState... allowed) {
		if (!List.of(allowed).contains(state)) {
			throw new VisitRuleException("Cannot " + action + " a visit that is " + state);
		}
	}

	public Long getId() { return id; }
	public VisitType getType() { return type; }
	public VisitState getState() { return state; }
	public String getVisitorName() { return visitorName; }
	public String getVisitorPhone() { return visitorPhone; }
	public String getPurpose() { return purpose; }
	public AppUser getHost() { return host; }
	public LocalDate getVisitDate() { return visitDate; }
	public String getPassNumber() { return passNumber; }
	public Instant getArrivedAt() { return arrivedAt; }
	public Instant getCheckedInAt() { return checkedInAt; }
	public Instant getCheckedOutAt() { return checkedOutAt; }

}
