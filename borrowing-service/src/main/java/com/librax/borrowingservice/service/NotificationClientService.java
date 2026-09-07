package com.librax.borrowingservice.service;

import com.librax.borrowingservice.model.OverdueNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationClientService {

    private final RestTemplate restTemplate;

    // gọi notification-service qua tên logic thay vì IP cố định (giống như book-service)
    private static final String NOTIFICATION_SERVICE = "notification-service";

    public boolean sendOverdueNotification(OverdueNotificationRequest request) {
        String url = "http://" + NOTIFICATION_SERVICE + "/api/notifications/overdue";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OverdueNotificationRequest> body = new HttpEntity<>(request, headers);

        try {
            log.info("Gửi thông báo quá hạn đến {}, borrowingId={}",
                    request.getBorrowerEmail(), request.getBorrowingId());
            var response = restTemplate.postForEntity(url, body, String.class);
            return response.getStatusCode().is2xxSuccessful();

        } catch (ResourceAccessException e) {
            // notification-service không phản hồi, nhưng KHÔNG để borrowing-service crash
            // TODO: sau này có thể đẩy vào retry queue (Kafka/RabbitMQ)
            log.error("notification-service không phản hồi: {}", e.getMessage());
            return false;

        } catch (Exception e) {
            log.error("Lỗi khi gọi notification-service: {}", e.getMessage());
            return false;
        }
    }
}
