package cardindex.dojocardindex.Post;


import cardindex.dojocardindex.Comment.Repository.CommentRepository;
import cardindex.dojocardindex.Comment.models.Comment;
import cardindex.dojocardindex.Post.Repository.PostRepository;
import cardindex.dojocardindex.Post.Service.PostService;
import cardindex.dojocardindex.Post.models.Post;
import cardindex.dojocardindex.User.models.Degree;
import cardindex.dojocardindex.User.models.UserRole;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.web.dto.CreatePostRequest;
import cardindex.dojocardindex.web.dto.CreateUserRequest;
import cardindex.dojocardindex.web.dto.RegisterRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PostITest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;

    @Test
    void testCreateNewPost(){

        createTestUserWithSecurityContext();

        CreatePostRequest postDTO = new CreatePostRequest();
        postDTO.setTitle("Заглавие");
        postDTO.setContent("Публикация");

        postService.createNewPost(postDTO);

        assertEquals(1,postRepository.findAll().size());

    }

    @Test
    void testClosePost(){


        createTestUserWithSecurityContext();

        Post post = Post.builder()
                .title("Заглавие")
                .content("Публикация")
                .author(userRepository.getByEmail("user1@examplez.com"))
                .created(LocalDateTime.now())
                .isRead(true)
                .build();

        postRepository.save(post);



        postService.closePost(post.getId());

        //Проверява дали стойността на isRead e променена на true
        assertTrue(post.isRead());

    }

    @Test
    void testDeleteOldPosts(){

        createTestUserWithSecurityContext();

        Post oldPost = Post.builder()
                .title("Заглавие")
                .content("Публикация")
                .author(userRepository.getByEmail("user1@examplez.com"))
                .created(LocalDateTime.now().minusMonths(7))
                .isRead(true)
                .build();

        postRepository.save(oldPost);

        Post newPost = Post.builder()
                .title("Заглавие")
                .content("Публикация")
                .author(userRepository.getByEmail("user1@examplez.com"))
                .created(LocalDateTime.now())
                .isRead(true)
                .build();

        postRepository.save(newPost);

        Comment comment = Comment.builder()
                .commentAuthor(userRepository.getByEmail("admin@example.com"))
                .content("Коментар")
                .post(oldPost)
                .commented(LocalDateTime.now().minusMonths(6))
                .build();
        commentRepository.save(comment);
        assertEquals(1,commentRepository.findAll().size());

        //Проверяваме дали 2 поста са създадени в базата
        assertEquals(2,postRepository.findAll().size());

        postService.deleteOldPosts(LocalDateTime.now().minusDays(1));
        //Проверяваме дали поста е изтрит от Базата
        assertEquals(1,postRepository.findAll().size());
        UUID postId = postRepository.findAll().stream().findFirst().get().getId();
        assertEquals(newPost.getId(),postId);
        assertTrue(commentRepository.findAll().isEmpty());
    }


    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void createTestUserWithSecurityContext() {
        CreateUserRequest userDto= CreateUserRequest.builder()
                .email("user1@examplez.com")
                .role(UserRole.ADMIN)
                .reachedDegree(Degree.NONE)
                .firstName("Ivan")
                .lastName("Ivanov").build();

        RegisterRequest registerDTO = RegisterRequest.builder()
                .email("user1@examplez.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .password("123321").build();

        userService.createNewUser(userDto);
        userService.register(registerDTO);
        userService.approveRequest(userRepository.getByEmail("user1@examplez.com").getId());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user1@examplez.com",
                null,
                Collections.emptyList());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }


}
