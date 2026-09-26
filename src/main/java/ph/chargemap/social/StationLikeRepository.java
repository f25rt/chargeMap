package ph.chargemap.social;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StationLikeRepository extends MongoRepository<StationLike, ObjectId> {
    Optional<StationLike> findByStationIdAndUserId(ObjectId stationId, ObjectId userId);
    boolean existsByStationIdAndUserId(ObjectId stationId, ObjectId userId);
    long countByStationId(ObjectId stationId);
}
