package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Comment.Service.CommentService;
import cardindex.dojocardindex.Post.Service.PostService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreateCommentRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final PostService postService;
    private final UserService userService;

    @Autowired
    public CommentController(CommentService commentService, PostService postService, UserService userService) {
        this.commentService = commentService;
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping("/new-comment")
    public ModelAndView getNewCommentPage(@RequestParam UUID postId, @AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("new-comment");
        modelAndView.addObject("currentUser",currentUser);
        modelAndView.addObject("postId", postId);
        modelAndView.addObject("createCommentRequest", new CreateCommentRequest());

        return modelAndView;
    }

    @PostMapping("/new-comment")
    public ModelAndView createNewComment(@RequestParam UUID postId,
                                         @Valid CreateCommentRequest createCommentRequest,
                                         BindingResult result,
                                         @AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());
        System.out.println(postId);
        if (result.hasErrors()){
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("new-comment");
            modelAndView.addObject("currentUser", currentUser);
            return modelAndView;
        }

        commentService.addComment(postId,currentUser,createCommentRequest);

        ModelAndView modelAndView = new ModelAndView("redirect:/posts");
        modelAndView.addObject("currentUser", currentUser);

        return modelAndView;
    }
}
