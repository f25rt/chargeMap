package ph.chargemap.report;

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
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.Charger;
import ph.chargemap.charger.ChargerStatus;
import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;
import ph.chargemap.station.Station;
import ph.chargemap.support.MongoTestContainer;
import ph.chargemap.user.dto.LoginRequest;
import ph.chargemap.user.dto.RegisterUserRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end user reporting flow against a real MongoDB (Requirement 8).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportFlowIT extends MongoTestContainer {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MongoTemplate mongoTemplate;

    private ObjectId chargerId;
    private String stationId;

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();
        chargerId = new ObjectId();
        Station s = new Station();
        s.setName("Report Test Station");
        s.setLocation(new GeoJsonPoint(123.9, 10.31));
        s.setChargers(List.of(new Charger(chargerId, ConnectorType.CCS2, ChargerType.DC_FAST,
                60, ChargerStatus.AVAILABLE, Instant.now())));
        s.setAvailabilitySummary(AvailabilitySummary.AVAILABLE);
        s.setAvailableCount(1);
        s.setTotalChargers(1);
        s.setAvailabilityUpdatedAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        Station saved = mongoTemplate.save(s);
        stationId = saved.getId().toHexString();
    }

    private String token() throws Exception {
        var register = new RegisterUserRequest("reporter@example.com", "Reporter", "password123");
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));
        var login = new LoginRequest("reporter@example.com", "password123");
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String reportBody(String status, String chargerId) throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("status", status);
        if (chargerId != null) {
            node.put("chargerId", chargerId);
        }
        return objectMapper.writeValueAsString(node);
    }

    @Test
    void report_occupied_updatesStationSummary() throws Exception {
        String token = token();

        mockMvc.perform(post("/api/stations/" + stationId + "/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody("OCCUPIED", chargerId.toHexString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availabilitySummary").value("OCCUPIED"))
                .andExpect(jsonPath("$.availableCount").value(0));

        // The station's public availability should now reflect the report.
        mockMvc.perform(get("/api/stations/" + stationId + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCount").value(0));
    }

    @Test
    void report_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/stations/" + stationId + "/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody("OCCUPIED", chargerId.toHexString())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void report_invalidChargerId_returns400() throws Exception {
        String token = token();
        // Valid ObjectId format but not belonging to the station.
        String foreignCharger = new ObjectId().toHexString();

        mockMvc.perform(post("/api/stations/" + stationId + "/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody("OCCUPIED", foreignCharger)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void report_invalidStatus_returns400() throws Exception {
        String token = token();

        mockMvc.perform(post("/api/stations/" + stationId + "/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody("NONSENSE", chargerId.toHexString())))
                .andExpect(status().isBadRequest());
    }
}
