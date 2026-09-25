package ph.chargemap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the {@code chargemap.*} configuration in application.yml.
 */
@ConfigurationProperties(prefix = "chargemap")
public class ChargeMapProperties {

    private final Geo geo = new Geo();
    private final Availability availability = new Availability();
    private final Pagination pagination = new Pagination();
    private final Security security = new Security();

    public Geo getGeo() {
        return geo;
    }

    public Availability getAvailability() {
        return availability;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public Security getSecurity() {
        return security;
    }

    /** Geospatial query defaults (Requirements 2.4, 2.5). */
    public static class Geo {
        private double defaultRadiusKm = 5;
        private int maxResults = 50;

        public double getDefaultRadiusKm() {
            return defaultRadiusKm;
        }

        public void setDefaultRadiusKm(double defaultRadiusKm) {
            this.defaultRadiusKm = defaultRadiusKm;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }
    }

    /** Availability staleness threshold (Requirements 4.3, 6.2). */
    public static class Availability {
        private long stalenessMinutes = 30;

        public long getStalenessMinutes() {
            return stalenessMinutes;
        }

        public void setStalenessMinutes(long stalenessMinutes) {
            this.stalenessMinutes = stalenessMinutes;
        }
    }

    /** Pagination defaults and bounds (Requirement 13.2). */
    public static class Pagination {
        private int defaultSize = 20;
        private int maxSize = 100;

        public int getDefaultSize() {
            return defaultSize;
        }

        public void setDefaultSize(int defaultSize) {
            this.defaultSize = defaultSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }

    private final RateLimit rateLimit = new RateLimit();

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /** Redis-backed rate limiting (product spec §37 abuse protection). */
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerMinute = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }

    /** JWT security settings (Requirement 9). */
    public static class Security {
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }

        public static class Jwt {
            private String secret;
            private long expiryMinutes = 1440;

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public long getExpiryMinutes() {
                return expiryMinutes;
            }

            public void setExpiryMinutes(long expiryMinutes) {
                this.expiryMinutes = expiryMinutes;
            }
        }
    }
}
