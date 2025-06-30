package cardindex.dojocardindex.notification.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class NotificationPreferenceRequest {


    private UUID recipientId;


    private boolean enabled;


    private String info;
}
