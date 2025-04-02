package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Post.Service.PostService;
import cardindex.dojocardindex.Post.models.Post;
import cardindex.dojocardindex.User.models.*;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreatePostRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
public class PostControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private PostService postService;

    @Test
    public void getPostsPage_ShouldReturnPostsPage_WhenAuthenticated() throws Exception {

        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = createTestUserDetails(userId);

        User mockUser = User.builder()
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .build();

        Post post1 = Post.builder()
                .Id(UUID.randomUUID())
                .title("First Post")
                .content("Content of first post")
                .author(mockUser)
                .isRead(false)
                .build();

        Post post2 = Post.builder()
                .Id(UUID.randomUUID())
                .title("Second Post")
                .content("Content of second post")
                .author(mockUser)
                .isRead(false)
                .build();

        List<Post> mockPosts = Arrays.asList(post1, post2);

        when(userService.getUserById(userDetails.getId())).thenReturn(mockUser);
        when(postService.getAllUnreadPosts()).thenReturn(mockPosts);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );

        mockMvc.perform(get("/posts").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("posts"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("allActivePosts"))
                .andExpect(model().attribute("user", mockUser))
                .andExpect(model().attribute("allActivePosts", mockPosts));
    }

    @Test
    public void getPostsPage_ShouldReturnUnauthorized_WhenUnauthenticated() throws Exception {

        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPostCreatePage_shouldReturnPostRequestView_whenUserIsAuthorized() throws Exception {

        UUID userId = UUID.randomUUID();
        CustomUserDetails adminDetails = createAdminUserDetails(userId);

        User mockUser = User.builder()
                .id(adminDetails.getId())
                .email(adminDetails.getEmail())
                .firstName("Ivan")
                .lastName("Ivanov")
                .role(adminDetails.getRole())
                .registrationStatus(adminDetails.getRegistrationStatus())
                .status(adminDetails.getUserStatus())
                .reachedDegree(Degree.NONE)
                .build();

        when(userService.getUserById(userId)).thenReturn(mockUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        "password",
                        adminDetails.getAuthorities()
                )
        );

        mockMvc.perform(get("/posts/create").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("post-request"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("createPostRequest"));
    }

    @Test
    void createNewPost_shouldRedirectToPosts_whenRequestIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserDetails adminDetails = createAdminUserDetails(userId);

        User mockUser = User.builder()
                .id(adminDetails.getId())
                .email(adminDetails.getEmail())
                .firstName("Ivan")
                .lastName("Ivanov")
                .role(adminDetails.getRole())
                .registrationStatus(adminDetails.getRegistrationStatus())
                .status(adminDetails.getUserStatus())
                .reachedDegree(Degree.NONE)
                .build();

        CreatePostRequest createPostRequest = new CreatePostRequest();
        createPostRequest.setTitle("Заглавие");
        createPostRequest.setContent("Тест!");

        when(userService.getUserById(userId)).thenReturn(mockUser);


        mockMvc.perform(post("/posts/create")
                        .with(user(adminDetails)).with(csrf())
                        .param("title", "Заглавие")
                        .param("content", "Тест"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));
    }

    @Test
    void createNewPost_shouldReturnPostRequestView_whenValidationFails() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserDetails adminDetails = createAdminUserDetails(userId);

        User mockUser = User.builder()
                .id(adminDetails.getId())
                .email(adminDetails.getEmail())
                .firstName("Ivan")
                .lastName("Ivanov")
                .role(adminDetails.getRole())
                .registrationStatus(adminDetails.getRegistrationStatus())
                .status(adminDetails.getUserStatus())
                .reachedDegree(Degree.NONE)
                .build();

        when(userService.getUserById(userId)).thenReturn(mockUser);

        mockMvc.perform(post("/posts/create")
                        .with(user(adminDetails))
                        .param("title", "")
                        .param("content", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("post-request"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("createPostRequest"))
                .andExpect(model().hasErrors());
    }



    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static CustomUserDetails createAdminUserDetails(UUID userId) {
        return new CustomUserDetails(
                userId,
                "admin@example.com",
                "password",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE);

    }

    private static CustomUserDetails createTestUserDetails(UUID userId) {
        return new CustomUserDetails(
                userId,
                "user2@example.com",
                "password",
                UserRole.MEMBER,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE);
    }
}
