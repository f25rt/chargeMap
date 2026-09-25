package ph.chargemap.moderation;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.common.geo.GeoUtil;
import ph.chargemap.image.ImageService;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationDetailDto;
import ph.chargemap.station.StationMapper;
import ph.chargemap.station.StationRepository;

import java.time.Instant;

/**
 * Admin/operator station edits: location and image (Requirement 3). Pricing edits reuse
 * the existing operator pricing endpoint.
 */
@Service
public class StationEditService {

    private final StationRepository stationRepository;
    private final ImageService imageService;
    private final StationMapper mapper;

    public StationEditService(StationRepository stationRepository, ImageService imageService,
                              StationMapper mapper) {
        this.stationRepository = stationRepository;
        this.imageService = imageService;
        this.mapper = mapper;
    }

    public StationDetailDto updateLocation(String stationId, double lat, double lng) {
        GeoUtil.validateLatLng(lat, lng);
        Station station = load(stationId);
        station.setLocation(new GeoJsonPoint(lng, lat));
        station.setLastUpdated(Instant.now());
        return mapper.toDetail(stationRepository.save(station));
    }

    /** Admin/operator direct edit of a station's descriptive details (applied immediately). */
    public StationDetailDto updateDetails(String stationId, String name, String operator,
                                          String address, String area) {
        Station station = load(stationId);
        if (name != null && !name.isBlank()) {
            station.setName(name.trim());
        }
        if (operator != null) {
            station.setOperator(operator.isBlank() ? null : operator.trim());
        }
        if (address != null) {
            station.setAddress(address.isBlank() ? null : address.trim());
        }
        if (area != null) {
            station.setArea(area.isBlank() ? null : area.trim());
        }
        station.setLastUpdated(Instant.now());
        return mapper.toDetail(stationRepository.save(station));
    }

    public StationDetailDto replaceImage(String stationId, MultipartFile image) {
        Station station = load(stationId);
        ObjectId imageId = imageService.store(image);
        station.setImageId(imageId);
        station.setLastUpdated(Instant.now());
        return mapper.toDetail(stationRepository.save(station));
    }

    private Station load(String stationId) {
        if (stationId == null || !ObjectId.isValid(stationId)) {
            throw new BadRequestException("Invalid station id: " + stationId);
        }
        return stationRepository.findById(new ObjectId(stationId))
                .orElseThrow(() -> NotFoundException.station(stationId));
    }
}
