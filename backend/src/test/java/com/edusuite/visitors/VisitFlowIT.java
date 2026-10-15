package com.edusuite.visitors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.edusuite.visitors.visit.VisitService;
import com.jayway.jsonpath.JsonPath;

/** The pre-registered and walk-in flows through the real HTTP API, security and PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VisitFlowIT {

	static final RequestPostProcessor DESK = httpBasic("desk", "demo1234");
	static final RequestPostProcessor TINA = httpBasic("tina", "demo1234");
	static final RequestPostProcessor BEN = httpBasic("ben", "demo1234");
	static final RequestPostProcessor ADMIN = httpBasic("admin", "demo1234");

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	VisitService visitService;

	@Autowired
	Clock clock;

	@Test
	void preRegisteredVisitorIsCheckedInAndOutWithALanyardCard() throws Exception {
		long id = create("/api/visits/pre-registrations", TINA, """
				{"visitorName": "Pia Prereg", "purpose": "Parent meeting", "visitDate": "%s"}"""
			.formatted(LocalDate.now(clock)));
		mvc.perform(get("/api/visits/today").with(DESK))
			.andExpect(jsonPath("$[?(@.id == %d)].state".formatted(id)).value("EXPECTED"));

		post("/api/visits/" + id + "/check-in", DESK, pass(" p-1 "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.state").value("CHECKED_IN"))
			.andExpect(jsonPath("$.passNumber").value("P-1"))
			.andExpect(jsonPath("$.arrivedAt").isNotEmpty());
		mvc.perform(get("/api/notifications").with(TINA))
			.andExpect(jsonPath("$[*].message", hasItem(containsString("Pia Prereg has checked in"))));

		post("/api/visits/check-out", DESK, pass("P-1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.state").value("CHECKED_OUT"))
			.andExpect(jsonPath("$.checkedOutAt").isNotEmpty());
		post("/api/visits/check-out", DESK, pass("P-1")).andExpect(status().isNotFound());

		assertAudited(id, "PRE_REGISTERED", "CHECKED_IN", "CHECKED_OUT");
	}

	@Test
	void walkInGetsAPassOnlyAfterTheHostConfirms() throws Exception {
		long id = walkIn("Wally Walkin");
		mvc.perform(get("/api/notifications").with(TINA))
			.andExpect(jsonPath("$[*].message", hasItem(containsString("Walk-in visitor Wally Walkin"))));

		post("/api/visits/" + id + "/check-in", DESK, pass("W-1")).andExpect(status().isConflict());
		post("/api/visits/" + id + "/confirm", BEN, "").andExpect(status().isForbidden());
		post("/api/visits/" + id + "/confirm", TINA, "").andExpect(jsonPath("$.state").value("APPROVED"));
		post("/api/visits/" + id + "/decline", TINA, "").andExpect(status().isConflict());
		post("/api/visits/" + id + "/check-in", DESK, pass("W-1")).andExpect(jsonPath("$.state").value("CHECKED_IN"));
		post("/api/visits/check-out", DESK, pass("W-1")).andExpect(jsonPath("$.state").value("CHECKED_OUT"));

		assertAudited(id, "WALK_IN_REGISTERED", "NOTIFIED", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT");
		mvc.perform(get("/api/audit").with(ADMIN))
			.andExpect(jsonPath("$[?(@.visitId == %d && @.action == 'NOTIFIED')].details".formatted(id),
					hasItems("IN_APP to tina", "EMAIL to tina", "SMS to tina")));
	}

	@Test
	void declinedWalkInNeverGetsAPass() throws Exception {
		long id = walkIn("Dina Declined");
		post("/api/visits/" + id + "/decline", TINA, "").andExpect(jsonPath("$.state").value("DECLINED"));
		post("/api/visits/" + id + "/check-in", DESK, pass("D-1")).andExpect(status().isConflict());
	}

	@Test
	void unansweredWalkInTimesOutAndEntryIsNotGranted() throws Exception {
		long id = walkIn("Tom Timeout");
		visitService.timeOutUnanswered();
		assertState(id, "AWAITING_HOST");

		jdbc.update("update visit set created_at = created_at - interval '11 minutes' where id = ?", id);
		visitService.timeOutUnanswered();
		assertState(id, "TIMED_OUT");

		post("/api/visits/" + id + "/confirm", TINA, "").andExpect(status().isConflict());
		mvc.perform(get("/api/audit").with(ADMIN))
			.andExpect(jsonPath("$[?(@.visitId == %d && @.action == 'TIMED_OUT')].actor".formatted(id)).value("system"));
	}

	@Test
	void cardHeldByAVisitorOnCampusCannotBeIssuedAgain() throws Exception {
		long first = walkIn("Ana Uno");
		post("/api/visits/" + first + "/confirm", TINA, "");
		post("/api/visits/" + first + "/check-in", DESK, pass("C-1")).andExpect(status().isOk());

		long second = walkIn("Bea Dos");
		post("/api/visits/" + second + "/confirm", TINA, "");
		post("/api/visits/" + second + "/check-in", DESK, pass("c-1")).andExpect(status().isConflict());
		assertState(second, "APPROVED");
	}

	@Test
	void preRegistrationIsBoundToItsDate() throws Exception {
		LocalDate today = LocalDate.now(clock);
		String json = """
				{"visitorName": "Dan Date", "purpose": "Interview", "visitDate": "%s"}""";
		post("/api/visits/pre-registrations", TINA, json.formatted(today.minusDays(1))).andExpect(status().isConflict());

		long id = create("/api/visits/pre-registrations", TINA, json.formatted(today.plusDays(1)));
		post("/api/visits/" + id + "/check-in", DESK, pass("F-1")).andExpect(status().isConflict());
	}

	@Test
	void invalidWalkInsAreRejectedAndNothingIsSaved() throws Exception {
		Integer before = jdbc.queryForObject("select count(*) from visit", Integer.class);
		post("/api/visits/walk-ins", DESK, """
				{"visitorName": " ", "purpose": "Delivery", "hostId": %d}""".formatted(userId("tina")))
			.andExpect(status().isBadRequest());
		post("/api/visits/walk-ins", DESK, """
				{"visitorName": "Nina Nohost", "purpose": "Delivery", "hostId": %d}""".formatted(userId("desk")))
			.andExpect(status().isNotFound());
		assertThat(jdbc.queryForObject("select count(*) from visit", Integer.class)).isEqualTo(before);
	}

	@Test
	void visitorCheckedInLongerThanTheMaximumStayIsFlaggedOverstayed() throws Exception {
		long id = walkIn("Olga Overstay");
		post("/api/visits/" + id + "/confirm", TINA, "");
		post("/api/visits/" + id + "/check-in", DESK, pass("O-1"));
		mvc.perform(get("/api/visits/today").with(DESK))
			.andExpect(jsonPath("$[?(@.id == %d)].overstayed".formatted(id)).value(false));

		jdbc.update("update visit set checked_in_at = checked_in_at - interval '5 hours' where id = ?", id);
		mvc.perform(get("/api/visits/today").with(ADMIN))
			.andExpect(jsonPath("$[?(@.id == %d)].overstayed".formatted(id)).value(true));
	}

	long walkIn(String visitorName) throws Exception {
		return create("/api/visits/walk-ins", DESK, """
				{"visitorName": "%s", "purpose": "Delivery", "hostId": %d}""".formatted(visitorName, userId("tina")));
	}

	long create(String path, RequestPostProcessor user, String json) throws Exception {
		String body = post(path, user, json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	ResultActions post(String path, RequestPostProcessor user, String json) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.post(path).with(user).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	static String pass(String passNumber) {
		return "{\"passNumber\": \"" + passNumber + "\"}";
	}

	long userId(String username) {
		return jdbc.queryForObject("select id from app_user where username = ?", Long.class, username);
	}

	void assertState(long id, String state) {
		assertThat(jdbc.queryForObject("select state from visit where id = ?", String.class, id)).isEqualTo(state);
	}

	void assertAudited(long id, String... actions) throws Exception {
		mvc.perform(get("/api/audit").with(ADMIN))
			.andExpect(jsonPath("$[?(@.visitId == %d)].action".formatted(id), hasItems(actions)));
	}

}
