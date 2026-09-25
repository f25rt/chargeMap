package ph.chargemap.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent error body returned for all failed requests.
 *
 * @param code    stable machine-readable error code (e.g. {@code VALIDATION_ERROR})
 * @param message human-readable message
 * @param fields  optional per-field validation details
 * @param timestamp when the error was produced
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        Map<String, String> fields,
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, Instant.now());
    }

    public static ErrorResponse of(String code, String message, Map<String, String> fields) {
        return new ErrorResponse(code, message, fields, Instant.now());
    }
}
