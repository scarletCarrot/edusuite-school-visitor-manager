package com.edusuite.visitors.notification;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edusuite.visitors.audit.AuditLog;
import com.edusuite.visitors.user.AppUser;
import com.edusuite.visitors.user.AppUserRepository;
import com.edusuite.visitors.visit.Visit;

/** Sends notifications through every channel and serves a user's in-app notifications. */
@RestController
public class Notifier {

	private static final Logger log = LoggerFactory.getLogger(Notifier.class);

	private final List<NotificationChannel> channels;

	private final NotificationRepository notifications;

	private final AppUserRepository users;

	private final AuditLog audit;

	Notifier(List<NotificationChannel> channels, NotificationRepository notifications, AppUserRepository users,
			AuditLog audit) {
		this.channels = channels;
		this.notifications = notifications;
		this.users = users;
		this.audit = audit;
	}

	/** A failing channel never blocks the others or the visit; the failure is audited instead. */
	public void notify(AppUser recipient, String message, Visit visit) {
		for (NotificationChannel channel : channels) {
			String target = channel.name() + " to " + recipient.getUsername();
			try {
				channel.send(recipient, message);
				audit.record(AuditLog.SYSTEM, "NOTIFIED", visit, target);
			}
			catch (RuntimeException ex) {
				log.warn("Notification failed: {}", target, ex);
				audit.record(AuditLog.SYSTEM, "NOTIFICATION_FAILED", visit, target + ": " + ex.getMessage());
			}
		}
	}

	@GetMapping("/api/notifications")
	List<Notification> mine(Principal principal) {
		AppUser user = users.findByUsername(principal.getName()).orElseThrow();
		return notifications.findTop20ByRecipientIdOrderByIdDesc(user.getId());
	}

}
