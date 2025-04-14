package cardindex.dojocardindex.User.repository;

import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserStatus;
import org.springframework.data.domain.Sort;
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

    List<User> findAllByIdIsNot(UUID senderId);

    List<User> findAllByIdIsNotAndRegistrationStatusAndStatus(UUID id, RegistrationStatus registrationStatus, UserStatus status);

    List<User> findAllByStatus(UserStatus status, Sort sort);
}
