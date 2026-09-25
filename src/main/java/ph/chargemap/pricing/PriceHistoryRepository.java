package ph.chargemap.pricing;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Repository for superseded price records. */
public interface PriceHistoryRepository extends MongoRepository<PriceHistory, ObjectId> {

    List<PriceHistory> findByStationIdOrderByEffectiveFromDesc(ObjectId stationId);
}
