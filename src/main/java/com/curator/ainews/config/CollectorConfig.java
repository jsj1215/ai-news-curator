package com.curator.ainews.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "collector.tech-blogs")
@Getter
@Setter
public class CollectorConfig {

    private int maxAgeHours = 168;
    private List<BlogSource> sources = new ArrayList<>();

    @Getter
    @Setter
    public static class BlogSource {
        private String name;
        private String feedUrl;
    }
}
