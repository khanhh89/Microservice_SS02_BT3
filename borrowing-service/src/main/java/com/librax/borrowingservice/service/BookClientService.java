package com.librax.borrowingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/*
 * Vấn đề của code cũ:
 *   private RestTemplate restTemplate = new RestTemplate(); --> không có load balancing
 *   String url = "http://192.168.1.15:8082/api/books/" + bookId; --> gọi thẳng IP cứng
 *
 * Khi book-service scale lên nhiều instance thì IP sẽ thay đổi liên tục,
 * code cũ chỉ gọi vào 1 instance cố định nên sẽ lỗi ngay khi instance đó chết
 * hoặc IP bị đổi (rất hay xảy ra trong môi trường container / k8s).
 *
 * Cách sửa:
 *   1. Không new RestTemplate() trực tiếp, inject bean đã được đánh @LoadBalanced
 *   2. Thay IP bằng tên service "book-service" (tên đăng ký trên Eureka)
 *      --> Spring Cloud LoadBalancer sẽ tự tra Eureka lấy danh sách instance rồi
 *          chọn instance theo Round Robin
 *   3. Bọc try-catch để borrowing-service không bị crash khi book-service lỗi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookClientService {

    // bean này đã được khai báo @LoadBalanced trong RestTemplateConfig
    private final RestTemplate restTemplate;

    // tên này phải trùng với spring.application.name của book-service
    private static final String BOOK_SERVICE = "book-service";

    public String getBookTitle(Long bookId) {
        // dùng tên logic thay vì IP cứng
        String url = "http://" + BOOK_SERVICE + "/api/books/" + bookId + "/title";
        try {
            log.info("Gọi book-service lấy tên sách, bookId={}", bookId);
            return restTemplate.getForObject(url, String.class);

        } catch (ResourceAccessException e) {
            // book-service không phản hồi (timeout, tất cả instance down)
            log.error("book-service không phản hồi: {}", e.getMessage());
            return "Không thể lấy tên sách (book-service đang không khả dụng)";

        } catch (HttpClientErrorException e) {
            // trả lỗi 4xx, ví dụ 404 không tìm thấy sách
            log.warn("book-service trả lỗi {}: {}", e.getStatusCode(), e.getMessage());
            return "Sách id=" + bookId + " không tồn tại";

        } catch (HttpServerErrorException e) {
            // lỗi nội bộ phía book-service (5xx)
            log.error("book-service lỗi 5xx {}: {}", e.getStatusCode(), e.getMessage());
            return "book-service đang có sự cố, vui lòng thử lại sau";
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getBookDetails(Long bookId) {
        String url = "http://" + BOOK_SERVICE + "/api/books/" + bookId;
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("Lỗi khi gọi book-service: {}", e.getMessage());
            return null;
        }
    }

    public boolean isBookAvailable(Long bookId) {
        Map<String, Object> book = getBookDetails(bookId);
        if (book == null) return false;
        return Boolean.TRUE.equals(book.get("available"));
    }
}
