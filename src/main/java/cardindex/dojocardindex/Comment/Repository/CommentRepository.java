package cardindex.dojocardindex.Comment.Repository;

import cardindex.dojocardindex.Comment.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPost_Id(UUID postId);
}
