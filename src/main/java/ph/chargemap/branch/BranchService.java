package ph.chargemap.branch;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;
import ph.chargemap.image.ImageService;
import ph.chargemap.station.Station;
import ph.chargemap.station.StationRepository;
import ph.chargemap.user.Role;
import ph.chargemap.user.User;
import ph.chargemap.user.UserRepository;

import java.time.Instant;
import java.util.List;

/** Branch CRUD, image upload, and admin/station assignment (SUPER_ADMIN scope). */
@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final ImageService imageService;

    public BranchService(BranchRepository branchRepository, UserRepository userRepository,
                         StationRepository stationRepository, ImageService imageService) {
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.imageService = imageService;
    }

    public List<BranchDto> list() {
        return branchRepository.findAllByOrderByNameAsc().stream().map(BranchDto::from).toList();
    }

    public BranchDto get(String id) {
        return BranchDto.from(load(id));
    }

    public BranchDto create(BranchRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("Branch name is required");
        }
        Instant now = Instant.now();
        Branch b = new Branch();
        b.setName(req.name().trim());
        b.setDescription(blank(req.description()));
        b.setArea(blank(req.area()));
        b.setCreatedAt(now);
        b.setUpdatedAt(now);
        return BranchDto.from(branchRepository.save(b));
    }

    public BranchDto update(String id, BranchRequest req) {
        Branch b = load(id);
        if (req.name() != null && !req.name().isBlank()) {
            b.setName(req.name().trim());
        }
        if (req.description() != null) {
            b.setDescription(blank(req.description()));
        }
        if (req.area() != null) {
            b.setArea(blank(req.area()));
        }
        b.setUpdatedAt(Instant.now());
        return BranchDto.from(branchRepository.save(b));
    }

    /** Uploads and sets the branch profile ("profile") or banner ("banner") image. */
    public BranchDto setImage(String id, String kind, MultipartFile file) {
        Branch b = load(id);
        ObjectId imageId = imageService.store(file);
        if ("banner".equalsIgnoreCase(kind)) {
            b.setBannerImageId(imageId);
        } else if ("profile".equalsIgnoreCase(kind)) {
            b.setProfileImageId(imageId);
        } else {
            throw new BadRequestException("Image kind must be 'profile' or 'banner'");
        }
        b.setUpdatedAt(Instant.now());
        return BranchDto.from(branchRepository.save(b));
    }

    /** Assigns an admin user to this branch. */
    public void assignAdmin(String branchId, String userId) {
        Branch b = load(branchId);
        User u = loadUser(userId);
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.SUPER_ADMIN) {
            throw new BadRequestException("Only admins can be assigned to a branch");
        }
        u.setBranchId(b.getId());
        u.setUpdatedAt(Instant.now());
        userRepository.save(u);
    }

    /** Assigns a station to this branch. */
    public void assignStation(String branchId, String stationId) {
        Branch b = load(branchId);
        if (stationId == null || !ObjectId.isValid(stationId)) {
            throw new BadRequestException("Invalid station id");
        }
        Station s = stationRepository.findById(new ObjectId(stationId))
                .orElseThrow(() -> new NotFoundException("Station not found: " + stationId));
        s.setBranchId(b.getId());
        s.setLastUpdated(Instant.now());
        stationRepository.save(s);
    }

    private Branch load(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid branch id: " + id);
        }
        return branchRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new NotFoundException("Branch not found: " + id));
    }

    private User loadUser(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid user id: " + id);
        }
        return userRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    public record BranchRequest(String name, String description, String area) {
    }
}
