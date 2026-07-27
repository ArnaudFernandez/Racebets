package com.pixsom.racebets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class RacebetsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RacebetsApplication.class, args);
    }

}
