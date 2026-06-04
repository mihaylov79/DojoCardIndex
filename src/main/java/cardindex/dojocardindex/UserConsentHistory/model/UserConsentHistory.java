package cardindex.dojocardindex.UserConsentHistory.model;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "user_consent_history")
public class UserConsentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "consent_id")
    private UserConsent consent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentHistoryAction action;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;

    @ManyToOne
    @JoinColumn(name = "action_by_id")
    private User actionBy;

    @Column
    private String reason;

    @Column(length = 500)
    private String notes;

}
