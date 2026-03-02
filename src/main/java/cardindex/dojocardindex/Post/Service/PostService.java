package cardindex.dojocardindex.Post.Service;

import cardindex.dojocardindex.Comment.models.Comment;
import cardindex.dojocardindex.Post.Repository.PostRepository;
import cardindex.dojocardindex.Post.models.Post;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.telegram.TelegramNotificationService;
import cardindex.dojocardindex.web.dto.CreatePostRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final TelegramNotificationService telegramNotificationService;

    @Autowired
    public PostService(PostRepository postRepository, UserService userService, TelegramNotificationService telegramNotificationService) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.telegramNotificationService = telegramNotificationService;
    }

    public List<Post> getAllUnreadPosts(){
        return postRepository.findAllByIsReadIsFalse(Sort.by(Sort.Order.desc("created")));
    }


    public void createNewPost(CreatePostRequest createPostRequest){

//        String senderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User sender = userService.getCurrentUser();

        Post post = Post.builder()
                .author(sender)
                .created(LocalDateTime.now())
                .title(createPostRequest.getTitle())
                .content(createPostRequest.getContent())
                .isRead(false)
                .build();

        postRepository.save(post);

        try {
            telegramNotificationService.notifyNewPost(post);
            log.info("Telegram нотофикация за пост: {} е изпратена", post.getTitle());
        } catch (Exception e) {
            log.error("Грешка при изпращане на Telegram нотификация",e);
        }

    }

    public Post getPostById(UUID postId){

        return postRepository.findById(postId)
                .orElseThrow(()-> new EntityNotFoundException("Публикацията не е намерена."));
    }

    public void closePost(UUID postId){
        Post post = getPostById(postId);

        post = post.toBuilder()
                .isRead(true)
                .build();

        postRepository.save(post);
    }

    @Transactional
    public void deleteOldPosts(LocalDateTime date) {
        // Извличаме всички постове, създадени преди 6 месеца
        List<Post> oldPosts = postRepository.findAllByCreatedBeforeAndIsReadIsTrue(date);

        log.warn("Открити стари Постове за изтриване - {}",oldPosts.size());

        for (Post post : oldPosts) {
            log.warn("Изтривам пост с ID: {}", post.getId());

            // Премахвам коментарите на потребителите, свързани с този пост
            for (Comment comment : post.getComments()) {
                if (comment.getCommentAuthor() != null) {
                    // Извикваме метода в User, за да премахна коментарите
                    comment.getCommentAuthor().removeCommentsByPost(post);
                }
            }

            // Изтривам самия пост
            postRepository.delete(post);

        }
        log.warn("Изтрити постове постове създадени преди повече от 6 месеца : {}",oldPosts.size());
    }
}
