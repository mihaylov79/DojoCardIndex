package cardindex.dojocardindex.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParentConsentRequest {

    @NotBlank
    private String parentEmail;
    @NotBlank
    private String childFirstName;
    @NotBlank
    private String childLastName;
    @NotBlank
    private String consentLink;
}
