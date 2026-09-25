package ph.chargemap.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base type for application exceptions that carry an HTTP status and a stable
 * error code. The {@link GlobalExceptionHandler} translates these into
 * {@link ErrorResponse} bodies.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
