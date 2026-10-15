package com.edusuite.visitors.user;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String username;

	private String passwordHash;

	private String fullName;

	@Enumerated(EnumType.STRING)
	private Role role;

	private String email;

	private String phone;

	protected AppUser() {
	}

	public AppUser(String username, String passwordHash, String fullName, Role role, String email, String phone) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.role = role;
		this.email = email;
		this.phone = phone;
	}

	public Long getId() { return id; }
	public String getUsername() { return username; }
	public String getPasswordHash() { return passwordHash; }
	public String getFullName() { return fullName; }
	public Role getRole() { return role; }
	public String getEmail() { return email; }
	public String getPhone() { return phone; }

}
