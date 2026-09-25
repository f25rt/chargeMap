package ph.chargemap.user;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationMapper;
import ph.chargemap.station.StationRepository;
import ph.chargemap.station.StationSummaryDto;

import java.time.Instant;
import java.util.List;

/**
 * Manages the authenticated user's favorite stations (Requirement 11). Favorites are a
 * list of station ids on the user; listing resolves them to station summaries.
 */
@Service
public class FavoriteService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final StationMapper stationMapper;

    public FavoriteService(UserService userService, UserRepository userRepository,
                           StationRepository stationRepository, StationMapper stationMapper) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.stationMapper = stationMapper;
    }

    public void add(String stationId) {
        ObjectId id = toObjectId(stationId);
        if (!stationRepository.existsById(id)) {
            throw NotFoundException.station(stationId);
        }
        User user = userService.requireCurrentUser();
        if (user.getFavoriteStationIds().stream().noneMatch(id::equals)) {
            user.getFavoriteStationIds().add(id);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
        }
    }

    public void remove(String stationId) {
        ObjectId id = toObjectId(stationId);
        User user = userService.requireCurrentUser();
        boolean removed = user.getFavoriteStationIds().removeIf(id::equals);
        if (removed) {
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
        }
    }

    public List<StationSummaryDto> list() {
        User user = userService.requireCurrentUser();
        List<ObjectId> ids = user.getFavoriteStationIds();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Station> stations = stationRepository.findAllById(ids);
        return stations.stream().map(s -> stationMapper.toSummary(s, null)).toList();
    }

    private ObjectId toObjectId(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid station id: " + id);
        }
        return new ObjectId(id);
    }
}
