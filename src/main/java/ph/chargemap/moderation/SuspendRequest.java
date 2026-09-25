package ph.chargemap.moderation;

import jakarta.validation.constraints.NotBlank;

/** Reason for suspending a user (Requirement 5.1). */
public record SuspendRequest(@NotBlank String reason) {
}
