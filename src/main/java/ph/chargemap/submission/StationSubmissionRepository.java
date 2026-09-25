package ph.chargemap.submission;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StationSubmissionRepository extends MongoRepository<StationSubmission, ObjectId> {

    List<StationSubmission> findBySubmittedByOrderByCreatedAtDesc(ObjectId submittedBy);

    List<StationSubmission> findByStatusOrderByCreatedAtDesc(SubmissionStatus status);

    List<StationSubmission> findAllByOrderByCreatedAtDesc();
}
