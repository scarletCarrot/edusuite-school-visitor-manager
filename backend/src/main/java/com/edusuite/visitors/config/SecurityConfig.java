package com.edusuite.visitors.config;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.edusuite.visitors.user.AppUserRepository;

/**
 * Who may call what (docs/spec.md §2). Anything not listed is denied. Ownership rules (only the host may
 * respond) are checked in VisitService.
 */
@Configuration
class SecurityConfig {

	@Bean
	SecurityFilterChain api(HttpSecurity http) throws Exception {
		return http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/error").permitAll()
				.requestMatchers(GET, "/api/me", "/api/notifications").authenticated()
				.requestMatchers(POST, "/api/visits/pre-registrations").hasRole("EMPLOYEE")
				.requestMatchers(POST, "/api/visits/*/confirm", "/api/visits/*/decline").hasRole("EMPLOYEE")
				.requestMatchers(GET, "/api/visits/mine").hasRole("EMPLOYEE")
				.requestMatchers(GET, "/api/employees").hasRole("FRONT_DESK")
				.requestMatchers(POST, "/api/visits/walk-ins", "/api/visits/*/check-in", "/api/visits/check-out").hasRole("FRONT_DESK")
				.requestMatchers(GET, "/api/visits/today").hasAnyRole("FRONT_DESK", "ADMIN")
				.requestMatchers(GET, "/api/audit").hasRole("ADMIN")
				.anyRequest().denyAll())
			.httpBasic(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// Credentials travel in the Authorization header, never in cookies, so CSRF does not apply.
			.csrf(AbstractHttpConfigurer::disable)
			.build();
	}

	@Bean
	UserDetailsService userDetailsService(AppUserRepository users) {
		return username -> users.findByUsername(username)
			.map(user -> User.withUsername(user.getUsername())
				.password(user.getPasswordHash())
				.roles(user.getRole().name())
				.build())
			.orElseThrow(() -> new UsernameNotFoundException(username));
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
