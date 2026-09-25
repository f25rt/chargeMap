package ph.chargemap.station;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ph.chargemap.availability.AvailabilitySummary;
import ph.chargemap.charger.Charger;
import ph.chargemap.pricing.CurrentPricing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A charging station document. Chargers and the active price are embedded because they
 * are always read together on the map and detail screens. Availability is denormalized
 * (available count + summary + timestamp) so geo/availability queries filter and sort
 * without post-processing the charger array.
 *
 * <p>Geographic queries use the {@code 2dsphere} index on {@link #location}, which is
 * GeoJSON with {@code [longitude, latitude]} coordinate order.
 */
@Document(collection = "stations")
public class Station {

    @Id
    private ObjectId id;

    @TextIndexed(weight = 3)
    private String name;

    @TextIndexed
    private String operator;

    @TextIndexed
    private String address;

    @TextIndexed
    private String area;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private String openingHours;
    private String phone;
    private List<String> amenities = new ArrayList<>();
    private Double rating;

    private List<Charger> chargers = new ArrayList<>();

    // Indexed on currentPricing.pricePerKwh via MongoIndexConfig (embedded field).
    private CurrentPricing currentPricing;

    // Denormalized availability rollup (recomputed on report/update).
    private int availableCount;
    private int totalChargers;
    @Indexed
    private AvailabilitySummary availabilitySummary = AvailabilitySummary.UNKNOWN;
    @Indexed
    private Instant availabilityUpdatedAt;

    // Data-quality metadata (Requirement 12).
    private DataSource dataSource;
    private Confidence confidence;
    private Instant lastVerified;

    // Admin moderation: disabled stations are hidden from consumer results (section 38).
    private boolean disabled;

    // Optional station photo stored in GridFS (community submissions / operator uploads).
    private ObjectId imageId;

    private Instant createdAt;
    private Instant lastUpdated;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
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

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public List<Charger> getChargers() {
        return chargers;
    }

    public void setChargers(List<Charger> chargers) {
        this.chargers = chargers;
    }

    public CurrentPricing getCurrentPricing() {
        return currentPricing;
    }

    public void setCurrentPricing(CurrentPricing currentPricing) {
        this.currentPricing = currentPricing;
    }

    public int getAvailableCount() {
        return availableCount;
    }

    public void setAvailableCount(int availableCount) {
        this.availableCount = availableCount;
    }

    public int getTotalChargers() {
        return totalChargers;
    }

    public void setTotalChargers(int totalChargers) {
        this.totalChargers = totalChargers;
    }

    public AvailabilitySummary getAvailabilitySummary() {
        return availabilitySummary;
    }

    public void setAvailabilitySummary(AvailabilitySummary availabilitySummary) {
        this.availabilitySummary = availabilitySummary;
    }

    public Instant getAvailabilityUpdatedAt() {
        return availabilityUpdatedAt;
    }

    public void setAvailabilityUpdatedAt(Instant availabilityUpdatedAt) {
        this.availabilityUpdatedAt = availabilityUpdatedAt;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public void setConfidence(Confidence confidence) {
        this.confidence = confidence;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public ObjectId getImageId() {
        return imageId;
    }

    public void setImageId(ObjectId imageId) {
        this.imageId = imageId;
    }

    public Instant getLastVerified() {
        return lastVerified;
    }

    public void setLastVerified(Instant lastVerified) {
        this.lastVerified = lastVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
