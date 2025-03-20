package cardindex.dojocardindex.Post.Repository;

import cardindex.dojocardindex.Post.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {


    List<Post> findAllByCreatedBeforeAndIsReadIsTrue(LocalDateTime date);
}
