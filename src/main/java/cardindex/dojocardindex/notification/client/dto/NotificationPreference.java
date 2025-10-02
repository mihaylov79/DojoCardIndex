package cardindex.dojocardindex.notification.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NotificationPreference {

    private boolean enabled;
}
