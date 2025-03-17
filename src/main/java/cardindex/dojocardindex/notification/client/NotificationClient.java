package cardindex.dojocardindex.notification.client;


import cardindex.dojocardindex.notification.client.dto.NotificationPreferenceRequest;
import cardindex.dojocardindex.notification.client.dto.NotificationPreference;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "mail-svc", url = "http://localhost:8081/api/v1/notifications")
public interface NotificationClient {

    @PostMapping("/preferences")
    ResponseEntity<Void> updateNotificationPreference(@RequestBody NotificationPreferenceRequest preferenceRequest);

    @GetMapping("/preferences")
    ResponseEntity<NotificationPreference>getUserMailPreference(@RequestParam(name = "recipientId") UUID recipientId);

}