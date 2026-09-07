package com.librax.bookservice.service;

import com.librax.bookservice.model.Book;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ liên quan đến đầu sách.
 * Dùng in-memory map để demo (thực tế thay bằng repository + DB).
 */
@Service
public class BookService {

    // Dữ liệu giả lập (thay thế bằng JPA Repository + DB thực tế)
    private final Map<Long, Book> bookStore = new HashMap<>();

    public BookService() {
        bookStore.put(1L, Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .category("Software Engineering")
                .available(true)
                .build());

        bookStore.put(2L, Book.builder()
                .id(2L)
                .title("Designing Data-Intensive Applications")
                .author("Martin Kleppmann")
                .isbn("9781491903100")
                .category("System Design")
                .available(false)
                .build());

        bookStore.put(3L, Book.builder()
                .id(3L)
                .title("Microservices Patterns")
                .author("Chris Richardson")
                .isbn("9781617294549")
                .category("Architecture")
                .available(true)
                .build());
    }

    /**
     * Tìm sách theo ID.
     * @param id ID của đầu sách
     * @return Optional<Book>
     */
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(bookStore.get(id));
    }

    /**
     * Cập nhật trạng thái khả dụng của sách sau khi mượn/trả.
     * @param id        ID sách
     * @param available true = trả sách, false = mượn sách
     * @return Optional<Book> sau khi cập nhật
     */
    public Optional<Book> updateAvailability(Long id, boolean available) {
        Book book = bookStore.get(id);
        if (book == null) return Optional.empty();
        book.setAvailable(available);
        return Optional.of(book);
    }
}
