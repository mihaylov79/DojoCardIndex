package cardindex.dojocardindex.notification.service;

import cardindex.dojocardindex.notification.client.NotificationClient;
import cardindex.dojocardindex.notification.client.dto.NotificationPreferenceRequest;
import cardindex.dojocardindex.notification.client.dto.NotificationPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationClient notificationClient;
//    private final UserService userService;


    @Autowired
    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;

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

    public NotificationPreference getUserNotificationPreference(UUID recipientId) {
        ResponseEntity<NotificationPreference> httpResponse = notificationClient.getUserMailPreference(recipientId);
        if (!httpResponse.getStatusCode().is2xxSuccessful()){
            throw new RuntimeException("HTTP грешка: Неуспешен отговор за потребител [%s]".formatted(recipientId));
        }

        return httpResponse.getBody();
    }

}
