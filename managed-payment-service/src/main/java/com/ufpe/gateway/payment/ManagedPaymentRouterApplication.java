package com.ufpe.gateway.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ManagedPaymentRouterApplication {

    static void main(String[] args) {
        SpringApplication.run(ManagedPaymentRouterApplication.class, args);
    }

}
