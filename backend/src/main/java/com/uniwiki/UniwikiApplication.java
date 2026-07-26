package com.uniwiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UniwikiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniwikiApplication.class, args);
    }
}
