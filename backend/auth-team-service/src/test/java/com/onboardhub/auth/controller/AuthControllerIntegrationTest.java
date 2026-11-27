package com.onboardhub.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onboardhub.auth.entity.User;
import com.onboardhub.auth.enums.Role;
import com.onboardhub.auth.config.AdminBootstrapProperties;
import com.onboardhub.auth.config.AdminBootstrapRunner;
import com.onboardhub.auth.repository.UserRepository;
import com.onboardhub.auth.security.JwtService;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @BeforeEach
    void clearUsers() {
        users.deleteAll();
    }

    @Test
    void publicRegistrationAcceptsEmployeeAndRejectsOtherRolesWithoutCreatingUsers() throws Exception {
        register("person@example.com", null).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("EMPLOYEE"));
        register("person2@example.com", "EMPLOYEE").andExpect(status().isCreated());
        for (String role : new String[] {"HR", "ADMIN", "IT"}) {
            register("blocked-" + role.toLowerCase(Locale.ROOT) + "@example.com", role)
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value(
                            "Only administrators can create HR, IT or admin accounts"));
        }
        org.assertj.core.api.Assertions.assertThat(users.count()).isEqualTo(2);
    }

    @Test
    void registrationReportsDuplicateAndInvalidBody() throws Exception {
        register("person@example.com", null).andExpect(status().isCreated());
        register("person@example.com", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void loginReturnsTokenAndHidesWhetherEmailOrPasswordWasWrong() throws Exception {
        register("person@example.com", null).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("person@example.com", "Password@123")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("person@example.com", "incorrect")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid email or password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown@example.com", "incorrect")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void adminCreatesHrAndItUsersWhoCanLogIn() throws Exception {
        String token = createAdminAndToken();
        createUser(token, "hr@example.com", "HR").andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("HR"));
        createUser(token, "it@example.com", "IT").andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("IT"));
        login("hr@example.com").andExpect(status().isOk()).andExpect(jsonPath("$.role").value("HR"));
        login("it@example.com").andExpect(status().isOk()).andExpect(jsonPath("$.role").value("IT"));
    }

    @Test
    void userCreationRejectsUnauthenticatedNonAdminDuplicateAndInvalidRequests() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(createJson("first@example.com", "HR")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        String admin = createAdminAndToken();
        createUser(admin, "member@example.com", "EMPLOYEE").andExpect(status().isCreated());
        createUser(admin, "member@example.com", "IT").andExpect(status().isConflict());
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        for (Role role : new Role[] {Role.HR, Role.IT, Role.EMPLOYEE}) {
            String token = tokenFor(role);
            mvc.perform(post("/api/users").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(createJson("denied@example.com", "HR")))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        }
    }

    @Test
    void adminListsNewestFirstAndCanFilterByRole() throws Exception {
        String admin = createAdminAndToken();
        createUser(admin, "first@example.com", "HR").andExpect(status().isCreated());
        Thread.sleep(10);
        createUser(admin, "second@example.com", "HR").andExpect(status().isCreated());
        createUser(admin, "it@example.com", "IT").andExpect(status().isCreated());
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].email").value("it@example.com"))
                .andExpect(jsonPath("$[1].email").value("second@example.com"));
        mvc.perform(get("/api/users").param("role", "HR").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("second@example.com"));
        mvc.perform(get("/api/users").param("role", "NOPE").header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + tokenFor(Role.HR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void currentUserReturnsCaller() throws Exception {
        String token = tokenFor(Role.EMPLOYEE);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("employee@example.com"));
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerAndHealthArePublic() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void invalidBearerTokenIsAStandardUnauthorizedResponse() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer tampered.token.value"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    void bootstrappedAdministratorCanLogIn() throws Exception {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                new AdminBootstrapProperties("Bootstrap Admin", "BOOTSTRAP@example.com", "Password@123"),
                users, passwordEncoder);
        runner.run(new DefaultApplicationArguments(new String[0]));
        login("bootstrap@example.com").andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
    }

    private org.springframework.test.web.servlet.ResultActions register(String email, String role) throws Exception {
        String rolePart = role == null ? "" : ",\"role\":\"" + role + "\"";
        String body = "{\"fullName\":\"Test Person\",\"email\":\"" + email
                + "\",\"password\":\"Password@123\"" + rolePart + "}";
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, "Password@123")));
    }

    private org.springframework.test.web.servlet.ResultActions createUser(String token, String email, String role)
            throws Exception {
        return mvc.perform(post("/api/users").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(createJson(email, role)));
    }

    private String createAdminAndToken() {
        User admin = users.saveAndFlush(new User("Administrator", "admin@example.com",
                passwordEncoder.encode("Password@123"), Role.ADMIN));
        return jwtService.generateToken(admin);
    }

    private String tokenFor(Role role) {
        String email = role.name().toLowerCase(Locale.ROOT) + "@example.com";
        User user = users.saveAndFlush(new User(role.name(), email, passwordEncoder.encode("Password@123"), role));
        return jwtService.generateToken(user);
    }

    private String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private String createJson(String email, String role) {
        return "{\"fullName\":\"Test User\",\"email\":\"" + email
                + "\",\"password\":\"Password@123\",\"role\":\"" + role + "\"}";
    }
}
