package cardindex.dojocardindex.Utils;

import cardindex.dojocardindex.Comment.Repository.CommentRepository;
import cardindex.dojocardindex.Message.Repository.MessageRepository;
import cardindex.dojocardindex.Post.Repository.PostRepository;
import cardindex.dojocardindex.Post.models.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CleanUpService {

    private final MessageRepository messageRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CleanUpService(MessageRepository messageRepository, CommentRepository commentRepository, PostRepository postRepository) {
        this.messageRepository = messageRepository;
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }


//    TODO Да добавя Log за изтритите коментари и съобщения
    @Transactional
    @Scheduled(cron = "0 0 0 1 */3 * ")
    public void deleteOldRecords(){
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);

        List<Post>oldPosts = postRepository.findAllByCreatedBefore(threeMonthsAgo);

        if (!oldPosts.isEmpty()){
            log.warn("Открити стари постове за изтриване : {}",oldPosts.size());
            commentRepository.deleteByPostIn(oldPosts);

            postRepository.deleteAll(oldPosts);
            log.warn("Изтрити стари постове: {}",oldPosts.size());
        }

        messageRepository.deleteByCreatedBefore(threeMonthsAgo);
    }
}
