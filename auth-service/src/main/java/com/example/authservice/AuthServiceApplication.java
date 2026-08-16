package com.example.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(AuthServiceApplication.class, args);
        System.out.println("GOOGLE_CLIENT_ID = " +
                System.getenv("GOOGLE_CLIENT_ID"));
    }

}
