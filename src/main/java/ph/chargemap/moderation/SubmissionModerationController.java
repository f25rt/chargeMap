package ph.chargemap.moderation;

import jakarta.validation.Valid;
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
import ph.chargemap.station.StationDetailDto;
import ph.chargemap.submission.SubmissionDto;
import ph.chargemap.submission.SubmissionStatus;

import java.util.List;

/**
 * Submission review + station editing for OPERATOR + ADMIN (Requirements 2, 3).
 * Guarded by {@code /api/moderation/**}.
 */
@RestController
@RequestMapping("/api/moderation")
public class SubmissionModerationController {

    private final SubmissionModerationService moderation;
    private final StationEditService stationEdit;

    public SubmissionModerationController(SubmissionModerationService moderation,
                                          StationEditService stationEdit) {
        this.moderation = moderation;
        this.stationEdit = stationEdit;
    }

    @GetMapping("/submissions")
    public List<SubmissionDto> submissions(@RequestParam(required = false) SubmissionStatus status) {
        return moderation.list(status);
    }

    @GetMapping("/submissions/{id}")
    public SubmissionDto submission(@PathVariable String id) {
        return moderation.get(id);
    }

    @PostMapping("/submissions/{id}/comment")
    public SubmissionDto comment(@PathVariable String id, @Valid @RequestBody CommentRequest req) {
        return moderation.comment(id, req.text());
    }

    @PostMapping("/submissions/{id}/approve")
    public SubmissionDto approve(@PathVariable String id) {
        return moderation.approve(id);
    }

    @PostMapping("/submissions/{id}/reject")
    public SubmissionDto reject(@PathVariable String id, @Valid @RequestBody CommentRequest req) {
        return moderation.reject(id, req.text());
    }

    @PutMapping("/stations/{id}/location")
    public StationDetailDto updateLocation(@PathVariable String id,
                                           @Valid @RequestBody LocationUpdateRequest req) {
        return stationEdit.updateLocation(id, req.lat(), req.lng());
    }

    @PutMapping("/stations/{id}/details")
    public StationDetailDto updateDetails(@PathVariable String id,
                                          @RequestBody StationDetailsUpdateRequest req) {
        return stationEdit.updateDetails(id, req.name(), req.operator(), req.address(), req.area());
    }

    @PutMapping(value = "/stations/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StationDetailDto replaceImage(@PathVariable String id,
                                         @RequestParam("image") MultipartFile image) {
        return stationEdit.replaceImage(id, image);
    }
}
