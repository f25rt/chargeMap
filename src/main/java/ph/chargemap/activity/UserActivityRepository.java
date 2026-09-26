package ph.chargemap.activity;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserActivityRepository extends MongoRepository<UserActivity, ObjectId> {

    List<UserActivity> findByUserId(ObjectId userId);

    Optional<UserActivity> findByUserIdAndActivityId(ObjectId userId, ObjectId activityId);

    List<UserActivity> findByUserIdAndStatus(ObjectId userId, UserActivity.Status status);

    // For the "most-completed reward" metric.
    List<UserActivity> findByStatus(UserActivity.Status status);
}
