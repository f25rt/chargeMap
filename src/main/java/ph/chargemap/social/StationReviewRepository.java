package ph.chargemap.social;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StationReviewRepository extends MongoRepository<StationReview, ObjectId> {
    List<StationReview> findByStationIdOrderByCreatedAtDesc(ObjectId stationId);
}
