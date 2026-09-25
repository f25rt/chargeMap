package ph.chargemap.vehicle.sync.connector;

import java.time.Instant;

/** OAuth tokens (plaintext) as returned by a connector; encrypted before storage. */
public record TokenSet(String accessToken, String refreshToken, Instant expiresAt) {
}
