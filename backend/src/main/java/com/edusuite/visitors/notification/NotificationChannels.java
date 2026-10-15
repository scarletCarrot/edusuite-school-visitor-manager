package com.edusuite.visitors.notification;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.edusuite.visitors.user.AppUser;

/** The channels every notification is sent through (docs/spec.md §7). */
@Configuration
class NotificationChannels {

	@Bean
	NotificationChannel inApp(NotificationRepository notifications, Clock clock) {
		return new NotificationChannel() {
			public String name() { return "IN_APP"; }
			public void send(AppUser recipient, String message) {
				notifications.save(new Notification(recipient.getId(), message, clock.instant()));
			}
		};
	}

	@Bean
	NotificationChannel email() {
		return new SimulatedChannel("EMAIL", AppUser::getEmail);
	}

	@Bean
	NotificationChannel sms() {
		return new SimulatedChannel("SMS", AppUser::getPhone);
	}

}
