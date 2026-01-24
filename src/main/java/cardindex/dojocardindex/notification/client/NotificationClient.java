package cardindex.dojocardindex.notification.client;


import cardindex.dojocardindex.notification.client.dto.Notification;
import cardindex.dojocardindex.notification.client.dto.NotificationPreferenceRequest;
import cardindex.dojocardindex.notification.client.dto.NotificationPreference;
import cardindex.dojocardindex.notification.client.dto.NotificationRequest;
import cardindex.dojocardindex.web.dto.ForgottenPasswordRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "mail-svc", url = "https://mail-svc-app-container-app.yellowdesert-a725fdfe.switzerlandnorth.azurecontainerapps.io/api/v1/notifications")
public interface NotificationClient {

    @PostMapping("/preferences")
    ResponseEntity<Void> updateNotificationPreference(@RequestBody NotificationPreferenceRequest preferenceRequest);

    @GetMapping("/preferences")
    ResponseEntity<NotificationPreference>getUserMailPreference(@RequestParam(name = "recipientId") UUID recipientId);

    @GetMapping
    ResponseEntity<List<Notification>>getUserNotificationHistory(@RequestParam(name = "recipientId") UUID recipientId);

    @PostMapping
    ResponseEntity<Void>sendEmail(@RequestBody NotificationRequest notificationRequest);

    @PutMapping("/preferences")
    ResponseEntity<Void>changeNotificationPreferences(@RequestParam(name = "recipientId") UUID recipientId, @RequestParam(name = "enabled") boolean enabled);

    @DeleteMapping
    ResponseEntity<Void> clearNotificationHistory(@RequestParam(name = "recipientId") UUID recipientId);

    @PostMapping("/forgotten-password")
    ResponseEntity<Void>sendForgottenPasswordEmail(@RequestBody ForgottenPasswordRequest request);

}