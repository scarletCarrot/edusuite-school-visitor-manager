package com.edusuite.visitors.user;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class UserController {

	record Me(Long id, String username, String fullName, Role role) {
	}

	record Host(Long id, String fullName) {
	}

	private final AppUserRepository users;

	UserController(AppUserRepository users) {
		this.users = users;
	}

	@GetMapping("/me")
	Me me(Principal principal) {
		AppUser user = users.findByUsername(principal.getName()).orElseThrow();
		return new Me(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
	}

	/** Employees a walk-in can name as host; names only, no contact details. */
	@GetMapping("/employees")
	List<Host> employees() {
		return users.findByRoleOrderByFullName(Role.EMPLOYEE).stream()
			.map(user -> new Host(user.getId(), user.getFullName()))
			.toList();
	}

}
