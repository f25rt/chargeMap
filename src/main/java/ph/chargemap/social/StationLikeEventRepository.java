package ph.chargemap.social;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StationLikeEventRepository extends MongoRepository<StationLikeEvent, ObjectId> {
}
