package cardindex.dojocardindex.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Това поле не може да бъде празно")
    @Size(max = 500, message = "Вашият коментар не може да съдържа повече от 500 символа")
    private String content;
}
