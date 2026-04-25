package cardindex.dojocardindex.UserConsent.model;


import cardindex.dojocardindex.Agreement.model.Agreement;
import cardindex.dojocardindex.User.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @ManyToOne
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    @Column(name = "is_minor")
    private boolean isMinor;

    @Column(name = "parent_email")
    private String parentEmail;

    @Column(name = "sent_invitation_mail_status")
    @Enumerated(EnumType.STRING)
    private MailSendStatus sentInvitationMailStatus;

    @Column(name = "sent_confirmation_mail_status")
    @Enumerated(EnumType.STRING)
    private MailSendStatus sentConfirmationMailStatus;

    @Column(name = "cancellation_confirmation_mail_status")
    private MailSendStatus cancellationConfirmationMailStatus;

    @Column(name = "consent_token", unique = true)
    private String consentToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "parent_consented_at")
    private LocalDateTime parentConsentedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column
    private boolean pending;

    @Column(name = "pending_reason")
    private String pendingReason;

    @Column
    private boolean finished;

    @Column
    private boolean canceled = false;

    @Column
    private LocalDateTime canceledAt;

    @ManyToOne
    @JoinColumn(name = "canceled_by_id")
    private User canceledBy;

    @Column(name = "cancel_initiated_by")
    @Enumerated(EnumType.STRING)
    private CancelInitiator cancelInitiatedBy;



    //Валидация на съгласието
    public boolean isFullyConsented() {
        if(canceled) return false;
        if (pending) return true;
        if (!isMinor) return agreedAt != null;
        return agreedAt != null && parentConsentedAt != null;
    }

    public boolean isTokenValid() {
        return tokenExpiresAt != null && LocalDateTime.now().isBefore(tokenExpiresAt);
    }

}
