package ph.chargemap.moderation;

import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.image.ImageService;
import ph.chargemap.points.PointRules;
import ph.chargemap.points.PointRulesService;
import ph.chargemap.points.PointsService;
import ph.chargemap.prize.PrizeDto;
import ph.chargemap.prize.PrizeRequest;
import ph.chargemap.prize.PrizeService;

import java.util.List;
import java.util.Map;

/**
 * Admin/operator management of the rewards system (Requirement 7): prizes, point rules,
 * and manual point adjustments. Guarded by {@code /api/moderation/**}.
 */
@RestController
@RequestMapping("/api/moderation")
public class RewardsManagementController {

    private final PrizeService prizeService;
    private final PointRulesService rulesService;
    private final PointsService pointsService;
    private final ImageService imageService;

    public RewardsManagementController(PrizeService prizeService, PointRulesService rulesService,
                                       PointsService pointsService, ImageService imageService) {
        this.prizeService = prizeService;
        this.rulesService = rulesService;
        this.pointsService = pointsService;
        this.imageService = imageService;
    }

    // ----- Image upload (prize artwork, etc.) -----
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadImage(@RequestParam("image") MultipartFile image) {
        return Map.of("imageId", imageService.store(image).toHexString());
    }

    // ----- Prizes -----
    @GetMapping("/prizes")
    public List<PrizeDto> allPrizes() {
        return prizeService.all();
    }

    @PostMapping("/prizes")
    public PrizeDto createPrize(@Valid @RequestBody PrizeRequest req) {
        return prizeService.create(req);
    }

    @PutMapping("/prizes/{id}")
    public PrizeDto updatePrize(@PathVariable String id, @Valid @RequestBody PrizeRequest req) {
        return prizeService.update(id, req);
    }

    @PostMapping("/prizes/{id}/deactivate")
    public PrizeDto deactivatePrize(@PathVariable String id) {
        return prizeService.setActive(id, false);
    }

    @PostMapping("/prizes/{id}/activate")
    public PrizeDto activatePrize(@PathVariable String id) {
        return prizeService.setActive(id, true);
    }

    // ----- Point rules -----
    @GetMapping("/point-rules")
    public PointRules pointRules() {
        return rulesService.get();
    }

    @PutMapping("/point-rules")
    public PointRules updatePointRules(@RequestBody PointRules rules) {
        return rulesService.update(rules);
    }

    // ----- Manual points adjustment -----
    @PostMapping("/users/{id}/points")
    public void adjustPoints(@PathVariable String id, @Valid @RequestBody PointsAdjustRequest req) {
        if (!ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid user id: " + id);
        }
        pointsService.adjust(new ObjectId(id), req.delta(), req.reason());
    }
}
