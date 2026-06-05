package org.personal.library.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Storage storage = new Storage();
    private VirusScan virusScan = new VirusScan();

    @Data
    public static class Storage {
        private String books = "storage/books";
        private String thumbnails = "storage/thumbnails";
        private String indexes = "storage/indexes";
    }

    @Data
    public static class VirusScan {
        private String host = "localhost";
        private int port = 3310;
    }
}
