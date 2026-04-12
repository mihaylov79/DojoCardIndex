package cardindex.dojocardindex.web.dto;

import cardindex.dojocardindex.UserConsent.model.CancelInitiator;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CancellationConfirmationRequest {

    @NotBlank
    private String recipientMail;

    @NotBlank
    private String userFirstName;

    @NotBlank
    private String userLastName;

    @NotBlank
    private String agreementTitle;

    @NotBlank
    private LocalDateTime cancelledAt;

    private CancelInitiator cancelInitiatedBy;

    @NotBlank
    private UUID consentId;
}
