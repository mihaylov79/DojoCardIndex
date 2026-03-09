package cardindex.dojocardindex.web.dto;

import cardindex.dojocardindex.Document.model.DocumentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDocumentRequest {

    @NotBlank(message = "Това поле не може да бъде празно!")
    @Size(max = 100, message = "Заглавието не може да надвишава 100 символа")
    private String title;

    @Size(max = 500, message = "Описанието не може да надвишава 500 символа")
    private String description;

    private DocumentCategory category;
}
