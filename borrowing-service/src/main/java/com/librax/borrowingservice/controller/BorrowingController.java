package com.librax.borrowingservice.controller;

import com.librax.borrowingservice.model.BorrowingRecord;
import com.librax.borrowingservice.service.BorrowingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/borrowings")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingService borrowingService;

    @GetMapping
    public ResponseEntity<List<BorrowingRecord>> getAll() {
        return ResponseEntity.ok(borrowingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BorrowingRecord> getById(@PathVariable Long id) {
        return borrowingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // tạo phiếu mượn mới, nội bộ sẽ gọi book-service để lấy thông tin sách
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long bookId = Long.valueOf(body.get("bookId").toString());
        String email = (String) body.get("borrowerEmail");
        String name  = (String) body.get("borrowerName");

        return borrowingService.createBorrowing(bookId, email, name)
                .map(r -> ResponseEntity.status(HttpStatus.CREATED).body((Object) r))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Sách không khả dụng hoặc book-service đang bị lỗi"));
    }

    @PatchMapping("/{id}/return")
    public ResponseEntity<BorrowingRecord> returnBook(@PathVariable Long id) {
        return borrowingService.returnBook(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<BorrowingRecord>> getOverdue() {
        return ResponseEntity.ok(borrowingService.getOverdueRecords());
    }

    /**
     * POST /api/borrowings/{id}/notify-overdue
     *
     * Đây là REST endpoint tương ứng với thao tác notifyOverdue ở Bài 2.
     * Bài 2 dùng ESB để định tuyến message XML sang notification-service.
     * Bài 3 gọi thẳng REST API của notification-service qua tên logic Eureka.
     */
    @PostMapping("/{id}/notify-overdue")
    public ResponseEntity<Map<String, Object>> notifyOverdue(@PathVariable Long id) {
        if (borrowingService.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Không tìm thấy phiếu mượn id=" + id));
        }

        boolean ok = borrowingService.notifyOverdueForRecord(id);
        if (ok) {
            return ResponseEntity.ok(Map.of(
                    "borrowingId", id,
                    "message", "Đã gửi thông báo quá hạn thành công"
            ));
        } else {
            // trả 502: borrowing-service OK nhưng downstream notification-service lỗi
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "borrowingId", id,
                            "error", "Không gửi được thông báo (notification-service không phản hồi hoặc phiếu chưa quá hạn)"
                    ));
        }
    }

    // gửi hàng loạt thông báo cho tất cả phiếu đang quá hạn (dùng cho scheduled job)
    @PostMapping("/notify-overdue/batch")
    public ResponseEntity<Map<String, Object>> notifyAllOverdue() {
        List<BorrowingRecord> overdueList = borrowingService.getOverdueRecords();
        int success = 0;
        for (BorrowingRecord r : overdueList) {
            if (borrowingService.notifyOverdueForRecord(r.getId())) success++;
        }
        return ResponseEntity.ok(Map.of(
                "total", overdueList.size(),
                "sent", success,
                "failed", overdueList.size() - success
        ));
    }
}
