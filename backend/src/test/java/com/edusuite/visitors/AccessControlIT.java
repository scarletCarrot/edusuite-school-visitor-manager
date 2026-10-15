package com.edusuite.visitors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** The role matrix of docs/spec.md §2, enforced by the server whatever the UI shows. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccessControlIT {

	@Autowired
	MockMvc mvc;

	@ParameterizedTest(name = "{0} {1} {2} -> {3}")
	@CsvSource({
			"anonymous,      GET,  /api/visits/today,             401",
			"anonymous,      GET,  /api/me,                       401",
			"wrong-password, GET,  /api/me,                       401",
			"tina,           GET,  /api/me,                       200",
			"desk,           GET,  /api/visits/today,             200",
			"admin,          GET,  /api/visits/today,             200",
			"tina,           GET,  /api/visits/today,             403",
			"desk,           POST, /api/visits/pre-registrations, 403",
			"admin,          POST, /api/visits/pre-registrations, 403",
			"tina,           POST, /api/visits/walk-ins,          403",
			"admin,          POST, /api/visits/walk-ins,          403",
			"desk,           POST, /api/visits/1/confirm,         403",
			"admin,          POST, /api/visits/1/decline,         403",
			"tina,           POST, /api/visits/1/check-in,        403",
			"tina,           POST, /api/visits/check-out,         403",
			"admin,          POST, /api/visits/check-out,         403",
			"tina,           GET,  /api/employees,                403",
			"desk,           GET,  /api/visits/mine,              403",
			"tina,           GET,  /api/audit,                    403",
			"desk,           GET,  /api/audit,                    403",
			"admin,          GET,  /api/audit,                    200" })
	void rolesAreEnforcedOnTheServer(String user, String method, String path, int expectedStatus) throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}");
		if (user.equals("wrong-password")) {
			request.with(httpBasic("tina", "wrong"));
		}
		else if (!user.equals("anonymous")) {
			request.with(httpBasic(user, "demo1234"));
		}
		mvc.perform(request).andExpect(status().is(expectedStatus));
	}

}
