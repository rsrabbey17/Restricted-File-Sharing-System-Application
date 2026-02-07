package com.restrictedfileshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RestrictedFileSharingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestrictedFileSharingSystemApplication.class, args);
    }
}