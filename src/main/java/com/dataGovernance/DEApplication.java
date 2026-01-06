package com.dataGovernance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DEApplication {

    public static void main(String[] args) {
        SpringApplication.run(DEApplication.class, args);
    }

}
