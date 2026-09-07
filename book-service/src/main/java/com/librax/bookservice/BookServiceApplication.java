package com.librax.bookservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * LibraX - Book Service Application
 *
 * Service này đăng ký tên logic "book-service" lên Eureka Server.
 * Khi scale lên nhiều instance, mỗi instance tự động đăng ký với IP riêng.
 * borrowing-service chỉ cần gọi "http://book-service/..." mà không cần biết IP cụ thể.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class BookServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookServiceApplication.class, args);
    }
}
