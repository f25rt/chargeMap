package ph.chargemap.prize;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Body for creating/updating a prize. */
public record PrizeRequest(
        @NotBlank String name,
        String description,
        String imageId,
        @PositiveOrZero int pointCost
) {
}
