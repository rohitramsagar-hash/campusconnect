package com.campusconnect;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end API tests against an in-memory H2 database filled by DataSeeder. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CampusConnectApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private long categoryId(String token) throws Exception {
        String body = mvc.perform(get("/api/categories").header("Authorization", token))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get(0).get("id").asLong();
    }

    private JsonNode createComplaint(String token, boolean isPublic) throws Exception {
        String req = """
                {"title":"Fan not working in room 101","description":"The ceiling fan does not turn on at all.",
                 "categoryId":%d,"priority":"HIGH","location":"Room 101","anonymous":false,"isPublic":%s}
                """.formatted(categoryId(token), isPublic);
        String body = mvc.perform(post("/api/complaints").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.ticketNo", startsWith("CC-")))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private void setStatus(String token, long id, String status, String note, int expected) throws Exception {
        mvc.perform(patch("/api/complaints/" + id + "/status").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\",\"note\":" + (note == null ? "null" : "\"" + note + "\"") + "}"))
                .andExpect(status().is(expected));
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void protectedEndpointsNeedLogin() throws Exception {
        mvc.perform(get("/api/complaints"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mvc.perform(get("/api/complaints").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid")));
    }

    @Test
    void wrongPasswordGivesSameMessageAsUnknownEmail() throws Exception {
        for (String email : new String[]{"student@campus.edu", "nobody@campus.edu"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"Wrong@1234\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    void registerValidatesAndRejectsDuplicates() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details", hasSize(3)));

        String ok = "{\"name\":\"New Student\",\"email\":\"new@campus.edu\",\"password\":\"Hello1234\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("STUDENT"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void fullComplaintLifecycle() throws Exception {
        String student = login("student@campus.edu", "Student@123");
        String admin = login("admin@campus.edu", "Admin@123");
        String ravi = login("ravi.staff@campus.edu", "Staff@123");
        String priya = login("priya.staff@campus.edu", "Staff@123");
        long id = createComplaint(student, true).get("id").asLong();

        // staff can't act before being assigned
        setStatus(ravi, id, "IN_PROGRESS", null, 403);

        String staffList = mvc.perform(get("/api/users?role=STAFF").header("Authorization", admin))
                .andReturn().getResponse().getContentAsString();
        long raviId = -1;
        for (JsonNode u : json.readTree(staffList)) {
            if (u.get("email").asText().startsWith("ravi")) {
                raviId = u.get("id").asLong();
            }
        }
        mvc.perform(patch("/api/complaints/" + id + "/assign").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"staffId\":" + raviId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.name").value("Ravi Kumar"));

        setStatus(priya, id, "IN_PROGRESS", null, 403);        // not her complaint
        setStatus(ravi, id, "RESOLVED", null, 409);            // must go through IN_PROGRESS
        setStatus(ravi, id, "IN_PROGRESS", null, 200);
        setStatus(ravi, id, "RESOLVED", "Fan replaced", 200);
        setStatus(ravi, id, "CLOSED", null, 403);              // only the reporter confirms
        setStatus(student, id, "IN_PROGRESS", null, 400);      // reopening needs a reason
        setStatus(student, id, "CLOSED", null, 200);
        setStatus(student, id, "OPEN", null, 409);             // closed is final

        mvc.perform(get("/api/complaints/" + id).header("Authorization", student))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.resolvedAt", notNullValue()))
                .andExpect(jsonPath("$.history", hasSize(5)));
    }

    @Test
    void privateComplaintsAreHiddenFromOtherStudents() throws Exception {
        String ananya = login("student@campus.edu", "Student@123");
        String rahul = login("rahul@campus.edu", "Student@123");
        long id = createComplaint(ananya, false).get("id").asLong();

        mvc.perform(get("/api/complaints/" + id).header("Authorization", rahul))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/complaints/" + id).header("Authorization", login("admin@campus.edu", "Admin@123")))
                .andExpect(status().isOk());
    }

    @Test
    void upvotesAreOnePerUserAndNotOnOwnComplaint() throws Exception {
        String ananya = login("student@campus.edu", "Student@123");
        String rahul = login("rahul@campus.edu", "Student@123");
        long id = createComplaint(ananya, true).get("id").asLong();

        mvc.perform(post("/api/complaints/" + id + "/upvote").header("Authorization", ananya))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/complaints/" + id + "/upvote").header("Authorization", rahul))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvoteCount").value(1));
        mvc.perform(post("/api/complaints/" + id + "/upvote").header("Authorization", rahul))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/complaints/" + id + "/upvote").header("Authorization", rahul))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvoteCount").value(0));
    }

    @Test
    void studentsCannotUseAdminEndpoints() throws Exception {
        String student = login("student@campus.edu", "Student@123");
        mvc.perform(get("/api/users").header("Authorization", student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(post("/api/categories").header("Authorization", student)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Gym\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardIsScopedByRole() throws Exception {
        mvc.perform(get("/api/dashboard").header("Authorization", login("admin@campus.edu", "Admin@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("ALL"))
                .andExpect(jsonPath("$.total").value(8));
        mvc.perform(get("/api/dashboard").header("Authorization", login("student@campus.edu", "Student@123")))
                .andExpect(jsonPath("$.scope").value("OWN"))
                .andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void badInputGivesCleanErrors() throws Exception {
        String student = login("student@campus.edu", "Student@123");
        mvc.perform(post("/api/complaints").header("Authorization", student)
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
        mvc.perform(get("/api/complaints?status=BANANA").header("Authorization", student))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
        mvc.perform(get("/api/complaints/999999").header("Authorization", student))
                .andExpect(status().isNotFound());
    }
}
