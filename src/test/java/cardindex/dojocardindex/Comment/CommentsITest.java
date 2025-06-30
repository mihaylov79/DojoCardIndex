package cardindex.dojocardindex.Comment;

import cardindex.dojocardindex.Comment.Repository.CommentRepository;
import cardindex.dojocardindex.Comment.Service.CommentService;
import cardindex.dojocardindex.Comment.models.Comment;
import cardindex.dojocardindex.Post.Repository.PostRepository;
import cardindex.dojocardindex.Post.models.Post;
import cardindex.dojocardindex.User.models.*;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.web.dto.CreateCommentRequest;
import cardindex.dojocardindex.web.dto.CreateUserRequest;
import cardindex.dojocardindex.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
public class CommentsITest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testAddCommentToPost() {

        CreateUserRequest userDto = getTestCreateUserRequest();

        RegisterRequest registerDTO = getTestRegisterRequest();

        userService.createNewUser(userDto);
        userService.register(registerDTO);
        userService.approveRequest(userRepository.getByEmail("user1@examplez.com").getId());



        Post post = Post.builder().title("Тестова публикация").content("текст").author(userRepository.getByEmail("user1@examplez.com")).build();
        postRepository.save(post);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Това е коментар");


        commentService.addComment(post.getId(), userRepository.getByEmail("user1@examplez.com"), request);

        List<Comment> comments = commentRepository.findByPost_Id(post.getId());
        assertEquals(1, comments.size());
        assertEquals("Това е коментар", comments.get(0).getContent());
    }




    @Test
    public void testDeleteCommentFromPost() {

        CreateUserRequest userDto = getTestCreateUserRequest();

        RegisterRequest registerDTO = getTestRegisterRequest();

        userService.createNewUser(userDto);
        userService.register(registerDTO);
        userService.approveRequest(userRepository.getByEmail("user1@examplez.com").getId());

        Post post = Post.builder().title("Тестова публикация").content("текст").author(userRepository.getByEmail("user1@examplez.com")).build();
        postRepository.save(post);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Това е коментар");

        commentService.addComment(post.getId(), userRepository.getByEmail("user1@examplez.com"), request);
        Comment comment = commentRepository.findByPost_Id(post.getId()).get(0);

        // Act
        commentService.deleteComment(comment.getId(), userRepository.getByEmail("user1@examplez.com"));

        // Assert
        List<Comment> commentsAfterDeletion = commentRepository.findByPost_Id(post.getId());
        assertTrue(commentsAfterDeletion.isEmpty());
    }



    private static CreateUserRequest getTestCreateUserRequest() {
        return CreateUserRequest.builder()
                .email("user1@examplez.com")
                .role(UserRole.ADMIN)
                .reachedDegree(Degree.NONE)
                .firstName("Ivan")
                .lastName("Ivanov").build();

    }

    private static RegisterRequest getTestRegisterRequest() {
        return RegisterRequest.builder()
                .email("user1@examplez.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .password("123321").build();

    }


}


