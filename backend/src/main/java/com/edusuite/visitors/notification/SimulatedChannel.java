package com.edusuite.visitors.notification;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edusuite.visitors.user.AppUser;

/** Stands in for a real provider by writing the message to the log. */
record SimulatedChannel(String name, Function<AppUser, String> address) implements NotificationChannel {

	private static final Logger log = LoggerFactory.getLogger(SimulatedChannel.class);

	@Override
	public void send(AppUser recipient, String message) {
		log.info("[SIMULATED {}] to={} message={}", name, address.apply(recipient), message);
	}

}
