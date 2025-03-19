package cardindex.dojocardindex.Comment.Repository;

import cardindex.dojocardindex.Comment.models.Comment;
import cardindex.dojocardindex.Post.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPost_Id(UUID postId);

    @Transactional
    void deleteByPostIn(Collection<Post> posts);
}
