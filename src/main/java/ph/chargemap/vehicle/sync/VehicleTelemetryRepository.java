package ph.chargemap.vehicle.sync;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VehicleTelemetryRepository extends MongoRepository<VehicleTelemetry, ObjectId> {

    Optional<VehicleTelemetry> findByVehicleId(ObjectId vehicleId);

    void deleteByVehicleId(ObjectId vehicleId);
}
