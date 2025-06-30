package cardindex.dojocardindex.notification.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notification {

private LocalDateTime created;

private String title;

private String status;
}
