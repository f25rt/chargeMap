package ph.chargemap.submission;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.geo.GeoUtil;
import ph.chargemap.image.ImageService;
import ph.chargemap.user.User;
import ph.chargemap.user.UserService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Creates and lists user station submissions (Requirement 1). Suspended users are
 * blocked. OCR is done client-side; the client passes the raw text, heuristic result,
 * and any detected price, which we store for audit.
 */
@Service
public class SubmissionService {

    private final StationSubmissionRepository repository;
    private final ImageService imageService;
    private final UserService userService;

    public SubmissionService(StationSubmissionRepository repository, ImageService imageService,
                             UserService userService) {
        this.repository = repository;
        this.imageService = imageService;
        this.userService = userService;
    }

    public SubmissionDto create(MultipartFile image, SubmissionForm form) {
        User user = userService.requireActiveUser(); // 403 if suspended
        if (form.name() == null || form.name().isBlank()) {
            throw new BadRequestException("Station name is required");
        }
        GeoUtil.validateLatLng(form.lat(), form.lng());

        ObjectId imageId = imageService.store(image);

        StationSubmission s = new StationSubmission();
        s.setSubmittedBy(user.getId());
        s.setStatus(SubmissionStatus.PENDING);
        s.setName(form.name().trim());
        s.setOperator(blankToNull(form.operator()));
        s.setAddress(blankToNull(form.address()));
        s.setArea(blankToNull(form.area()));
        s.setLocation(new GeoJsonPoint(form.lng(), form.lat()));
        s.setImageId(imageId);
        s.setOcrText(form.ocrText());
        s.setOcrLooksLikeStation(form.ocrLooksLikeStation());
        s.setProposedPricePerKwh(form.pricePerKwh());
        s.setConnectorType(parseEnum(ConnectorType.class, form.connectorType()));
        s.setChargerType(parseEnum(ChargerType.class, form.chargerType()));
        s.setPowerKw(form.powerKw());
        Instant now = Instant.now();
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        return SubmissionDto.from(repository.save(s));
    }

    /**
     * Creates an EDIT request against an existing station. Only the fields the user wants
     * to change need be non-null; on approval a moderator applies the non-null fields to the
     * live station. No image is required for an edit. Suspended users are blocked (403).
     */
    public SubmissionDto createEdit(String targetStationId, EditForm form) {
        User user = userService.requireActiveUser(); // 403 if suspended
        if (targetStationId == null || !ObjectId.isValid(targetStationId)) {
            throw new BadRequestException("Invalid station id: " + targetStationId);
        }
        boolean hasAnyChange = notBlank(form.name()) || notBlank(form.operator())
                || notBlank(form.address()) || notBlank(form.area())
                || form.pricePerKwh() != null || notBlank(form.connectorType())
                || notBlank(form.chargerType()) || form.powerKw() != null;
        if (!hasAnyChange) {
            throw new BadRequestException("Provide at least one field to change");
        }

        StationSubmission s = new StationSubmission();
        s.setSubmittedBy(user.getId());
        s.setStatus(SubmissionStatus.PENDING);
        s.setType(SubmissionType.EDIT);
        s.setTargetStationId(new ObjectId(targetStationId));
        s.setName(blankToNull(form.name()));
        s.setOperator(blankToNull(form.operator()));
        s.setAddress(blankToNull(form.address()));
        s.setArea(blankToNull(form.area()));
        s.setProposedPricePerKwh(form.pricePerKwh());
        s.setConnectorType(parseEnum(ConnectorType.class, form.connectorType()));
        s.setChargerType(parseEnum(ChargerType.class, form.chargerType()));
        s.setPowerKw(form.powerKw());
        Instant now = Instant.now();
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        return SubmissionDto.from(repository.save(s));
    }

    public List<SubmissionDto> mySubmissions() {
        ObjectId userId = userService.requireCurrentUser().getId();
        return repository.findBySubmittedByOrderByCreatedAtDesc(userId).stream()
                .map(SubmissionDto::from)
                .toList();
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid value for " + type.getSimpleName() + ": " + value);
        }
    }

    /** Bound from multipart form fields (all strings/numbers alongside the image part). */
    public record SubmissionForm(
            String name,
            String operator,
            String address,
            String area,
            double lat,
            double lng,
            String ocrText,
            boolean ocrLooksLikeStation,
            BigDecimal pricePerKwh,
            String connectorType,
            String chargerType,
            Double powerKw
    ) {
    }

    /** JSON body for an edit request; only the fields to change need be set. */
    public record EditForm(
            String name,
            String operator,
            String address,
            String area,
            BigDecimal pricePerKwh,
            String connectorType,
            String chargerType,
            Double powerKw
    ) {
    }
}
