package ph.chargemap.security;

import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ph.chargemap.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Resolves the authenticated user's id from the security context. Used by controllers
 * for the {@code /users/me/**} endpoints.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static ObjectId requireId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof String id)
                || !ObjectId.isValid(id)) {
            throw new UnauthorizedException();
        }
        return new ObjectId(id);
    }

    /** 401 when no valid authenticated user is present. */
    public static class UnauthorizedException extends ApiException {
        public UnauthorizedException() {
            super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }
    }
}
