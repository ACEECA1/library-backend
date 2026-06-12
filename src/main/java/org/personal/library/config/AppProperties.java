package org.personal.library.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Storage storage = new Storage();
    private VirusScan virusScan = new VirusScan();
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        private int capacity = 100;
        private int tokens = 100;
        private int minutes = 1;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
    }

    @Data
    public static class Jwt {
        private String secret = "defaultSecretKeyThatIsAtLeast32BytesLongForHS256Algorithm";
        private long accessTokenExpirationMs = 3600000; 
        private long refreshTokenExpirationMs = 86400000; 
    }

    @Data
    public static class Storage {
        private String books = "storage/books";
        private String thumbnails = "storage/thumbnails";
        private String indexes = "storage/indexes";
    }

    @Data
    public static class VirusScan {
        private boolean enabled = true;
        private String host = "localhost";
        private int port = 3310;
    }
}
