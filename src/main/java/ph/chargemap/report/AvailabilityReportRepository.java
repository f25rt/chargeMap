package ph.chargemap.report;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AvailabilityReportRepository extends MongoRepository<AvailabilityReport, ObjectId> {

    List<AvailabilityReport> findByStationIdOrderByCreatedAtDesc(ObjectId stationId);
}
