package com.curator.ainews.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "filter")
@Getter
@Setter
public class FilterConfig {
    private Keywords keywords = new Keywords();
    private int maxAgeHours = 48;
    private int minTitleLength = 10;

    @Getter
    @Setter
    public static class Keywords {
        private List<String> ai = new ArrayList<>();
        private List<String> backend = new ArrayList<>();
        private List<String> exclude = new ArrayList<>();
    }
}
