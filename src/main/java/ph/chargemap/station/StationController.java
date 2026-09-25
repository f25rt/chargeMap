package ph.chargemap.station;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.availability.AvailabilityDto;
import ph.chargemap.charger.ChargerDto;
import ph.chargemap.common.web.PageRequests;
import ph.chargemap.common.web.PageResponse;
import ph.chargemap.pricing.PricingResponse;
import ph.chargemap.report.ReportRequest;
import ph.chargemap.report.ReportResponse;
import ph.chargemap.report.ReportService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public read API for stations (Requirements 1, 7). No authentication required
 * (Requirement 9.1).
 */
@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;
    private final PageRequests pageRequests;
    private final ReportService reportService;

    public StationController(StationService stationService, PageRequests pageRequests,
                            ReportService reportService) {
        this.stationService = stationService;
        this.pageRequests = pageRequests;
        this.reportService = reportService;
    }

    @GetMapping
    public PageResponse<StationSummaryDto> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String chargerType,
            @RequestParam(required = false) String connector,
            @RequestParam(required = false) Double minKw,
            @RequestParam(required = false) Double maxKw,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) Boolean availableOnly) {
        StationFilter filter = StationFilter.parse(chargerType, connector, minKw, maxKw, priceMax, availableOnly);
        Page<StationSummaryDto> result = stationService.list(pageRequests.of(page, size), filter);
        return PageResponse.from(result, dto -> dto);
    }

    @GetMapping("/nearby")
    public List<StationSummaryDto> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) String chargerType,
            @RequestParam(required = false) String connector,
            @RequestParam(required = false) Double minKw,
            @RequestParam(required = false) Double maxKw,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) Boolean availableOnly) {
        StationFilter filter = StationFilter.parse(chargerType, connector, minKw, maxKw, priceMax, availableOnly);
        return stationService.nearby(lat, lng, radius, filter);
    }

    @GetMapping("/cheapest")
    public List<StationSummaryDto> cheapest(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) String chargerType,
            @RequestParam(required = false) String connector,
            @RequestParam(required = false) Double minKw,
            @RequestParam(required = false) Double maxKw,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) Boolean availableOnly) {
        StationFilter filter = StationFilter.parse(chargerType, connector, minKw, maxKw, priceMax, availableOnly);
        return stationService.cheapest(lat, lng, radius, filter);
    }

    @GetMapping("/available")
    public List<StationSummaryDto> available(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) String chargerType,
            @RequestParam(required = false) String connector,
            @RequestParam(required = false) Double minKw,
            @RequestParam(required = false) Double maxKw,
            @RequestParam(required = false) BigDecimal priceMax) {
        // available implies availableOnly semantics; still allow other filters
        StationFilter filter = StationFilter.parse(chargerType, connector, minKw, maxKw, priceMax, false);
        return stationService.available(lat, lng, radius, filter);
    }

    @GetMapping("/search")
    public List<StationSummaryDto> search(@RequestParam String q) {
        return stationService.search(q);
    }

    @GetMapping("/{id}")
    public StationDetailDto detail(@PathVariable String id) {
        return stationService.detail(id);
    }

    @GetMapping("/{id}/chargers")
    public List<ChargerDto> chargers(@PathVariable String id) {
        return stationService.chargers(id);
    }

    @GetMapping("/{id}/pricing")
    public PricingResponse pricing(@PathVariable String id) {
        return stationService.pricing(id);
    }

    @GetMapping("/{id}/availability")
    public AvailabilityDto availability(@PathVariable String id) {
        return stationService.availability(id);
    }

    @PostMapping("/{id}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse report(@PathVariable String id, @Valid @RequestBody ReportRequest request) {
        return reportService.submit(id, request);
    }
}
