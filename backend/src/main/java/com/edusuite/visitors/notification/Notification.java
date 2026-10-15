package com.edusuite.visitors.notification;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** An in-app notification shown on the recipient's screen. */
@Entity
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long recipientId;

	private String message;

	private Instant createdAt;

	protected Notification() {
	}

	Notification(Long recipientId, String message, Instant createdAt) {
		this.recipientId = recipientId;
		this.message = message;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public String getMessage() { return message; }
	public Instant getCreatedAt() { return createdAt; }

}
