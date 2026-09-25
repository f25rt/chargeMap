package ph.chargemap.operator;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.station.StationDetailDto;

import java.util.List;

/**
 * Operator management API (product spec section 22). Access is restricted to
 * OPERATOR and ADMIN via SecurityConfig ({@code /api/operator/**}).
 */
@RestController
@RequestMapping("/api/operator")
public class OperatorController {

    private final OperatorService operatorService;

    public OperatorController(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @GetMapping("/stations")
    public List<StationDetailDto> stations() {
        return operatorService.listStations();
    }

    @GetMapping("/stations/{id}")
    public StationDetailDto station(@PathVariable String id) {
        return operatorService.get(id);
    }

    @PutMapping("/stations/{id}/chargers/{chargerId}/status")
    public StationDetailDto updateChargerStatus(
            @PathVariable String id,
            @PathVariable String chargerId,
            @Valid @RequestBody ChargerStatusUpdateRequest request) {
        return operatorService.updateChargerStatus(id, chargerId, request.status());
    }

    @PutMapping("/stations/{id}/pricing")
    public StationDetailDto updatePricing(
            @PathVariable String id,
            @Valid @RequestBody PricingUpdateRequest request) {
        return operatorService.updatePricing(id, request);
    }
}
