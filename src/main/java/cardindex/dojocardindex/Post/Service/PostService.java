package cardindex.dojocardindex.Post.Service;

import cardindex.dojocardindex.Post.Repository.PostRepository;
import cardindex.dojocardindex.Post.models.Post;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.web.dto.CreatePostRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;

    @Autowired
    public PostService(PostRepository postRepository, UserService userService) {
        this.postRepository = postRepository;
        this.userService = userService;
    }

    public List<Post> getAllUnreadPosts(){
        return postRepository.findAll(Sort.by(Sort.Order.desc("created")));
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

    }

    public Post getPostById(UUID postId){

        return postRepository.findById(postId)
                .orElseThrow(()-> new EntityNotFoundException("Публикацията не е намерена."));
    }
}
