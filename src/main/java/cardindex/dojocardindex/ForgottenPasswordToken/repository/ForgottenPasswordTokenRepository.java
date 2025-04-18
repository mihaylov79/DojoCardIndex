package cardindex.dojocardindex.ForgottenPasswordToken.repository;

import cardindex.dojocardindex.ForgottenPasswordToken.models.ForgottenPasswordToken;
import cardindex.dojocardindex.User.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ForgottenPasswordTokenRepository extends JpaRepository<ForgottenPasswordToken,String> {
    Optional<ForgottenPasswordToken> findByToken(String token);

    void deleteByUser(User user);
}
