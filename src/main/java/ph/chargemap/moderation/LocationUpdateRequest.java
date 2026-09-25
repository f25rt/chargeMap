package ph.chargemap.moderation;

import jakarta.validation.constraints.NotNull;

/** Body for editing a station's location (Requirement 3.1). */
public record LocationUpdateRequest(@NotNull Double lat, @NotNull Double lng) {
}
