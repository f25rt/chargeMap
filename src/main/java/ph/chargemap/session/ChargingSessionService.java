package ph.chargemap.session;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.session.SessionDtos.LogSessionRequest;
import ph.chargemap.session.SessionDtos.SessionDto;
import ph.chargemap.session.SessionDtos.TelemetryRollupDto;
import ph.chargemap.user.User;
import ph.chargemap.user.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Records and summarizes a user's charging sessions. Session data is user-entered (real),
 * and drives the driver telemetry rollup (energy logged, CO2 avoided, spend, trust score).
 */
@Service
public class ChargingSessionService {

    /**
     * Estimated CO2 avoided per kWh charged versus an equivalent internal-combustion trip.
     * ~0.5 kg CO2/kWh is a widely-cited figure for EV vs. petrol on a typical grid; it's an
     * estimate, not a measured value, and is labeled as such in the UI.
     */
    private static final double CO2_KG_PER_KWH = 0.5;

    private final ChargingSessionRepository repository;
    private final UserService userService;

    public ChargingSessionService(ChargingSessionRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    public SessionDto log(LogSessionRequest req) {
        if (req == null || req.energyKwh() == null || req.energyKwh() <= 0) {
            throw new BadRequestException("Energy delivered (kWh) is required");
        }
        User user = userService.requireActiveUser();
        Instant now = Instant.now();

        ChargingSession s = new ChargingSession();
        s.setUserId(user.getId());
        if (req.stationId() != null && ObjectId.isValid(req.stationId())) {
            s.setStationId(new ObjectId(req.stationId()));
        }
        s.setStationName(req.stationName() != null && !req.stationName().isBlank()
                ? req.stationName().trim() : "Unlinked session");
        s.setEnergyKwh(round1(req.energyKwh()));
        s.setDurationMinutes(req.durationMinutes());
        s.setPeakKw(req.peakKw());
        if (req.pricePerKwh() != null) {
            s.setPricePerKwh(req.pricePerKwh());
            s.setCost(req.pricePerKwh()
                    .multiply(BigDecimal.valueOf(s.getEnergyKwh()))
                    .setScale(2, RoundingMode.HALF_UP));
        }
        s.setCo2SavedKg(round1(s.getEnergyKwh() * CO2_KG_PER_KWH));
        s.setStartedAt(req.startedAt() != null ? req.startedAt() : now);
        s.setCreatedAt(now);

        return SessionDto.from(repository.save(s));
    }

    public List<SessionDto> listCurrentUser() {
        ObjectId userId = userService.requireCurrentUser().getId();
        return repository.findByUserIdOrderByStartedAtDesc(userId).stream()
                .map(SessionDto::from)
                .toList();
    }

    /** Rollup for the profile hero card: energy, CO2, spend, and a derived trust score. */
    public TelemetryRollupDto rollupForCurrentUser() {
        User user = userService.requireCurrentUser();
        List<ChargingSession> sessions = repository.findByUserIdOrderByStartedAtDesc(user.getId());

        double energy = 0;
        double co2 = 0;
        BigDecimal spent = BigDecimal.ZERO;
        double pricedEnergy = 0;
        BigDecimal pricedCost = BigDecimal.ZERO;
        for (ChargingSession s : sessions) {
            energy += s.getEnergyKwh();
            co2 += s.getCo2SavedKg();
            if (s.getCost() != null) {
                spent = spent.add(s.getCost());
                pricedEnergy += s.getEnergyKwh();
                pricedCost = pricedCost.add(s.getCost());
            }
        }
        Double avgPrice = pricedEnergy > 0
                ? pricedCost.divide(BigDecimal.valueOf(pricedEnergy), 2, RoundingMode.HALF_UP).doubleValue()
                : null;

        int trust = trustScore(user, sessions.size());
        return new TelemetryRollupDto(
                round1(energy),
                round1(co2),
                sessions.size(),
                spent.setScale(2, RoundingMode.HALF_UP),
                avgPrice,
                trust,
                trustTier(trust));
    }

    /**
     * Derives a 0–100 trust score from the user's real contribution history: stations
     * added, updates/reports made, and logged sessions. Caps at 99 (no perfect scores);
     * new accounts start low and earn trust through verified contributions.
     */
    private int trustScore(User user, int sessionCount) {
        long contributions = user.getStationsAdded() * 5 + user.getUpdatesMade() * 2 + sessionCount;
        // Diminishing-returns curve: 50 base + up to ~49 from contributions.
        double score = 50 + 49 * (1 - Math.exp(-contributions / 20.0));
        return (int) Math.min(99, Math.round(score));
    }

    private String trustTier(int score) {
        if (score >= 90) {
            return "Top 5%";
        }
        if (score >= 75) {
            return "Trusted";
        }
        if (score >= 60) {
            return "Established";
        }
        return "New Pilot";
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
