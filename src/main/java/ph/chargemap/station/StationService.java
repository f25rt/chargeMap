package ph.chargemap.station;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ph.chargemap.availability.AvailabilityDto;
import ph.chargemap.availability.AvailabilityService;
import ph.chargemap.charger.ChargerDto;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.pricing.PriceHistoryDto;
import ph.chargemap.pricing.PriceHistoryRepository;
import ph.chargemap.pricing.PricingDto;
import ph.chargemap.pricing.PricingResponse;

import java.time.Instant;
import java.util.List;

/**
 * Read operations for stations: paginated list, detail, and the chargers/pricing/
 * availability sub-resources (Requirements 1, 7).
 */
@Service
public class StationService {

    private final StationRepository stationRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final StationMapper mapper;
    private final AvailabilityService availabilityService;
    private final StationGeoQueryService geoQueryService;

    public StationService(StationRepository stationRepository,
                          PriceHistoryRepository priceHistoryRepository,
                          StationMapper mapper,
                          AvailabilityService availabilityService,
                          StationGeoQueryService geoQueryService) {
        this.stationRepository = stationRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.mapper = mapper;
        this.availabilityService = availabilityService;
        this.geoQueryService = geoQueryService;
    }

    // ----- Geospatial queries (Requirements 2, 3, 4) with filters (Requirement 6) -----

    public List<StationSummaryDto> nearby(double lat, double lng, Double radiusKm, StationFilter filter) {
        GeoContext ctx = geoContext(lat, lng, radiusKm);
        var criteria = filterCriteria(filter);
        return toSummaries(geoQueryService.nearby(ctx.center(), ctx.radiusKm(), criteria, geoQueryService.maxResults()));
    }

    public List<StationSummaryDto> cheapest(double lat, double lng, Double radiusKm, StationFilter filter) {
        GeoContext ctx = geoContext(lat, lng, radiusKm);
        var criteria = filterCriteria(filter);
        return toSummaries(geoQueryService.cheapest(ctx.center(), ctx.radiusKm(), criteria, geoQueryService.maxResults()));
    }

    public List<StationSummaryDto> available(double lat, double lng, Double radiusKm, StationFilter filter) {
        GeoContext ctx = geoContext(lat, lng, radiusKm);
        var criteria = filterCriteria(filter);
        return toSummaries(geoQueryService.available(ctx.center(), ctx.radiusKm(), criteria, geoQueryService.maxResults()));
    }

    // ----- Search (Requirement 5) -----

    public List<StationSummaryDto> search(String query) {
        if (query == null || query.trim().length() < 2) {
            throw new BadRequestException("Search query must be at least 2 characters");
        }
        return geoQueryService.search(query.trim(), geoQueryService.maxResults())
                .stream()
                .map(s -> mapper.toSummary(s, null))
                .toList();
    }

    private org.springframework.data.mongodb.core.query.Criteria filterCriteria(StationFilter filter) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        return filter.toCriteria(geoQueryService.stalenessCutoff());
    }

    private List<StationSummaryDto> toSummaries(List<GeoStation> stations) {
        return stations.stream()
                .map(s -> mapper.toSummary(s.getStation(), s.getDistanceMeters()))
                .toList();
    }

    private GeoContext geoContext(double lat, double lng, Double radiusKm) {
        ph.chargemap.common.geo.GeoUtil.validateLatLng(lat, lng);
        double resolved = ph.chargemap.common.geo.GeoUtil.resolveRadiusKm(
                radiusKm, geoQueryService.defaultRadiusKm(), 50);
        return new GeoContext(new ph.chargemap.common.geo.GeoPoint(lat, lng), resolved);
    }

    private record GeoContext(ph.chargemap.common.geo.GeoPoint center, double radiusKm) {
    }

    public Page<StationSummaryDto> list(Pageable pageable, StationFilter filter) {
        var criteria = filterCriteria(filter);
        return geoQueryService.listFiltered(criteria, pageable).map(s -> mapper.toSummary(s, null));
    }

    public StationDetailDto detail(String id) {
        return mapper.toDetail(findOrThrow(id));
    }

    public List<ChargerDto> chargers(String id) {
        return mapper.chargerDtos(findOrThrow(id));
    }

    public PricingResponse pricing(String id) {
        Station station = findOrThrow(id);
        PricingDto current = PricingDto.from(station.getCurrentPricing());
        List<PriceHistoryDto> history = priceHistoryRepository
                .findByStationIdOrderByEffectiveFromDesc(station.getId())
                .stream()
                .map(PriceHistoryDto::from)
                .toList();
        return new PricingResponse(current, history);
    }

    public AvailabilityDto availability(String id) {
        Station station = findOrThrow(id);
        return new AvailabilityDto(
                mapper.effectiveSummary(station),
                station.getAvailableCount(),
                station.getTotalChargers(),
                station.getAvailabilityUpdatedAt(),
                availabilityService.isStale(station, Instant.now()),
                mapper.chargerDtos(station)
        );
    }

    Station findOrThrow(String id) {
        ObjectId objectId = toObjectId(id);
        return stationRepository.findById(objectId)
                .orElseThrow(() -> NotFoundException.station(id));
    }

    static ObjectId toObjectId(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid station id: " + id);
        }
        return new ObjectId(id);
    }
}
