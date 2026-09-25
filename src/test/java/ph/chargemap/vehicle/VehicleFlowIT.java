package ph.chargemap.vehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ph.chargemap.charger.ConnectorType;
import ph.chargemap.support.MongoTestContainer;
import ph.chargemap.user.dto.LoginRequest;
import ph.chargemap.user.dto.RegisterUserRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end vehicle profile flow incl. cross-user isolation (Requirement 10). */
@SpringBootTest
@AutoConfigureMockMvc
class VehicleFlowIT extends MongoTestContainer {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void clean() {
        mongoTemplate.getDb().drop();
    }

    private String tokenFor(String email) throws Exception {
        var register = new RegisterUserRequest(email, "Owner", "password123");
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));
        var login = new LoginRequest(email, "password123");
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String vehicleBody() throws Exception {
        return objectMapper.writeValueAsString(
                new VehicleRequest("BYD", "Atto 3", 60.5, ConnectorType.CCS2, 7.0, 80.0));
    }

    @Test
    void create_list_update_delete() throws Exception {
        String token = tokenFor("owner@example.com");

        String created = mockMvc.perform(post("/api/users/me/vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.make").value("BYD"))
                .andExpect(jsonPath("$.connectorType").value("CCS2"))
                .andReturn().getResponse().getContentAsString();
        String vehicleId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/users/me/vehicles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(vehicleId));

        String updateBody = objectMapper.writeValueAsString(
                new VehicleRequest("BYD", "Atto 3 Extended", 60.5, ConnectorType.CCS2, 7.0, 88.0));
        mockMvc.perform(put("/api/users/me/vehicles/" + vehicleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Atto 3 Extended"))
                .andExpect(jsonPath("$.maxDcKw").value(88.0));

        mockMvc.perform(delete("/api/users/me/vehicles/" + vehicleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/vehicles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void otherUsersVehicle_isNotFound() throws Exception {
        String ownerToken = tokenFor("owner@example.com");
        String created = mockMvc.perform(post("/api/users/me/vehicles")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleBody()))
                .andReturn().getResponse().getContentAsString();
        String vehicleId = objectMapper.readTree(created).get("id").asText();

        String otherToken = tokenFor("other@example.com");
        // Another user cannot see or delete the owner's vehicle.
        mockMvc.perform(delete("/api/users/me/vehicles/" + vehicleId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/users/me/vehicles"))
                .andExpect(status().isUnauthorized());
    }
}
