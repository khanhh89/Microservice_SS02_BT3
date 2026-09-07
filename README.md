# BT3 — Chuyển đổi từ SOA sang Microservice Architecture (REST API)

## Cấu trúc project

```
bt3/
├── eureka-server/          # Service Registry, chạy trước (port 8761)
├── book-service/           # Quản lý sách (port 8082)
├── borrowing-service/      # Quản lý mượn/trả (port 8083)
├── ANALYSIS_SOA_vs_MSA.md  # Bài phân tích yêu cầu 4
└── README.md
```

---

## Vấn đề và cách sửa (yêu cầu 1 & 2)

### Code cũ (có vấn đề)

```java
@Service
public class BookClientService {
    private RestTemplate restTemplate = new RestTemplate(); // không có load balancing

    public String getBookTitle(Long bookId) {
        String url = "http://192.168.1.15:8082/api/books/" + bookId; // IP cứng
        return restTemplate.getForObject(url, String.class);
        // không có try-catch -> crash khi book-service lỗi
    }
}
```

**Vấn đề 1 — IP cứng:**
book-service khi scale lên nhiều instance thì mỗi instance có IP khác nhau. Code cũ chỉ biết 1 địa chỉ `192.168.1.15:8082`, khi instance đó chết hoặc IP thay đổi (rất thường xảy ra trong môi trường container) thì service sập ngay. Ngoài ra dù scale lên 5 instance đi nữa thì toàn bộ traffic vẫn chỉ dồn vào 1 instance.

**Vấn đề 2 — Không có xử lý lỗi:**
Khi book-service timeout hoặc trả lỗi, RestTemplate ném exception thẳng lên, borrowing-service crash theo → người dùng không mượn được sách vì lý do không liên quan (chỉ là notification đang lỗi chẳng hạn).

### Code đã sửa

**Bước 1:** Tạo `RestTemplateConfig` với `@LoadBalanced`:
```java
@Configuration
public class RestTemplateConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**Bước 2:** Sửa `BookClientService` — inject bean thay vì new, dùng tên logic thay IP:
```java
@Service
@RequiredArgsConstructor
public class BookClientService {
    private final RestTemplate restTemplate; // bean @LoadBalanced
    
    public String getBookTitle(Long bookId) {
        // "book-service" là spring.application.name trong application.yml của book-service
        // Eureka + LoadBalancer sẽ tự resolve sang IP:port của instance đang healthy
        String url = "http://book-service/api/books/" + bookId + "/title";
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (ResourceAccessException e) {
            log.error("book-service không phản hồi: {}", e.getMessage());
            return "Không thể lấy tên sách"; // fallback, không crash
        } catch (HttpClientErrorException e) {
            log.warn("book-service trả lỗi {}", e.getStatusCode());
            return "Sách không tồn tại";
        } catch (HttpServerErrorException e) {
            log.error("book-service lỗi nội bộ {}", e.getStatusCode());
            return "book-service đang có sự cố";
        }
    }
}
```

---

## REST Endpoints

### book-service `:8082`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/books/{id}` | Lấy thông tin sách |
| GET | `/api/books/{id}/title` | Lấy tên sách |
| PATCH | `/api/books/{id}/availability` | Cập nhật trạng thái |

### borrowing-service `:8083`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/borrowings` | Danh sách phiếu mượn |
| GET | `/api/borrowings/{id}` | Chi tiết phiếu mượn |
| POST | `/api/borrowings` | Tạo phiếu mượn |
| PATCH | `/api/borrowings/{id}/return` | Trả sách |
| GET | `/api/borrowings/overdue` | Danh sách phiếu quá hạn |
| POST | `/api/borrowings/{id}/notify-overdue` | Gửi thông báo quá hạn |
| POST | `/api/borrowings/notify-overdue/batch` | Gửi hàng loạt |

Endpoint `POST /api/borrowings/{id}/notify-overdue` là REST thay thế cho thao tác `notifyOverdue` trong ESB ở Bài 2.

---