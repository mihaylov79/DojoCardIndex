package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Post.Service.PostService;
import cardindex.dojocardindex.Post.models.Post;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreatePostRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;


@Controller
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;

    public PostController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getPostsPage(@AuthenticationPrincipal CustomUserDetails details){

        User user = userService.getUserById(details.getId());
        List<Post> allActivePosts = postService.getAllUnreadPosts();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("posts");
        modelAndView.addObject("user", user);
        modelAndView.addObject("allActivePosts",allActivePosts);

        return modelAndView;
    }

    @GetMapping("/create")
    public ModelAndView getPostCreatePage(@AuthenticationPrincipal CustomUserDetails details){
        User user  = userService.getUserById(details.getId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("post-request");
        modelAndView.addObject("user", user);
        modelAndView.addObject("createPostRequest", new CreatePostRequest());

        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @PostMapping("/create")
    public ModelAndView createNewPost(@AuthenticationPrincipal CustomUserDetails details, CreatePostRequest createPostRequest, BindingResult result){

        User user = userService.getUserById(details.getId());

        if (result.hasErrors()){
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("post-request");
            modelAndView.addObject("user", user);

            return modelAndView;
        }
            postService.createNewPost(createPostRequest);

        return new ModelAndView("redirect:/posts");
    }



}
