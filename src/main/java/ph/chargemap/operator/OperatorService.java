package ph.chargemap.operator;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.Charger;
import ph.chargemap.charger.ChargerStatus;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.pricing.CurrentPricing;
import ph.chargemap.pricing.PriceHistory;
import ph.chargemap.pricing.PriceHistoryRepository;
import ph.chargemap.pricing.PricingModel;
import ph.chargemap.station.DataSource;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationDetailDto;
import ph.chargemap.station.StationMapper;
import ph.chargemap.station.StationRepository;

import java.time.Instant;
import java.util.List;

/**
 * Operator management actions: update charger status and station pricing (Requirement 22,
 * product spec Operator Portal). Operator-authored changes are marked as
 * {@link DataSource#OPERATOR} with a fresh verification timestamp.
 *
 * <p>MVP scope note: station "ownership" (claiming) is a future phase, so any OPERATOR or
 * ADMIN may manage any station for now. When claiming lands, add an owner check here.
 */
@Service
public class OperatorService {

    private final StationRepository stationRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final StationMapper mapper;

    public OperatorService(StationRepository stationRepository,
                           PriceHistoryRepository priceHistoryRepository,
                           StationMapper mapper) {
        this.stationRepository = stationRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.mapper = mapper;
    }

    /** All stations, mapped to detail DTOs, for the operator console. */
    public List<StationDetailDto> listStations() {
        return stationRepository.findAll().stream().map(mapper::toDetail).toList();
    }

    public StationDetailDto get(String stationId) {
        return mapper.toDetail(load(stationId));
    }

    /** Set a charger's status and recompute the station availability rollup. */
    public StationDetailDto updateChargerStatus(String stationId, String chargerId,
                                                ChargerStatus status) {
        Station station = load(stationId);
        ObjectId cid = parseId(chargerId, "charger");
        Charger target = station.getChargers().stream()
                .filter(c -> cid.equals(c.getChargerId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Charger not found: " + chargerId));

        Instant now = Instant.now();
        target.setStatus(status);
        target.setStatusUpdatedAt(now);
        recomputeAvailability(station, now);
        station.setDataSource(DataSource.OPERATOR);
        stationRepository.save(station);
        return mapper.toDetail(station);
    }

    /** Update the station's current price, archiving the previous price to history. */
    public StationDetailDto updatePricing(String stationId, PricingUpdateRequest request) {
        Station station = load(stationId);
        Instant now = Instant.now();
        PricingModel model = request.pricingModel() != null ? request.pricingModel()
                : PricingModel.PER_KWH;

        CurrentPricing prev = station.getCurrentPricing();
        if (prev != null && prev.getPricePerKwh() != null
                && prev.getPricePerKwh().compareTo(request.pricePerKwh()) != 0) {
            // Archive the superseded price.
            priceHistoryRepository.save(new PriceHistory(
                    station.getId(),
                    prev.getPricePerKwh(),
                    prev.getPricingModel(),
                    prev.getEffectiveFrom(),
                    now,
                    now));
        }

        CurrentPricing pricing = new CurrentPricing(request.pricePerKwh(), model, now, null);
        // Optional dynamic tariff (off-peak rate + peak window). Only set when all present
        // and the window is valid; otherwise the station stays on a flat rate.
        Integer ps = request.peakStartHour();
        Integer pe = request.peakEndHour();
        if (request.offPeakPricePerKwh() != null && ps != null && pe != null
                && ps >= 0 && ps <= 23 && pe >= 0 && pe <= 23 && !ps.equals(pe)) {
            pricing.setOffPeakPricePerKwh(request.offPeakPricePerKwh());
            pricing.setPeakStartHour(ps);
            pricing.setPeakEndHour(pe);
        }
        station.setCurrentPricing(pricing);
        station.setDataSource(DataSource.OPERATOR);
        station.setLastUpdated(now);
        station.setLastVerified(now);
        stationRepository.save(station);
        return mapper.toDetail(station);
    }

    private void recomputeAvailability(Station station, Instant now) {
        int available = (int) station.getChargers().stream()
                .filter(c -> c.getStatus() == ChargerStatus.AVAILABLE)
                .count();
        station.setAvailableCount(available);
        station.setTotalChargers(station.getChargers().size());
        station.setAvailabilitySummary(
                available > 0 ? AvailabilitySummary.AVAILABLE : AvailabilitySummary.OCCUPIED);
        station.setAvailabilityUpdatedAt(now);
        station.setLastUpdated(now);
        station.setLastVerified(now);
    }

    private Station load(String stationId) {
        return stationRepository.findById(parseId(stationId, "station"))
                .orElseThrow(() -> NotFoundException.station(stationId));
    }

    private ObjectId parseId(String id, String kind) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid " + kind + " id: " + id);
        }
        return new ObjectId(id);
    }
}
