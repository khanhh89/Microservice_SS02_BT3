package com.librax.borrowingservice.service;

import com.librax.borrowingservice.model.BorrowingRecord;
import com.librax.borrowingservice.model.BorrowingRecord.BorrowingStatus;
import com.librax.borrowingservice.model.OverdueNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowingService {

    private static final double FINE_PER_DAY = 5000.0; // VND
    private static final int BORROW_DAYS = 14;

    private final BookClientService bookClientService;
    private final NotificationClientService notificationClientService;

    // lưu tạm trong bộ nhớ, thực tế dùng JPA + database
    private final Map<Long, BorrowingRecord> records = new HashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    public Optional<BorrowingRecord> createBorrowing(Long bookId, String email, String name) {
        // kiểm tra sách còn không trước khi tạo phiếu
        if (!bookClientService.isBookAvailable(bookId)) {
            log.warn("Sách {} không khả dụng hoặc book-service lỗi", bookId);
            return Optional.empty();
        }

        String bookTitle = bookClientService.getBookTitle(bookId);
        LocalDate now = LocalDate.now();

        BorrowingRecord r = BorrowingRecord.builder()
                .id(idSeq.getAndIncrement())
                .bookId(bookId)
                .bookTitle(bookTitle)
                .borrowerEmail(email)
                .borrowerName(name)
                .borrowDate(now)
                .dueDate(now.plusDays(BORROW_DAYS))
                .status(BorrowingStatus.ACTIVE)
                .build();

        records.put(r.getId(), r);
        return Optional.of(r);
    }

    public Optional<BorrowingRecord> returnBook(Long id) {
        BorrowingRecord r = records.get(id);
        if (r == null) return Optional.empty();
        r.setReturnDate(LocalDate.now());
        r.setStatus(BorrowingStatus.RETURNED);
        return Optional.of(r);
    }

    public List<BorrowingRecord> getOverdueRecords() {
        LocalDate today = LocalDate.now();
        return records.values().stream()
                .filter(r -> r.getStatus() == BorrowingStatus.ACTIVE && today.isAfter(r.getDueDate()))
                .peek(r -> r.setStatus(BorrowingStatus.OVERDUE))
                .collect(Collectors.toList());
    }

    public boolean notifyOverdueForRecord(Long id) {
        BorrowingRecord r = records.get(id);
        if (r == null) return false;

        LocalDate today = LocalDate.now();
        if (!today.isAfter(r.getDueDate())) return false;

        long days = ChronoUnit.DAYS.between(r.getDueDate(), today);

        OverdueNotificationRequest req = OverdueNotificationRequest.builder()
                .borrowingId(r.getId())
                .borrowerEmail(r.getBorrowerEmail())
                .borrowerName(r.getBorrowerName())
                .bookTitle(r.getBookTitle())
                .dueDate(r.getDueDate())
                .overdueDays(days)
                .fineAmount(days * FINE_PER_DAY)
                .build();

        r.setStatus(BorrowingStatus.OVERDUE);
        return notificationClientService.sendOverdueNotification(req);
    }

    public Optional<BorrowingRecord> findById(Long id) {
        return Optional.ofNullable(records.get(id));
    }

    public List<BorrowingRecord> findAll() {
        return new ArrayList<>(records.values());
    }
}
