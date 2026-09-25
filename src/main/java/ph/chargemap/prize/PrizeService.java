package ph.chargemap.prize;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import ph.chargemap.common.error.BadRequestException;
import ph.chargemap.common.error.NotFoundException;

import java.time.Instant;
import java.util.List;

/** Prize catalog + management (Requirement 7). Redemption is out of scope for now. */
@Service
public class PrizeService {

    private final PrizeRepository repository;

    public PrizeService(PrizeRepository repository) {
        this.repository = repository;
    }

    /** Active catalog for users. */
    public List<PrizeDto> activeCatalog() {
        return repository.findByActiveTrueOrderByPointCostAsc().stream().map(PrizeDto::from).toList();
    }

    /** All prizes (incl. inactive) for management. */
    public List<PrizeDto> all() {
        return repository.findAllByOrderByPointCostAsc().stream().map(PrizeDto::from).toList();
    }

    public PrizeDto create(PrizeRequest req) {
        Prize p = new Prize();
        apply(p, req);
        Instant now = Instant.now();
        p.setActive(true);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return PrizeDto.from(repository.save(p));
    }

    public PrizeDto update(String id, PrizeRequest req) {
        Prize p = load(id);
        apply(p, req);
        p.setUpdatedAt(Instant.now());
        return PrizeDto.from(repository.save(p));
    }

    public PrizeDto setActive(String id, boolean active) {
        Prize p = load(id);
        p.setActive(active);
        p.setUpdatedAt(Instant.now());
        return PrizeDto.from(repository.save(p));
    }

    private void apply(Prize p, PrizeRequest req) {
        p.setName(req.name());
        p.setDescription(req.description());
        p.setPointCost(req.pointCost());
        p.setImageId(req.imageId() != null && ObjectId.isValid(req.imageId())
                ? new ObjectId(req.imageId()) : null);
    }

    private Prize load(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid prize id: " + id);
        }
        return repository.findById(new ObjectId(id))
                .orElseThrow(() -> new NotFoundException("Prize not found: " + id));
    }
}
