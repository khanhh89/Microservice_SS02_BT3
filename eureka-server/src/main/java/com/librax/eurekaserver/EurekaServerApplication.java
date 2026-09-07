package com.librax.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * LibraX - Eureka Discovery Server
 * Cung cấp service registry cho toàn bộ microservice của hệ thống LibraX.
 * Các service (book-service, borrowing-service, notification-service, ...)
 * đều đăng ký tên logic tại đây, thay thế địa chỉ IP cố định của mô hình SOA.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
