package ph.chargemap.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.station.StationDetailDto;

import java.util.List;

/**
 * Admin oversight API (product spec section 38). Restricted to ADMIN via SecurityConfig
 * ({@code /api/admin/**}).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public AdminStatsDto stats() {
        return adminService.stats();
    }

    @GetMapping("/reports")
    public List<ReportFeedItem> reports(@RequestParam(defaultValue = "50") int limit) {
        return adminService.recentReports(limit);
    }

    @GetMapping("/pricing-trends")
    public List<PricingTrendItem> pricingTrends(@RequestParam(defaultValue = "90") int days) {
        return adminService.pricingTrends(days);
    }

    @GetMapping("/stations")
    public List<StationDetailDto> stations() {
        return adminService.allStations();
    }

    @PostMapping("/stations/{id}/disable")
    public StationDetailDto disable(@PathVariable String id) {
        return adminService.setDisabled(id, true);
    }

    @PostMapping("/stations/{id}/enable")
    public StationDetailDto enable(@PathVariable String id) {
        return adminService.setDisabled(id, false);
    }

    @PostMapping("/stations/{id}/verify")
    public StationDetailDto verify(@PathVariable String id) {
        return adminService.verify(id);
    }
}
