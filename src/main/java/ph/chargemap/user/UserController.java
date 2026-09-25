package ph.chargemap.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.points.PointsLedgerDto;
import ph.chargemap.points.PointsService;
import ph.chargemap.user.dto.RegisterUserRequest;
import ph.chargemap.user.dto.UserProfileDto;

import java.util.List;

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

    @GetMapping("/me/points")
    public List<PointsLedgerDto> myPoints() {
        var userId = userService.requireCurrentUser().getId();
        return pointsService.recentLedger(userId).stream().map(PointsLedgerDto::from).toList();
    }
}
