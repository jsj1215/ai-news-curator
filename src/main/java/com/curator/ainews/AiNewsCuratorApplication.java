package com.curator.ainews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiNewsCuratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiNewsCuratorApplication.class, args);
    }
}
