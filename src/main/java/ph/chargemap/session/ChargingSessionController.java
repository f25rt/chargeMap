package ph.chargemap.session;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.session.SessionDtos.LogSessionRequest;
import ph.chargemap.session.SessionDtos.SessionDto;
import ph.chargemap.session.SessionDtos.TelemetryRollupDto;

import java.util.List;

/**
 * Charging sessions + driver telemetry rollup. All endpoints require authentication and
 * act only on the calling user's own sessions.
 */
@RestController
@RequestMapping("/api/sessions")
public class ChargingSessionController {

    private final ChargingSessionService service;

    public ChargingSessionController(ChargingSessionService service) {
        this.service = service;
    }

    /** Log a completed charging session. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionDto log(@Valid @RequestBody LogSessionRequest req) {
        return service.log(req);
    }

    /** The caller's session history, newest first. */
    @GetMapping
    public List<SessionDto> list() {
        return service.listCurrentUser();
    }

    /** Aggregate telemetry rollup (energy, CO2, spend, trust score) for the profile. */
    @GetMapping("/telemetry")
    public TelemetryRollupDto telemetry() {
        return service.rollupForCurrentUser();
    }
}
