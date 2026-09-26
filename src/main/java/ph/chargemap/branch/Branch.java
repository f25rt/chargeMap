package ph.chargemap.branch;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * An organizational branch. Admins are assigned to a branch and stations can belong to a
 * branch. Profile/banner images are GridFS ids; the profile image doubles as the branch's
 * map marker icon when set.
 */
@Document(collection = "branches")
public class Branch {

    @Id
    private ObjectId id;

    private String name;
    private String description;
    private String area;

    private ObjectId profileImageId; // also used as the map marker icon
    private ObjectId bannerImageId;

    private Instant createdAt;
    private Instant updatedAt;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public ObjectId getProfileImageId() {
        return profileImageId;
    }

    public void setProfileImageId(ObjectId profileImageId) {
        this.profileImageId = profileImageId;
    }

    public ObjectId getBannerImageId() {
        return bannerImageId;
    }

    public void setBannerImageId(ObjectId bannerImageId) {
        this.bannerImageId = bannerImageId;
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
