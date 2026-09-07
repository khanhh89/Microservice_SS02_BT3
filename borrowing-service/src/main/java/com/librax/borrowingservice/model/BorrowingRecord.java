package com.librax.borrowingservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Đại diện cho một phiếu mượn sách trong hệ thống LibraX.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowingRecord {

    private Long id;
    private Long bookId;
    private String bookTitle;       // cache lại tên sách để tránh gọi book-service mỗi lần đọc
    private String borrowerEmail;
    private String borrowerName;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;   // null nếu chưa trả
    private BorrowingStatus status;

    public enum BorrowingStatus {
        ACTIVE,         // Đang mượn
        RETURNED,       // Đã trả đúng hạn
        OVERDUE,        // Quá hạn
        CANCELLED       // Huỷ
    }
}
