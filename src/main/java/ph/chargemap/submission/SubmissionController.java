package ph.chargemap.submission;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * User station submission endpoints (Requirement 1). Create is multipart (image + fields);
 * suspended users are blocked in the service (403).
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionDto create(
            @RequestParam("image") MultipartFile image,
            @RequestParam String name,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String area,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) String ocrText,
            @RequestParam(required = false, defaultValue = "false") boolean ocrLooksLikeStation,
            @RequestParam(required = false) BigDecimal pricePerKwh,
            @RequestParam(required = false) String connectorType,
            @RequestParam(required = false) String chargerType,
            @RequestParam(required = false) Double powerKw) {
        var form = new SubmissionService.SubmissionForm(
                name, operator, address, area, lat, lng, ocrText, ocrLooksLikeStation,
                pricePerKwh, connectorType, chargerType, powerKw);
        return submissionService.create(image, form);
    }

    /**
     * Propose edits to an existing station (Requirement 6 follow-up). Changes are queued
     * for admin/operator approval before they apply to the live station. JSON body; only
     * the fields to change need be present.
     */
    @PostMapping(value = "/stations/{stationId}/edits", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionDto proposeEdit(@PathVariable String stationId,
                                     @RequestBody SubmissionService.EditForm form) {
        return submissionService.createEdit(stationId, form);
    }

    @GetMapping("/me")
    public List<SubmissionDto> mine() {
        return submissionService.mySubmissions();
    }
}
