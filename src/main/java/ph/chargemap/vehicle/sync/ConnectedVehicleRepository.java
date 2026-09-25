package ph.chargemap.vehicle.sync;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectedVehicleRepository extends MongoRepository<ConnectedVehicle, ObjectId> {

    List<ConnectedVehicle> findByUserIdOrderByCreatedAtAsc(ObjectId userId);

    List<ConnectedVehicle> findByConnectedTrue();

    Optional<ConnectedVehicle> findByIdAndUserId(ObjectId id, ObjectId userId);
}
