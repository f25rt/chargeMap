package ph.chargemap.user.dto;

/** Access token returned by login (Requirement 9.3). */
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {

    public static TokenResponse bearer(String token, long expiresInSeconds) {
        return new TokenResponse(token, "Bearer", expiresInSeconds);
    }
}
