package ph.chargemap.station;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import ph.chargemap.config.MongoIndexConfig;
import ph.chargemap.support.MongoTestContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the required station indexes are created against a real MongoDB:
 * the 2dsphere geo index, the text index, and the embedded charger/price filter
 * indexes (Requirements 2.3, 5.4, 6.1).
 */
@DataMongoTest
class StationIndexIT extends MongoTestContainer {

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void ensureIndexes() {
        // @DataMongoTest triggers annotation-based index creation; also run the
        // programmatic filter indexes that live in MongoIndexConfig.
        new MongoIndexConfig(mongoTemplate).ensureIndexes();
    }

    @Test
    void createsGeoTextAndFilterIndexes() {
        List<IndexInfo> indexes = mongoTemplate.indexOps(Station.class).getIndexInfo();

        // 2dsphere index on location
        assertThat(indexes).anySatisfy(idx ->
                assertThat(idx.getIndexFields()).anySatisfy(f -> {
                    assertThat(f.getKey()).isEqualTo("location");
                    assertThat(f.isGeo()).isTrue();
                }));

        // text index over name/operator/address/area
        assertThat(indexes).anySatisfy(idx -> assertThat(idx.getIndexFields())
                .anySatisfy(f -> assertThat(f.getKey()).contains("_fts")));

        // programmatic filter indexes
        assertThat(indexes).anySatisfy(idx -> assertThat(idx.getName()).isEqualTo("charger_connectorType"));
        assertThat(indexes).anySatisfy(idx -> assertThat(idx.getName()).isEqualTo("charger_chargerType"));
        assertThat(indexes).anySatisfy(idx -> assertThat(idx.getName()).isEqualTo("charger_powerKw"));
        assertThat(indexes).anySatisfy(idx -> assertThat(idx.getName()).isEqualTo("currentPricing_price"));
    }
}
