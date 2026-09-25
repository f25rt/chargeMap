package ph.chargemap.station;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.common.error.GlobalExceptionHandler;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.common.geo.GeoPoint;
import ph.chargemap.common.web.PageRequests;
import ph.chargemap.config.ChargeMapProperties;
import ph.chargemap.security.JwtAuthenticationFilter;
import ph.chargemap.security.JwtService;
import ph.chargemap.security.SecurityConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        ph.chargemap.common.web.RateLimitFilter.class,
        GlobalExceptionHandler.class, PageRequests.class, ChargeMapProperties.class})
class StationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    StationService stationService;

    @MockBean
    ph.chargemap.report.ReportService reportService;

    @MockBean
    org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    private StationSummaryDto sampleSummary(String id) {
        return new StationSummaryDto(id, "SM City Cebu", "PowerUp PH", "North Reclamation Area",
                new GeoPoint(10.3116, 123.9180), null, new BigDecimal("15.00"),
                2, 4, AvailabilitySummary.AVAILABLE, Instant.now(),
                DataSource.OPERATOR, Confidence.HIGH, Instant.now());
    }

    @Test
    void list_returnsPagedSummaries() throws Exception {
        var page = new PageImpl<>(List.of(sampleSummary("6520a0000000000000000001")),
                PageRequest.of(0, 20), 1);
        when(stationService.list(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("SM City Cebu"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void detail_returns404ForUnknownId() throws Exception {
        when(stationService.detail(eq("missing"))).thenThrow(NotFoundException.station("missing"));

        mockMvc.perform(get("/api/stations/missing").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void list_returns400ForInvalidConnectorFilter() throws Exception {
        // StationFilter.parse runs in the controller before the service is called.
        mockMvc.perform(get("/api/stations").param("connector", "NOTREAL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void search_returns400ForShortQuery() throws Exception {
        when(stationService.search(eq("a")))
                .thenThrow(new ph.chargemap.common.error.BadRequestException(
                        "Search query must be at least 2 characters"));

        mockMvc.perform(get("/api/stations/search").param("q", "a")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
