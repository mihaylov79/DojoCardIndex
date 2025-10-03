package cardindex.dojocardindex.notification.service;

import cardindex.dojocardindex.notification.client.NotificationClient;
import cardindex.dojocardindex.notification.client.dto.Notification;
import cardindex.dojocardindex.notification.client.dto.NotificationPreferenceRequest;
import cardindex.dojocardindex.notification.client.dto.NotificationPreference;
import cardindex.dojocardindex.notification.client.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationClient notificationClient;


    @Autowired
    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;

    }

    @Async
    public void checkNotificationPreference(UUID recipientId, String email) {
        try {
            NotificationPreferenceRequest preferenceRequest = NotificationPreferenceRequest.builder()
                    .recipientId(recipientId)
                    .enabled(true) // по подразбиране включено
                    .info(email)
                    .build();

            notificationClient.updateNotificationPreference(preferenceRequest);
        } catch (Exception e) {
            log.error("Грешка при проверка/създаване на предпочитания за известия за потребител [{}]", recipientId, e);
        }
    }



    public void saveNotificationPreference(UUID recipientId, boolean notification,String email){

        NotificationPreferenceRequest preferenceRequest = NotificationPreferenceRequest.builder()
                                                            .recipientId(recipientId)
                                                            .enabled(notification)
                                                            .info(email)
                                                            .build();

        try {
            ResponseEntity<Void> httpResponse = notificationClient
                                                    .updateNotificationPreference(preferenceRequest);
            if (!httpResponse.getStatusCode().is2xxSuccessful()){
                log.error("[Грешка при Feign заявка към notification-svc] Неуспешно запазване на настройки за известия за потребител с идентификация = [{}]", recipientId);
            }
        }catch (Exception e){
            log.error("Грешка при комуникацията с mail-svc",e);
        }

    }
    @Cacheable(value = "notification-preference", key = "#recipientId")
    public NotificationPreference getUserNotificationPreference(UUID recipientId) {
        try {
            ResponseEntity<NotificationPreference> httpResponse = notificationClient.getUserMailPreference(recipientId);

            if (!httpResponse.getStatusCode().is2xxSuccessful() || httpResponse.getBody() == null){
                // Ако няма preference, върнете default стойност
                log.warn("Няма notification preference за потребител [{}]. Връщам default.", recipientId);
                return NotificationPreference.builder()
                        .enabled(false)
                        .build();
            }

            return httpResponse.getBody();
        } catch (Exception e) {
            // Fallback при грешка
            log.error("Грешка при четене на preference за [{}]. Връщам default.", recipientId, e);
            return NotificationPreference.builder()
                    .enabled(false)
                    .build();
        }
    }
    @Cacheable(value = "notification-history", key = "#recipientId")
    public List<Notification> getNotificationHistory(UUID recipientId) {
        ResponseEntity<List<Notification>> httpResponse = notificationClient
                                                                    .getUserNotificationHistory(recipientId);
        return httpResponse.getBody();
    }
    @CacheEvict(value = "notification-history", key = "#recipientID")
    public void sendNotification(UUID recipientID, String firstName, String lastName, String title, String content) {
        // Проверка на предпочитанията за известия
        NotificationPreference preference = getUserNotificationPreference(recipientID);

        if (preference.isEnabled()) {
            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .title(title)
                    .content(content)
                    .recipientId(recipientID)
                    .firstName(firstName)
                    .lastName(lastName)
                    .build();

            ResponseEntity<Void> httpResponse;

            try {
                httpResponse = notificationClient.sendEmail(notificationRequest);

                if (!httpResponse.getStatusCode().is2xxSuccessful()) {
                    log.error("[Грешка при Feign заявка към notification-svc] Известие към потребител с идентификация [{}] не беше изпратено!", recipientID);
                }
            } catch (Exception e) {
                log.warn("Известие към потребител с идентификация [{}] не беше изпратено!", recipientID, e);
            }
        } else {
            log.warn("Известията за потребител с ID [{}] са изключени. Известие няма да бъде изпратено.", recipientID);
        }
    }
    @CacheEvict(value = "notification-preference", key = "#recipientId")
    public void changeNotificationPreferences(UUID recipientId,boolean enabled){
        try {
            notificationClient.changeNotificationPreferences(recipientId, enabled);
        } catch (Exception e) {
            log.warn("Неочаквана грешка при промяна на настройки за известия за потребител [{}]",recipientId,e);
        }
    }
    @CacheEvict(value = "notification-history", key = "#recipientId")
    public void removeUserNotificationHistory(UUID recipientId){

        try{
            notificationClient.clearNotificationHistory(recipientId);
        } catch (Exception e) {
            log.error("Не може да бъде установена връзка с mail-svc за да бъде изчистена историята на известията за потребител с идентификация - [{}]",recipientId,e);
        }

    }

}
