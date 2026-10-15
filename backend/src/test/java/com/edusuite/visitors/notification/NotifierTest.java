package com.edusuite.visitors.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import com.edusuite.visitors.audit.AuditLog;
import com.edusuite.visitors.user.AppUser;
import com.edusuite.visitors.user.Role;

class NotifierTest {

	static final AppUser HOST = new AppUser("host", "x", "Hana Host", Role.EMPLOYEE, "host@example.edu", "+639000000001");

	@Test
	void failingChannelDoesNotStopTheOthersAndIsAudited() {
		AuditLog audit = mock(AuditLog.class);
		List<String> delivered = new ArrayList<>();
		Notifier notifier = new Notifier(List.of(
				channel("SMS", (recipient, message) -> { throw new IllegalStateException("gateway down"); }),
				channel("EMAIL", (recipient, message) -> delivered.add(message))), null, null, audit);

		notifier.notify(HOST, "Visitor at the gate", null);

		assertThat(delivered).containsExactly("Visitor at the gate");
		verify(audit).record(eq("system"), eq("NOTIFICATION_FAILED"), any(), eq("SMS to host: gateway down"));
		verify(audit).record(eq("system"), eq("NOTIFIED"), any(), eq("EMAIL to host"));
	}

	static NotificationChannel channel(String name, BiConsumer<AppUser, String> send) {
		return new NotificationChannel() {
			public String name() { return name; }
			public void send(AppUser recipient, String message) { send.accept(recipient, message); }
		};
	}

}
