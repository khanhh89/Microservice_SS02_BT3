package com.librax.borrowingservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO dùng để gửi thông báo quá hạn (notifyOverdue) sang notification-service
 * thông qua REST API — thay thế cơ chế ESB message trong mô hình SOA cũ.
 *
 * Trong SOA, thông báo này được gửi qua ESB với XML schema cứng nhắc.
 * Trong MSA, chúng ta dùng JSON DTO nhẹ hơn, dễ thay đổi hơn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverdueNotificationRequest {

    /** ID phiếu mượn quá hạn */
    private Long borrowingId;

    /** Email người mượn để gửi thông báo */
    private String borrowerEmail;

    /** Tên người mượn */
    private String borrowerName;

    /** Tiêu đề cuốn sách bị quá hạn */
    private String bookTitle;

    /** Ngày đến hạn trả (đã qua) */
    private LocalDate dueDate;

    /** Số ngày quá hạn */
    private long overdueDays;

    /** Số tiền phạt (VND / ngày x số ngày) */
    private double fineAmount;
}
