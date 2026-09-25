package ph.chargemap.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class that starts a single shared MongoDB container for integration tests and
 * points Spring Data at it. Real geospatial ($geoNear), text search, and index behavior
 * cannot be faithfully mocked, so these tests exercise an actual MongoDB.
 */
@Testcontainers
public abstract class MongoTestContainer {

    static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7"));

    static {
        MONGO.start();
    }

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }
}
