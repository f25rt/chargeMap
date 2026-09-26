package ph.chargemap.branch;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BranchRepository extends MongoRepository<Branch, ObjectId> {
    List<Branch> findAllByOrderByNameAsc();
}
