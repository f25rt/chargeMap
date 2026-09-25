package ph.chargemap.station;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data repository for {@link Station}. Geo and filter queries that need the
 * aggregation framework live in a dedicated query service; simple CRUD is here.
 */
public interface StationRepository extends MongoRepository<Station, ObjectId> {
}
