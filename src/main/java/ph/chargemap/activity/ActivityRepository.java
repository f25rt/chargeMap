package ph.chargemap.activity;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityRepository extends MongoRepository<Activity, ObjectId> {
    List<Activity> findByActiveTrueOrderByCreatedAtDesc();
}
