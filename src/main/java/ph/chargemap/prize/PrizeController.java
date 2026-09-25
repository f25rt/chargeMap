package ph.chargemap.prize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public prize catalog (Requirement 7.1). */
@RestController
@RequestMapping("/api/prizes")
public class PrizeController {

    private final PrizeService prizeService;

    public PrizeController(PrizeService prizeService) {
        this.prizeService = prizeService;
    }

    @GetMapping
    public List<PrizeDto> catalog() {
        return prizeService.activeCatalog();
    }
}
