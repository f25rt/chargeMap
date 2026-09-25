package ph.chargemap.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ph.chargemap.station.StationSummaryDto;

import java.util.List;

/** Authenticated favorites management (Requirement 11). */
@RestController
@RequestMapping("/api/users/me/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<StationSummaryDto> list() {
        return favoriteService.list();
    }

    @PostMapping("/{stationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable String stationId) {
        favoriteService.add(stationId);
    }

    @DeleteMapping("/{stationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String stationId) {
        favoriteService.remove(stationId);
    }
}
