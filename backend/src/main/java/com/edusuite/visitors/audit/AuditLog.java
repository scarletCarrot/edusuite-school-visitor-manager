package com.edusuite.visitors.audit;

import java.time.Clock;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edusuite.visitors.visit.Visit;

/** Writes audit events and serves the latest ones to admins. */
@RestController
public class AuditLog {

	public static final String SYSTEM = "system";

	private final AuditEventRepository events;

	private final Clock clock;

	AuditLog(AuditEventRepository events, Clock clock) {
		this.events = events;
		this.clock = clock;
	}

	/** Joins the caller's transaction, so a change and its audit event are saved together. */
	public void record(String actor, String action, Visit visit, String details) {
		events.save(new AuditEvent(clock.instant(), actor, action, visit.getId(), details));
	}

	@GetMapping("/api/audit")
	List<AuditEvent> latest() {
		return events.findTop200ByOrderByIdDesc();
	}

}
