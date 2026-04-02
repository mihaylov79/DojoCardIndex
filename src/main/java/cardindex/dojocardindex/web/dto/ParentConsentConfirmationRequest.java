package cardindex.dojocardindex.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ParentConsentConfirmationRequest {

    @NotBlank
    private String parentEmail;

    @NotBlank
    private String childFirstName;

    @NotBlank
    private String childLastName;

    @NotBlank
    private String agreementTitle;

    @NotBlank
    private String agreementContent;

    @NotBlank
    private LocalDateTime parentConsentAt;

    @NotBlank
    private UUID consentId;
}
