package cardindex.dojocardindex.web.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Това поле не може да бъде празно")
    @Email(message = "Въведете валиден мейл адрес")
    private String recipient;

//    @NotBlank(message = "Това поле не може да бъде празно")
//    private User sender;

    @NotBlank(message = "Това поле не може да бъде празно")
    @Size(max = 300, message = "Вашето съобщение не може да надвишава 300 символа")
    private String content;


    private LocalDateTime created;


    private boolean isRead;
}
