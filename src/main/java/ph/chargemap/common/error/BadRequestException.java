package ph.chargemap.common.error;

import org.springframework.http.HttpStatus;

/** Thrown for invalid input that bean validation did not catch. Maps to HTTP 400. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
