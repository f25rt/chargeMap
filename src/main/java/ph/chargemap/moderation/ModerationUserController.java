package ph.chargemap.moderation;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User management for OPERATOR + ADMIN (Requirement 5). Guarded by
 * {@code /api/moderation/**} in SecurityConfig.
 */
@RestController
@RequestMapping("/api/moderation/users")
public class ModerationUserController {

    private final ModerationUserService service;

    public ModerationUserController(ModerationUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<ManagedUserDto> list() {
        return service.list();
    }

    @PostMapping("/{id}/suspend")
    public ManagedUserDto suspend(@PathVariable String id, @Valid @RequestBody SuspendRequest req) {
        return service.suspend(id, req.reason());
    }

    @PostMapping("/{id}/unsuspend")
    public ManagedUserDto unsuspend(@PathVariable String id) {
        return service.unsuspend(id);
    }
}
