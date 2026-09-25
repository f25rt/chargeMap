package ph.chargemap.prize;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PrizeRepository extends MongoRepository<Prize, ObjectId> {

    List<Prize> findByActiveTrueOrderByPointCostAsc();

    List<Prize> findAllByOrderByPointCostAsc();
}
