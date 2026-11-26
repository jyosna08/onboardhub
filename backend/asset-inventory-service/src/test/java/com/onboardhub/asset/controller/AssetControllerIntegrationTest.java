package com.onboardhub.asset.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onboardhub.asset.dto.CreateAssetRequest;
import com.onboardhub.asset.entity.Asset;
import com.onboardhub.asset.enums.AssetStatus;
import com.onboardhub.asset.enums.AssetType;
import com.onboardhub.asset.enums.Role;
import com.onboardhub.asset.repository.AssetAssignmentRepository;
import com.onboardhub.asset.repository.AssetRepository;
import com.onboardhub.asset.service.AssetService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssetControllerIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AssetRepository assets;
    @Autowired private AssetAssignmentRepository assignments;
    @Autowired private AssetService assetService;
    @Value("${app.jwt.secret}") private String jwtSecret;

    @BeforeEach
    void clearDatabase() {
        assignments.deleteAll();
        assets.deleteAll();
    }

    @Test
    void itCreatesAssetsAndGetsValidationAndDuplicateErrors() throws Exception {
        createViaHttp(AssetType.LAPTOP, "SERIAL-1", "IT")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.assetTag").value(org.hamcrest.Matchers.startsWith("LAP-")));
        createViaHttp(AssetType.LAPTOP, "SERIAL-1", "IT")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        mvc.perform(post("/api/assets").header("Authorization", bearer(Role.IT))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void hrCanReadAssetsAndSummaryButCannotMutate() throws Exception {
        Asset asset = createDirect(AssetType.LAPTOP, "HR-READ-1");
        String token = bearer(Role.HR);
        mvc.perform(get("/api/assets").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/api/assets/{id}", asset.getId()).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/api/assets/{id}/history", asset.getId()).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/api/assets/summary").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(post("/api/assets").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(assetJson("HR-NEW", "LAPTOP"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/assets/{id}/status", asset.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RETIRED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/assets/{id}/assign", asset.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(assignJson("LAPTOP")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/assets/{id}/return", asset.getId()).header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotAccessAnyAssetEndpoint() throws Exception {
        Asset asset = createDirect(AssetType.LAPTOP, "EMP-READ-1");
        String token = bearer(Role.EMPLOYEE);
        mvc.perform(get("/api/assets").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/assets/{id}", asset.getId()).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/assets/{id}/history", asset.getId()).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/assets/summary").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/assets").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(assetJson("EMP-NEW", "LAPTOP"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/assets/{id}/status", asset.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RETIRED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/assets/{id}/assign", asset.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(assignJson("LAPTOP")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/assets/{id}/return", asset.getId()).header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestsReceiveStandard401() throws Exception {
        mvc.perform(get("/api/assets")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/assets/summary")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/assets").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signedTokenWithoutRequiredClaimsIsUnauthorized() throws Exception {
        String malformed = Jwts.builder().subject("23").claim("role", "IT")
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
        mvc.perform(get("/api/assets").header("Authorization", "Bearer " + malformed))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void filtersByTypeStatusSearchAndCombination() throws Exception {
        Asset laptop = createDirect(AssetType.LAPTOP, "DELL-100");
        createDirect(AssetType.LAPTOP, "DELL-200");
        Asset monitor = createDirect(AssetType.MONITOR, "ACME-300");
        assetService.updateStatus(monitor.getId(), new com.onboardhub.asset.dto.UpdateStatusRequest(AssetStatus.IN_REPAIR));
        String token = bearer(Role.ADMIN);
        mvc.perform(get("/api/assets").param("type", "LAPTOP").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/assets").param("status", "IN_REPAIR").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(monitor.getId()));
        mvc.perform(get("/api/assets").param("search", "dell-100").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(laptop.getId()));
        mvc.perform(get("/api/assets").param("type", "LAPTOP").param("status", "AVAILABLE")
                        .param("search", "dell").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/assets").param("type", "INVALID").header("Authorization", token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void assignmentConflictReturnAndHistoryWork() throws Exception {
        Asset laptop = createDirect(AssetType.LAPTOP, "ASSIGN-1");
        String token = bearer(Role.IT);
        MvcResult assigned = mvc.perform(post("/api/assets/{id}/assign", laptop.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(assignJson("LAPTOP")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ASSIGNED")).andReturn();
        String assetTag = objectMapper.readTree(assigned.getResponse().getContentAsString()).get("assetTag").asText();
        mvc.perform(post("/api/assets/{id}/assign", laptop.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(assignJson("LAPTOP")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Asset " + assetTag + " is not available"));
        Asset monitor = createDirect(AssetType.MONITOR, "ASSIGN-2");
        mvc.perform(post("/api/assets/{id}/assign", monitor.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(assignJson("LAPTOP")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message")
                        .value("Asset type does not match: expected LAPTOP but the asset is MONITOR"));
        mvc.perform(post("/api/assets/{id}/return", laptop.getId()).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AVAILABLE"));
        mvc.perform(get("/api/assets/{id}/history", laptop.getId()).header("Authorization", bearer(Role.HR)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].returnedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].returnedByName").value("Test IT"));
    }

    @Test
    void patchStatusesAndAssignmentRestrictionsWork() throws Exception {
        Asset asset = createDirect(AssetType.LAPTOP, "STATUS-1");
        String token = bearer(Role.IT);
        patchStatus(asset.getId(), "IN_REPAIR", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REPAIR"));
        patchStatus(asset.getId(), "RETIRED", token).andExpect(status().isOk());
        patchStatus(asset.getId(), "AVAILABLE", token).andExpect(status().isOk());
        mvc.perform(post("/api/assets/{id}/assign", asset.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(assignJson("LAPTOP")))
                .andExpect(status().isOk());
        patchStatus(asset.getId(), "RETIRED", token).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Return the asset before changing its status"));
        patchStatus(asset.getId(), "ASSIGNED", token).andExpect(status().isBadRequest());
    }

    @Test
    void summaryRouteReturnsOverallAndAllSixTypes() throws Exception {
        createDirect(AssetType.LAPTOP, "SUM-1");
        mvc.perform(get("/api/assets/summary").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.available").value(1)).andExpect(jsonPath("$.byType.length()").value(6))
                .andExpect(jsonPath("$.byType[0].type").value("LAPTOP"));
    }

    private org.springframework.test.web.servlet.ResultActions createViaHttp(AssetType type, String serial, String role)
            throws Exception {
        return mvc.perform(post("/api/assets").header("Authorization", bearer(Role.valueOf(role)))
                .contentType(MediaType.APPLICATION_JSON).content(assetJson(serial, type.name())));
    }

    private Asset createDirect(AssetType type, String serial) {
        var response = assetService.createAsset(new CreateAssetRequest(type, "Dell", "Latitude", serial,
                LocalDate.of(2025, 1, 1)));
        return assets.findById(response.id()).orElseThrow();
    }

    private org.springframework.test.web.servlet.ResultActions patchStatus(Long id, String status, String token)
            throws Exception {
        return mvc.perform(patch("/api/assets/{id}/status", id).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private String bearer(Role role) {
        Instant now = Instant.now();
        return "Bearer " + Jwts.builder().subject("23").claim("email", "test@example.com")
                .claim("role", role.name()).claim("fullName", "Test " + role.name())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }

    private String assetJson(String serial, String type) {
        return "{\"type\":\"" + type + "\",\"make\":\"Dell\",\"model\":\"Latitude\","
                + "\"serialNumber\":\"" + serial + "\",\"purchaseDate\":\"2025-01-01\"}";
    }

    private String assignJson(String expectedType) {
        return "{\"employeeId\":19,\"employeeName\":\"Asha Rao\",\"expectedType\":\"" + expectedType + "\"}";
    }
}
