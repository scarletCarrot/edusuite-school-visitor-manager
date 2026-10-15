package com.edusuite.visitors.config;

import static com.edusuite.visitors.user.Role.*;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.edusuite.visitors.user.AppUser;
import com.edusuite.visitors.user.AppUserRepository;

/** Seeds fictional accounts into an empty database. All passwords are "demo1234" (see README). */
@Component
class DemoUsers implements ApplicationRunner {

	private final AppUserRepository users;

	private final PasswordEncoder encoder;

	DemoUsers(AppUserRepository users, PasswordEncoder encoder) {
		this.users = users;
		this.encoder = encoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (users.count() > 0) {
			return;
		}
		String password = encoder.encode("demo1234");
		users.saveAll(List.of(
				new AppUser("admin", password, "Paula Principal", ADMIN, "principal@example.edu", "+639000000001"),
				new AppUser("desk", password, "Gary Guard", FRONT_DESK, "gate@example.edu", "+639000000002"),
				new AppUser("tina", password, "Tina Teacher", EMPLOYEE, "tina.teacher@example.edu", "+639000000003"),
				new AppUser("ben", password, "Ben Bursar", EMPLOYEE, "ben.bursar@example.edu", "+639000000004"),
				new AppUser("office", password, "Admin Office", EMPLOYEE, "office@example.edu", "+639000000005")));
	}

}
