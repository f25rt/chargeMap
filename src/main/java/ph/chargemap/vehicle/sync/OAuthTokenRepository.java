package ph.chargemap.vehicle.sync;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OAuthTokenRepository extends MongoRepository<OAuthToken, ObjectId> {

    Optional<OAuthToken> findByVehicleId(ObjectId vehicleId);

    void deleteByVehicleId(ObjectId vehicleId);
}
