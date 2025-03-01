package cardindex.dojocardindex.User.repository;

import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {


    Optional<User> findByEmail(String email);

    User getByEmail(String email);


    List<User> findByStatusAndRegistrationStatus(UserStatus status, RegistrationStatus registrationStatus);

    List<User> findByRegistrationStatus(RegistrationStatus registrationStatus);
}
