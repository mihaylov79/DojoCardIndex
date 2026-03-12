package cardindex.dojocardindex.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgreementRequest {

    @NotBlank(message = "Това поле не може да бъде празно!")
    private String title;

    @NotBlank(message = "Това поле не може да бъде празно!")
    private String content;
}
