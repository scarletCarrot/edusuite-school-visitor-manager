package com.edusuite.visitors.notification;

import com.edusuite.visitors.user.AppUser;

/** One way of reaching a person. Adding a real email or SMS provider means adding one implementation. */
public interface NotificationChannel {

	String name();

	void send(AppUser recipient, String message);

}
