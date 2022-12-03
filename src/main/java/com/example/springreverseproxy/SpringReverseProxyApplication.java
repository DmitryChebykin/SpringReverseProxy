package com.example.springreverseproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class SpringReverseProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringReverseProxyApplication.class, args);
    }
}
