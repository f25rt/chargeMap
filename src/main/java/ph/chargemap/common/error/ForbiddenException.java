package ph.chargemap.common.error;

import org.springframework.http.HttpStatus;

/** Thrown when an authenticated user may not access a resource. Maps to HTTP 403. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
