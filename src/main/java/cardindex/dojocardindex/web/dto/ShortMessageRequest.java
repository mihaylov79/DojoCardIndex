package cardindex.dojocardindex.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShortMessageRequest {


    @Size(min = 1,max = 300, message = "Съобщението не може да бъде празно поле или да надвишава 300 символа.")
    private String messageContent;
}
