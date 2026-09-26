package ph.chargemap.branch;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.branch.MetricsService.BranchPriceTrend;
import ph.chargemap.branch.MetricsService.LikeTrendPoint;
import ph.chargemap.branch.MetricsService.StationUpdateFrequency;
import ph.chargemap.user.Role;
import ph.chargemap.user.User;
import ph.chargemap.user.UserService;

import java.util.List;

/**
 * Studies/metrics for admins + super admin (guarded by {@code /api/moderation/**}).
 * Reward-completion metrics live under {@code /api/moderation/activities/metrics}.
 */
@RestController
@RequestMapping("/api/moderation/metrics")
public class MetricsController {

    private final MetricsService metricsService;
    private final UserService userService;

    public MetricsController(MetricsService metricsService, UserService userService) {
        this.metricsService = metricsService;
        this.userService = userService;
    }

    /** Branch price-change trend. days=1 for daily, 7 for weekly, etc. */
    @GetMapping("/branch-price-trend")
    public List<BranchPriceTrend> branchPriceTrend(@RequestParam(defaultValue = "7") int days) {
        return metricsService.branchPriceTrend(days);
    }

    /** How often a specific station gets price/image updates. */
    @GetMapping("/station/{stationId}/update-frequency")
    public StationUpdateFrequency stationUpdateFrequency(@PathVariable String stationId,
                                                         @RequestParam(defaultValue = "30") int days) {
        return metricsService.stationUpdateFrequency(stationId, days);
    }

    /**
     * Like trend for the calling admin's assigned branch. A SUPER_ADMIN (or an admin with
     * no branch) sees all branches.
     */
    @GetMapping("/branch-like-trend")
    public List<LikeTrendPoint> branchLikeTrend(@RequestParam(defaultValue = "7") int days) {
        User me = userService.requireCurrentUser();
        var branchId = (me.getRole() == Role.SUPER_ADMIN) ? null : me.getBranchId();
        return metricsService.likeTrend(branchId, days);
    }
}
