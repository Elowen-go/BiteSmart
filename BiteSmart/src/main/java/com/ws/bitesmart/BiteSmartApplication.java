package com.ws.bitesmart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BiteSmartApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiteSmartApplication.class, args);
    }

}
