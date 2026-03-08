package cardindex.dojocardindex.web.dto;

import cardindex.dojocardindex.Document.model.DocumentCategory;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDocumentRequest {

    @Size(max = 100, message = "Заглавието не може да надвишава 100 символа")
    private String title;

    @Size(max = 500, message = "Описанието не може да надвишава 500 символа")
    private String description;

    private DocumentCategory category;
}
