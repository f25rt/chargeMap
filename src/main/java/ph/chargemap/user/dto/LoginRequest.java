package ph.chargemap.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Body for {@code POST /api/auth/login} (Requirement 9.3). */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
