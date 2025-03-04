package cardindex.dojocardindex.web.dto;

import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.models.Requirements;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class EditEventRequest {

        private EventType eventType;

        private String eventDescription;

        private LocalDate startDate;

        private LocalDate endDate;

        private Requirements requirements;

}
