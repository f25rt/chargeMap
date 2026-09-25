package ph.chargemap.moderation;

import jakarta.validation.constraints.NotBlank;

/** Manual points adjustment by admin/operator (Requirement 7.4). */
public record PointsAdjustRequest(long delta, @NotBlank String reason) {
}
