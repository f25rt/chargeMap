package ph.chargemap.station;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link Station}. Geo and filter queries that need the
 * aggregation framework live in a dedicated query service; simple CRUD is here.
 */
public interface StationRepository extends MongoRepository<Station, ObjectId> {

    /** Lookup by external source identifier (used by importers to dedup on re-run). */
    Optional<Station> findBySourceRef(String sourceRef);

    /** Batch lookup of already-imported stations for a set of source refs. */
    List<Station> findBySourceRefIn(Collection<String> sourceRefs);
}
