package ph.chargemap.moderation;

import jakarta.validation.constraints.NotBlank;

/** Body for adding a comment or rejecting a submission (Requirement 2.3, 2.4). */
public record CommentRequest(@NotBlank String text) {
}
