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

    @Column(name = "sent_mail_status")
    @Enumerated(EnumType.STRING)
    private MailSendStatus sentMailStatus;

    @Column(name = "consent_token", unique = true)
    private String consentToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "parent_consented_at")
    private LocalDateTime parentConsentedAt;

    @Column
    private boolean pending;

    @Column(name = "pending_reason")
    private String pendingReason;

    @Column
    private boolean finished;

    //Валидация на съгласието
    public boolean isFullyConsented() {
        if (pending) return true;
        if (!isMinor) return agreedAt != null;
        return agreedAt != null && parentConsentedAt != null;
    }

    public boolean isTokenValid() {
        return tokenExpiresAt != null && LocalDateTime.now().isBefore(tokenExpiresAt);
    }

}
