package ph.chargemap.activity;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.activity.ActivityDtos.ActivityCompletionStat;
import ph.chargemap.activity.ActivityDtos.ActivityDto;
import ph.chargemap.activity.ActivityDtos.CreateActivityRequest;
import ph.chargemap.activity.ActivityDtos.UserActivityDto;

import java.util.List;

/**
 * Task activities. Management (create/list/metrics) is under {@code /api/moderation}
 * (ADMIN/OPERATOR), user-facing browse/choose under {@code /api/activities} (any signed-in
 * user).
 */
@RestController
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    // ----- Management (admin/operator) -----
    @PostMapping("/api/moderation/activities")
    public ActivityDto create(@RequestBody CreateActivityRequest req) {
        return activityService.create(req);
    }

    @GetMapping("/api/moderation/activities")
    public List<ActivityDto> listAll() {
        return activityService.listAll();
    }

    @GetMapping("/api/moderation/activities/metrics")
    public List<ActivityCompletionStat> metrics() {
        return activityService.completionStats();
    }

    // ----- User-facing -----
    @GetMapping("/api/activities")
    public List<UserActivityDto> mine() {
        return activityService.listForCurrentUser();
    }

    @PostMapping("/api/activities/{id}/choose")
    @ResponseStatus(HttpStatus.OK)
    public UserActivityDto choose(@PathVariable String id) {
        return activityService.choose(id);
    }
}
