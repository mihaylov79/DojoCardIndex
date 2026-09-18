package cardindex.dojocardindex.notification.service;

import cardindex.dojocardindex.notification.client.NotificationClient;
import cardindex.dojocardindex.notification.client.dto.Notification;
import cardindex.dojocardindex.notification.client.dto.NotificationPreferenceRequest;
import cardindex.dojocardindex.notification.client.dto.NotificationPreference;
import cardindex.dojocardindex.notification.client.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<NotificationService> selfProvider; //решава проблема с извикване на кеширан метод в рамките на същият клас -
                                                                    // където извикването става директно в обекта и не минава през Spring проксито.
                                                                    // Решава проблема като Spring автоматично разрешава проксито в момента на извикване на selfProvider.getObject().
                                                                    // selfProvider.getObject(): Връща вече създадения от Spring контекста singleton бийн (неговото CGLIB/AOP прокси).
                                                                    // Това е обикновена операция по четене на референция от паметта, която отнема наносекунди.
                                                                    // Няма нови нишки или блокирания Няма нови нишки или блокирания: Операцията не създава нови нишки.
                                                                    // Справяне с циклични зависимости - ObjectProvider отлага извличането на бийна до момента на реалното извикване, което елиминира цикъла.


    @Autowired
    public NotificationService(NotificationClient notificationClient, ObjectProvider<NotificationService> selfProvider) {
        this.notificationClient = notificationClient;

        this.selfProvider = selfProvider;
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
        NotificationPreference preference = selfProvider.getObject().getUserNotificationPreference(recipientID);

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
