package ph.chargemap.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.points.PointsLedgerDto;
import ph.chargemap.points.PointsService;
import ph.chargemap.user.dto.RegisterUserRequest;
import ph.chargemap.user.dto.UserProfileDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PointsService pointsService;

    public UserController(UserService userService, PointsService pointsService) {
        this.userService = userService;
        this.pointsService = pointsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileDto register(@Valid @RequestBody RegisterUserRequest request) {
        return userService.register(request);
    }

    @GetMapping("/me")
    public UserProfileDto me() {
        return userService.me();
    }

    /** Edit the current user's display name (used by the admin profile screen too). */
    @PutMapping("/me")
    public UserProfileDto updateName(@RequestBody Map<String, String> body) {
        return userService.updateName(body.get("name"));
    }

    /** Change the current user's password (requires the current password). */
    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(body.get("currentPassword"), body.get("newPassword"));
    }

    @GetMapping("/me/points")
    public List<PointsLedgerDto> myPoints() {
        var userId = userService.requireCurrentUser().getId();
        return pointsService.recentLedger(userId).stream().map(PointsLedgerDto::from).toList();
    }
}
