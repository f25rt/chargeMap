package ph.chargemap.submission;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ph.chargemap.charger.ChargerType;
import ph.chargemap.charger.ConnectorType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A user-submitted charging station awaiting moderation (Requirement 1). On approval a
 * live {@code Station} is created from these fields.
 */
@Document(collection = "station_submissions")
public class StationSubmission {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId submittedBy;

    @Indexed
    private SubmissionStatus status = SubmissionStatus.PENDING;

    /** NEW = create a station on approval; EDIT = apply changes to {@code targetStationId}. */
    private SubmissionType type = SubmissionType.NEW;

    /** For EDIT submissions: the existing station the proposed changes apply to. */
    private ObjectId targetStationId;

    private String name;
    private String operator;
    private String address;
    private String area;
    private GeoJsonPoint location;
    private ObjectId imageId;

    // OCR audit + proposed data
    private String ocrText;
    private boolean ocrLooksLikeStation;
    private BigDecimal proposedPricePerKwh;
    private ConnectorType connectorType;
    private ChargerType chargerType;
    private Double powerKw;

    private List<SubmissionComment> comments = new ArrayList<>();

    private ObjectId reviewedBy;
    private Instant reviewedAt;
    private ObjectId createdStationId;

    private Instant createdAt;
    private Instant updatedAt;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(ObjectId submittedBy) {
        this.submittedBy = submittedBy;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public SubmissionType getType() {
        return type;
    }

    public void setType(SubmissionType type) {
        this.type = type;
    }

    public ObjectId getTargetStationId() {
        return targetStationId;
    }

    public void setTargetStationId(ObjectId targetStationId) {
        this.targetStationId = targetStationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public GeoJsonPoint getLocation() {
        return location;
    }

    public void setLocation(GeoJsonPoint location) {
        this.location = location;
    }

    public ObjectId getImageId() {
        return imageId;
    }

    public void setImageId(ObjectId imageId) {
        this.imageId = imageId;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public boolean isOcrLooksLikeStation() {
        return ocrLooksLikeStation;
    }

    public void setOcrLooksLikeStation(boolean ocrLooksLikeStation) {
        this.ocrLooksLikeStation = ocrLooksLikeStation;
    }

    public BigDecimal getProposedPricePerKwh() {
        return proposedPricePerKwh;
    }

    public void setProposedPricePerKwh(BigDecimal proposedPricePerKwh) {
        this.proposedPricePerKwh = proposedPricePerKwh;
    }

    public ConnectorType getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(ConnectorType connectorType) {
        this.connectorType = connectorType;
    }

    public ChargerType getChargerType() {
        return chargerType;
    }

    public void setChargerType(ChargerType chargerType) {
        this.chargerType = chargerType;
    }

    public Double getPowerKw() {
        return powerKw;
    }

    public void setPowerKw(Double powerKw) {
        this.powerKw = powerKw;
    }

    public List<SubmissionComment> getComments() {
        return comments;
    }

    public void setComments(List<SubmissionComment> comments) {
        this.comments = comments;
    }

    public ObjectId getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(ObjectId reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public ObjectId getCreatedStationId() {
        return createdStationId;
    }

    public void setCreatedStationId(ObjectId createdStationId) {
        this.createdStationId = createdStationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
