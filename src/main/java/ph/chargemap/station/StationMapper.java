package ph.chargemap.station;

import org.springframework.stereotype.Component;
import ph.chargemap.availability.AvailabilityService;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.ChargerDto;
import ph.chargemap.common.geo.GeoPoint;
import ph.chargemap.common.geo.GeoUtil;
import ph.chargemap.pricing.CurrentPricing;
import ph.chargemap.pricing.PricingDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Maps {@link Station} documents to API DTOs. All availability values are passed through
 * {@link AvailabilityService#effectiveSummary} so stale data is downgraded to
 * {@code UNKNOWN} consistently (Requirements 1.4, 4.2, 4.3).
 */
@Component
public class StationMapper {

    private final AvailabilityService availabilityService;

    public StationMapper(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    public StationSummaryDto toSummary(Station s, Double distanceMeters) {
        Instant now = Instant.now();
        return new StationSummaryDto(
                s.getId() == null ? null : s.getId().toHexString(),
                s.getName(),
                s.getOperator(),
                s.getArea(),
                location(s),
                distanceMeters,
                price(s.getCurrentPricing()),
                s.getAvailableCount(),
                s.getTotalChargers(),
                availabilityService.effectiveSummary(s, now),
                s.getAvailabilityUpdatedAt(),
                s.getDataSource(),
                s.getConfidence(),
                s.getLastUpdated()
        );
    }

    public StationDetailDto toDetail(Station s) {
        Instant now = Instant.now();
        return new StationDetailDto(
                s.getId() == null ? null : s.getId().toHexString(),
                s.getName(),
                s.getOperator(),
                s.getAddress(),
                s.getArea(),
                location(s),
                s.getOpeningHours(),
                s.getPhone(),
                s.getAmenities(),
                s.getRating(),
                chargerDtos(s),
                PricingDto.from(s.getCurrentPricing()),
                availabilityService.effectiveSummary(s, now),
                s.getAvailableCount(),
                s.getTotalChargers(),
                s.getAvailabilityUpdatedAt(),
                s.getDataSource(),
                s.getConfidence(),
                s.getLastVerified(),
                s.getLastUpdated(),
                s.isDisabled(),
                s.getImageId() == null ? null : s.getImageId().toHexString(),
                s.getLikeCount()
        );
    }

    public AvailabilitySummary effectiveSummary(Station s) {
        return availabilityService.effectiveSummary(s, Instant.now());
    }

    public List<ChargerDto> chargerDtos(Station s) {
        return s.getChargers().stream().map(ChargerDto::from).toList();
    }

    private GeoPoint location(Station s) {
        return s.getLocation() == null ? null : GeoUtil.toGeoPoint(s.getLocation());
    }

    private BigDecimal price(CurrentPricing p) {
        return p == null ? null : p.getPricePerKwh();
    }
}
