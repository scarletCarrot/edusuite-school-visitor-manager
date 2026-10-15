package com.edusuite.visitors.visit;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/visits")
class VisitController {

	record PreRegistration(@NotBlank @Size(max = 100) String visitorName, @Size(max = 30) String visitorPhone,
			@NotBlank @Size(max = 200) String purpose, @NotNull LocalDate visitDate) {
	}

	record WalkIn(@NotBlank @Size(max = 100) String visitorName, @Size(max = 30) String visitorPhone,
			@NotBlank @Size(max = 200) String purpose, @NotNull Long hostId) {
	}

	record Pass(@NotBlank @Size(max = 20) String passNumber) {
	}

	private final VisitService visits;

	VisitController(VisitService visits) {
		this.visits = visits;
	}

	@PostMapping("/pre-registrations")
	@ResponseStatus(HttpStatus.CREATED)
	VisitView preRegister(Principal user, @Valid @RequestBody PreRegistration body) {
		return visits.preRegister(user.getName(), body.visitorName(), body.visitorPhone(), body.purpose(), body.visitDate());
	}

	@PostMapping("/walk-ins")
	@ResponseStatus(HttpStatus.CREATED)
	VisitView registerWalkIn(Principal user, @Valid @RequestBody WalkIn body) {
		return visits.registerWalkIn(user.getName(), body.visitorName(), body.visitorPhone(), body.purpose(), body.hostId());
	}

	@PostMapping("/{id}/confirm")
	VisitView confirm(Principal user, @PathVariable long id) {
		return visits.respond(user.getName(), id, true);
	}

	@PostMapping("/{id}/decline")
	VisitView decline(Principal user, @PathVariable long id) {
		return visits.respond(user.getName(), id, false);
	}

	@PostMapping("/{id}/check-in")
	VisitView checkIn(Principal user, @PathVariable long id, @Valid @RequestBody Pass body) {
		return visits.checkIn(user.getName(), id, body.passNumber());
	}

	@PostMapping("/check-out")
	VisitView checkOut(Principal user, @Valid @RequestBody Pass body) {
		return visits.checkOut(user.getName(), body.passNumber());
	}

	@GetMapping("/today")
	List<VisitView> today() {
		return visits.forToday();
	}

	@GetMapping("/mine")
	List<VisitView> mine(Principal user) {
		return visits.hostedBy(user.getName());
	}

}
