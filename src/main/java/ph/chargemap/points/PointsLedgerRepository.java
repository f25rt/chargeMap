package ph.chargemap.points;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface PointsLedgerRepository extends MongoRepository<PointsLedgerEntry, ObjectId> {

    List<PointsLedgerEntry> findTop50ByUserIdOrderByCreatedAtDesc(ObjectId userId);

    /** Sum of points for a user + task type since a cutoff — used for daily caps. */
    List<PointsLedgerEntry> findByUserIdAndTaskTypeAndCreatedAtGreaterThanEqual(
            ObjectId userId, PointsService.TaskType taskType, Instant since);
}
