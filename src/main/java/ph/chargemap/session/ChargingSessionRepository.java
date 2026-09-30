package ph.chargemap.session;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Spring Data repository for {@link ChargingSession}. */
public interface ChargingSessionRepository extends MongoRepository<ChargingSession, ObjectId> {

    List<ChargingSession> findByUserIdOrderByStartedAtDesc(ObjectId userId);

    long countByUserId(ObjectId userId);
}
