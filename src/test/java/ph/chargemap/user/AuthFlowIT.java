package ph.chargemap.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ph.chargemap.support.MongoTestContainer;
import ph.chargemap.user.dto.LoginRequest;
import ph.chargemap.user.dto.RegisterUserRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end authentication flow against a real MongoDB (Requirement 9): register,
 * duplicate rejection, login, authenticated /me, and unauthenticated 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIT extends MongoTestContainer {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @Test
    void fullAuthFlow() throws Exception {
        var register = new RegisterUserRequest("driver@example.com", "Juan", "password123");

        // Register -> 201
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(json(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("driver@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        // Duplicate email -> 409
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(json(register)))
                .andExpect(status().isConflict());

        // Login -> token
        var login = new LoginRequest("driver@example.com", "password123");
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(json(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("accessToken").asText();

        // /me with token -> 200
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("driver@example.com"));

        // /me without token -> 401
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        // wrong password -> 401
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("driver@example.com", "wrongpass"))))
                .andExpect(status().isUnauthorized());
    }
}
