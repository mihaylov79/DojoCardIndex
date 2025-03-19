package cardindex.dojocardindex.Message.Repository;

import cardindex.dojocardindex.Message.models.Message;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByRecipient_IdAndIsReadFalse(UUID recipientId, Sort sort);

    @Transactional
    void deleteByCreatedBefore(LocalDateTime createdBefore);
}
