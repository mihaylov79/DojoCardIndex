package cardindex.dojocardindex.notification.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class NotificationRequest {

    private UUID recipientId;

    private String firstName;

    private String lastName;

    private String title;

    private String content;
}
