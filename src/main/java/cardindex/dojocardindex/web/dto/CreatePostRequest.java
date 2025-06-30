package cardindex.dojocardindex.web.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequest {

    @NotBlank(message = "Това поле не може да бъде празно")
    private String title;

    @NotBlank(message = "Това поле не може да бъде празно")
    @Size(max = 1000, message = "Вашият пост не може да съдържа повече от 1000 символа")
    private String content;


}
