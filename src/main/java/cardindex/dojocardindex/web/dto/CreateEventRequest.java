package cardindex.dojocardindex.web.dto;

import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.models.Requirements;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;


@Data
public class CreateEventRequest {

    private EventType eventType;

    @NotBlank(message = "Това поле не може да бъде празно")
    private String eventDescription;

    @NotBlank(message = "Това поле не може да бъде празно")
    private LocalDate startDate;

    @NotBlank(message = "Това поле не може да бъде празно")
    private LocalDate endDate;

    @NotBlank(message = "Това поле не може да бъде празно")
    private String location;

    private Requirements requirements;
}
