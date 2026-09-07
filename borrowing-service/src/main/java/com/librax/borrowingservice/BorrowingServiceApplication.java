package com.librax.borrowingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * LibraX - Borrowing Service Application
 *
 * Service quản lý việc mượn/trả sách và thông báo quá hạn.
 * Giao tiếp với book-service và notification-service qua REST API
 * với client-side load balancing (thay cho ESB trong mô hình SOA).
 */
@SpringBootApplication
@EnableDiscoveryClient
public class BorrowingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BorrowingServiceApplication.class, args);
    }
}
