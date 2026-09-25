package ph.chargemap.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ph.chargemap.station.Station;
import ph.chargemap.support.MongoTestContainer;
import ph.chargemap.user.dto.LoginRequest;
import ph.chargemap.user.dto.RegisterUserRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end favorites flow against a real MongoDB (Requirement 11). */
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteFlowIT extends MongoTestContainer {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MongoTemplate mongoTemplate;

    private String stationId;

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();
        Station s = new Station();
        s.setName("Favorite Station");
        s.setLocation(new GeoJsonPoint(123.9, 10.31));
        stationId = mongoTemplate.save(s).getId().toHexString();
    }

    private String token() throws Exception {
        var register = new RegisterUserRequest("fav@example.com", "Fan", "password123");
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));
        var login = new LoginRequest("fav@example.com", "password123");
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void add_list_remove() throws Exception {
        String token = token();

        mockMvc.perform(post("/api/users/me/favorites/" + stationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/favorites").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(stationId))
                .andExpect(jsonPath("$[0].name").value("Favorite Station"));

        mockMvc.perform(delete("/api/users/me/favorites/" + stationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/favorites").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void addUnknownStation_returns404() throws Exception {
        String token = token();
        String missing = new ObjectId().toHexString();

        mockMvc.perform(post("/api/users/me/favorites/" + missing)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/users/me/favorites"))
                .andExpect(status().isUnauthorized());
    }
}
