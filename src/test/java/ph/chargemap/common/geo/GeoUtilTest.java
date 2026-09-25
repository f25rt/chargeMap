package ph.chargemap.common.geo;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import ph.chargemap.common.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoUtilTest {

    @Test
    void toGeoJson_swapsToLngLatOrder() {
        GeoJsonPoint point = GeoUtil.toGeoJson(new GeoPoint(10.3116, 123.9165));
        // GeoJSON stores x = longitude, y = latitude.
        assertThat(point.getX()).isEqualTo(123.9165);
        assertThat(point.getY()).isEqualTo(10.3116);
    }

    @Test
    void toGeoPoint_swapsBackToLatLng() {
        GeoPoint point = GeoUtil.toGeoPoint(new GeoJsonPoint(123.9165, 10.3116));
        assertThat(point.lat()).isEqualTo(10.3116);
        assertThat(point.lng()).isEqualTo(123.9165);
    }

    @Test
    void kmMetersConversion_roundTrips() {
        assertThat(GeoUtil.kmToMeters(5)).isEqualTo(5000);
        assertThat(GeoUtil.metersToKm(5000)).isEqualTo(5);
    }

    @Test
    void validateLatLng_rejectsOutOfRange() {
        assertThatThrownBy(() -> GeoUtil.validateLatLng(91, 0))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> GeoUtil.validateLatLng(0, 181))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveRadiusKm_appliesDefaultAndBounds() {
        assertThat(GeoUtil.resolveRadiusKm(null, 5, 50)).isEqualTo(5);
        assertThat(GeoUtil.resolveRadiusKm(10.0, 5, 50)).isEqualTo(10);
        assertThatThrownBy(() -> GeoUtil.resolveRadiusKm(0.0, 5, 50))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> GeoUtil.resolveRadiusKm(51.0, 5, 50))
                .isInstanceOf(BadRequestException.class);
    }
}
