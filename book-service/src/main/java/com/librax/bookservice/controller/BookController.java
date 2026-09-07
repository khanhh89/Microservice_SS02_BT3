package com.librax.bookservice.controller;

import com.librax.bookservice.model.Book;
import com.librax.bookservice.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller cung cấp API cho book-service.
 *
 * Trong kiến trúc MSA, các service khác (borrowing-service, notification-service, ...)
 * gọi vào đây qua HTTP/REST với tên logic "book-service" thay vì IP cứng.
 */
@Slf4j
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * GET /api/books/{id}
     * Trả về thông tin đầy đủ của một đầu sách theo ID.
     * Đây là endpoint mà borrowing-service gọi để lấy thông tin sách trước khi tạo phiếu mượn.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        log.info("[book-service] Nhận yêu cầu lấy thông tin sách id={}", id);
        return bookService.findById(id)
                .map(book -> {
                    log.info("[book-service] Trả về sách: {}", book.getTitle());
                    return ResponseEntity.ok(book);
                })
                .orElseGet(() -> {
                    log.warn("[book-service] Không tìm thấy sách id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * GET /api/books/{id}/title
     * Trả về chỉ tiêu đề sách — endpoint nhẹ dành cho các service chỉ cần tên sách.
     */
    @GetMapping("/{id}/title")
    public ResponseEntity<String> getBookTitle(@PathVariable Long id) {
        log.info("[book-service] Nhận yêu cầu lấy tên sách id={}", id);
        return bookService.findById(id)
                .map(book -> ResponseEntity.ok(book.getTitle()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * PATCH /api/books/{id}/availability
     * Cập nhật trạng thái khả dụng của sách (mượn / trả).
     * Body: { "available": true/false }
     */
    @PatchMapping("/{id}/availability")
    public ResponseEntity<Book> updateAvailability(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {

        boolean available = body.getOrDefault("available", true);
        log.info("[book-service] Cập nhật availability sách id={} -> {}", id, available);

        return bookService.updateAvailability(id, available)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
