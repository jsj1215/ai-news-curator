package com.curator.ainews.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "collector.reddit")
@Getter
@Setter
public class RedditConfig {
    private List<String> subreddits = new ArrayList<>();
    private int maxAgeHours = 4;
}
