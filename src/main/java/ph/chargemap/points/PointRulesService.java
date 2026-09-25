package ph.chargemap.points;

import org.springframework.stereotype.Service;

/**
 * Loads and updates the singleton {@link PointRules} config, seeding defaults on first
 * access (Requirement 7.3).
 */
@Service
public class PointRulesService {

    private final PointRulesRepository repository;

    public PointRulesService(PointRulesRepository repository) {
        this.repository = repository;
    }

    public PointRules get() {
        return repository.findById(PointRules.SINGLETON_ID)
                .orElseGet(() -> repository.save(new PointRules()));
    }

    public PointRules update(PointRules incoming) {
        PointRules rules = get();
        rules.setPointsPerStationAdd(incoming.getPointsPerStationAdd());
        rules.setPointsPerStationUpdate(incoming.getPointsPerStationUpdate());
        rules.setPointsPerReport(incoming.getPointsPerReport());
        rules.setDailyCapStationAdd(incoming.getDailyCapStationAdd());
        rules.setDailyCapStationUpdate(incoming.getDailyCapStationUpdate());
        rules.setDailyCapReport(incoming.getDailyCapReport());
        return repository.save(rules);
    }
}
