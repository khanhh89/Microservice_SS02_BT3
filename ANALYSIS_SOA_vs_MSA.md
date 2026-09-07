# Phân tích so sánh SOA và MSA trong bối cảnh LibraX

## Đặt vấn đề

Hệ thống LibraX trước đây dùng kiến trúc SOA với ESB (Enterprise Service Bus) làm trung gian giao tiếp giữa các service. Tất cả message từ `borrowing-service` sang `notification-service` hay `book-service` đều phải đi qua ESB, ESB đảm nhiệm việc định tuyến, chuyển đổi định dạng dữ liệu và xử lý lỗi tập trung.

Khi chuyển sang MSA, các service gọi thẳng nhau qua REST API mà không cần ESB nữa. Bài này phân tích xem sự thay đổi đó ảnh hưởng thế nào đến LibraX.

---

## Ưu điểm khi chuyển sang MSA

### Tốc độ phát triển nhanh hơn

Trong SOA, mỗi khi thay đổi cấu trúc message (ví dụ thêm trường `fineAmount` vào thông báo quá hạn) thì phải cập nhật XML schema trên ESB, test lại pipeline rồi mới deploy từng service. Đây là quy trình chậm và cần nhiều người phối hợp.

Với MSA, `borrowing-service` và `notification-service` chỉ cần thống nhất DTO (ở đây là `OverdueNotificationRequest`). Muốn thêm trường thì sửa class, deploy lại service đó là xong, không đụng chạm gì đến service khác. Với LibraX, điều này có nghĩa là team có thể phát triển và thử nghiệm tính năng mới nhanh hơn rất nhiều.

### Scale độc lập từng service

Vào đầu học kỳ, lượng mượn sách tăng vọt. Với SOA, muốn xử lý lượng request lớn hơn thì phải scale cả ESB lên, tốn tài nguyên. Với MSA, chỉ cần scale `borrowing-service` thêm vài instance, Eureka tự đăng ký, `@LoadBalanced RestTemplate` sẽ tự phân phối request sang các instance mới mà không cần cấu hình thêm gì.

### Cô lập lỗi tốt hơn

Đây là điểm quan trọng nhất liên quan trực tiếp đến đề bài. Trong SOA, ESB là điểm thất bại duy nhất (SPOF) — ESB chết thì toàn bộ hệ thống tê liệt. Trong MSA, nếu `notification-service` chết, `borrowing-service` vẫn xử lý được phiếu mượn bình thường, chỉ phần gửi email là bị ảnh hưởng. Người dùng vẫn mượn sách được, chỉ là không nhận email thông báo thôi. Đây chính là lý do code cũ cần thêm try-catch — để khi `book-service` lỗi thì `borrowing-service` không crash theo.

---

## Nhược điểm khi chuyển sang MSA

### Vận hành phức tạp hơn nhiều

Với SOA và ESB, mọi log đều tập trung ở một chỗ, dễ trace. Với MSA, nếu một luồng mượn sách lỗi, phải xem log của `borrowing-service`, `book-service`, `notification-service` và cả Eureka. Nếu không có distributed tracing (như Zipkin hay Jaeger) thì việc tìm ra lỗi ở đâu rất mất thời gian.

### Vấn đề nhất quán dữ liệu

ESB cũ đảm bảo guaranteed delivery — message gửi đi chắc chắn đến nơi. REST thì không có cơ chế đó. Ví dụ: `borrowing-service` tạo phiếu mượn thành công nhưng khi gọi `PATCH /api/books/{id}/availability` để đánh dấu sách "đang được mượn" thì mạng tắt giữa chừng. Kết quả là phiếu mượn đã tồn tại nhưng sách vẫn hiển thị "available" → người khác có thể mượn cùng cuốn sách đó. Trong SOA với ESB, vấn đề này được xử lý bởi transaction của ESB, còn MSA phải tự implement Saga Pattern để giải quyết.

### Chi phí hạ tầng ban đầu

Team phải tự dựng thêm Eureka Server, cấu hình LoadBalancer, nếu cần Circuit Breaker thì thêm Resilience4j. Trong khi SOA với ESB thương mại (MuleSoft, WSO2) đã có sẵn tất cả những thứ đó.

---

## Kết luận

| Tiêu chí | SOA (ESB) | MSA (REST + Eureka) |
|---|---|---|
| Tốc độ phát triển | Chậm | **Nhanh hơn** |
| Scale | Phải scale cả ESB | **Scale từng service** |
| Chịu lỗi | ESB = SPOF | **Cô lập lỗi tốt hơn** |
| Độ phức tạp vận hành | Thấp | **Cao hơn** (cần thêm tool) |
| Nhất quán dữ liệu | Tốt | **Cần thêm Saga/Outbox** |

Với LibraX, việc chuyển sang MSA giúp mỗi team phụ trách một service có thể làm việc độc lập, deploy riêng mà không ảnh hưởng nhau — phù hợp khi dự án mở rộng. Nhưng team cần đầu tư vào observability (distributed tracing, centralized logging) và xử lý bài toán eventual consistency, không thì sẽ bị "distributed monolith" — tức là code phân tán nhưng vẫn giữ mọi nhược điểm của monolith.
