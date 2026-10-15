package com.edusuite.visitors.audit;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Append-only record of who did what, and when. There is deliberately no way to change one. */
@Entity
public class AuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Instant occurredAt;

	private String actor;

	private String action;

	private Long visitId;

	private String details;

	protected AuditEvent() {
	}

	AuditEvent(Instant occurredAt, String actor, String action, Long visitId, String details) {
		this.occurredAt = occurredAt;
		this.actor = actor;
		this.action = action;
		this.visitId = visitId;
		this.details = details;
	}

	public Long getId() { return id; }
	public Instant getOccurredAt() { return occurredAt; }
	public String getActor() { return actor; }
	public String getAction() { return action; }
	public Long getVisitId() { return visitId; }
	public String getDetails() { return details; }

}
