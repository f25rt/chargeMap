package ph.chargemap.submission;

import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;
import ph.chargemap.common.geo.GeoPoint;
import ph.chargemap.common.geo.GeoUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** View of a station submission for user + moderation screens. */
public record SubmissionDto(
        String id,
        String submittedBy,
        SubmissionStatus status,
        SubmissionType type,
        String targetStationId,
        String name,
        String operator,
        String address,
        String area,
        GeoPoint location,
        String imageId,
        boolean ocrLooksLikeStation,
        BigDecimal proposedPricePerKwh,
        ConnectorType connectorType,
        ChargerType chargerType,
        Double powerKw,
        List<CommentDto> comments,
        String createdStationId,
        Instant createdAt,
        Instant reviewedAt
) {
    public record CommentDto(String authorName, String role, String text, Instant createdAt) {
    }

    public static SubmissionDto from(StationSubmission s) {
        return new SubmissionDto(
                s.getId().toHexString(),
                s.getSubmittedBy() == null ? null : s.getSubmittedBy().toHexString(),
                s.getStatus(),
                s.getType(),
                s.getTargetStationId() == null ? null : s.getTargetStationId().toHexString(),
                s.getName(),
                s.getOperator(),
                s.getAddress(),
                s.getArea(),
                s.getLocation() == null ? null : GeoUtil.toGeoPoint(s.getLocation()),
                s.getImageId() == null ? null : s.getImageId().toHexString(),
                s.isOcrLooksLikeStation(),
                s.getProposedPricePerKwh(),
                s.getConnectorType(),
                s.getChargerType(),
                s.getPowerKw(),
                s.getComments().stream()
                        .map(c -> new CommentDto(c.getAuthorName(), c.getRole(), c.getText(),
                                c.getCreatedAt()))
                        .toList(),
                s.getCreatedStationId() == null ? null : s.getCreatedStationId().toHexString(),
                s.getCreatedAt(),
                s.getReviewedAt()
        );
    }
}
