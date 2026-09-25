package ph.chargemap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Enables Spring-managed MongoDB multi-document transactions. Requires a replica set
 * (MongoDB Atlas clusters are replica sets; local dev uses the {@code mongo:7} container
 * which also runs as a single-node replica set for transactions). With this in place,
 * {@code @Transactional} service methods commit all their writes atomically or roll back
 * together — preventing partial state such as an approved submission with no points award.
 */
@Configuration
@EnableTransactionManagement
public class MongoConfig {

    @Bean
    MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
