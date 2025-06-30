package cardindex.dojocardindex.ForgottenPasswordToken.models;


import cardindex.dojocardindex.User.models.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ForgottenPasswordToken {

    @Id
    @Column(nullable = false, unique = true, updatable = false)
    private String token;

    @OneToOne
    private User user;

    @Column(nullable = false)
    private LocalDateTime created;

    @Column(nullable = false)
    private LocalDateTime expires;

    @Column(nullable = false)
    private boolean used;


}
