package ph.chargemap.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ph.chargemap.vehicle.Vehicle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Application user. Vehicles are embedded (few per user, always accessed with the owner)
 * and favorites are stored as a list of station ids (Requirements 9, 10, 11).
 */
@Document(collection = "users")
public class User {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String email;

    private String name;

    @JsonIgnore
    private String passwordHash;

    private Role role = Role.USER;

    private List<Vehicle> vehicles = new ArrayList<>();
    private List<ObjectId> favoriteStationIds = new ArrayList<>();

    // Moderation (Requirement 5)
    private boolean suspended;
    private String suspendedReason;
    private Instant suspendedAt;
    private ObjectId suspendedBy;

    // Gamification (Requirement 6)
    private long pointsBalance;
    private long lifetimePoints;
    private long stationsAdded;
    private long updatesMade;

    private Instant createdAt;
    private Instant updatedAt;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<ObjectId> getFavoriteStationIds() {
        return favoriteStationIds;
    }

    public void setFavoriteStationIds(List<ObjectId> favoriteStationIds) {
        this.favoriteStationIds = favoriteStationIds;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public String getSuspendedReason() {
        return suspendedReason;
    }

    public void setSuspendedReason(String suspendedReason) {
        this.suspendedReason = suspendedReason;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }

    public void setSuspendedAt(Instant suspendedAt) {
        this.suspendedAt = suspendedAt;
    }

    public ObjectId getSuspendedBy() {
        return suspendedBy;
    }

    public void setSuspendedBy(ObjectId suspendedBy) {
        this.suspendedBy = suspendedBy;
    }

    public long getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(long pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public long getLifetimePoints() {
        return lifetimePoints;
    }

    public void setLifetimePoints(long lifetimePoints) {
        this.lifetimePoints = lifetimePoints;
    }

    public long getStationsAdded() {
        return stationsAdded;
    }

    public void setStationsAdded(long stationsAdded) {
        this.stationsAdded = stationsAdded;
    }

    public long getUpdatesMade() {
        return updatesMade;
    }

    public void setUpdatesMade(long updatesMade) {
        this.updatesMade = updatesMade;
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
