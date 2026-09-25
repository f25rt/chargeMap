package ph.chargemap.vehicle.sync;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Encrypted OAuth tokens for a connected vehicle. Access/refresh tokens are AES-256-GCM
 * ciphertext and are never exposed by any API/DTO.
 */
@Document(collection = "oauth_tokens")
public class OAuthToken {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId userId;

    @Indexed(unique = true)
    private ObjectId vehicleId;

    private Manufacturer provider;
    private String accessTokenEnc;
    private String refreshTokenEnc;
    private Instant expiresAt;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public ObjectId getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(ObjectId vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Manufacturer getProvider() {
        return provider;
    }

    public void setProvider(Manufacturer provider) {
        this.provider = provider;
    }

    public String getAccessTokenEnc() {
        return accessTokenEnc;
    }

    public void setAccessTokenEnc(String accessTokenEnc) {
        this.accessTokenEnc = accessTokenEnc;
    }

    public String getRefreshTokenEnc() {
        return refreshTokenEnc;
    }

    public void setRefreshTokenEnc(String refreshTokenEnc) {
        this.refreshTokenEnc = refreshTokenEnc;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
