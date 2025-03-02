package cardindex.dojocardindex.web;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.EditUserProfileRequest;
import cardindex.dojocardindex.web.mapper.DTOMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user/details/edit/{id}")
    public ModelAndView getUserEditPage(@PathVariable UUID id) {

            User user = userService.getUserById(id);


            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("EditUserProfileUser");
            modelAndView.addObject("user", user);
            modelAndView.addObject("editUserProfileRequest", DTOMapper.mapUserToEditUserRequest(user));
            return modelAndView;

    }


    @PostMapping("/user/details/edit/{id}")
    public ModelAndView editProfileDetails(@PathVariable UUID id, EditUserProfileRequest editUserProfileRequest,BindingResult result){

        if (result.hasErrors()){
            User user = userService.getUserById(id);

            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("EditUserProfileUser");
            modelAndView.addObject("user", user);
            modelAndView.addObject("editUserProfileRequest", editUserProfileRequest);
            return modelAndView;
        }

        userService.editUserProfile(id,editUserProfileRequest);

        return new ModelAndView("redirect:/home");
    }

//    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
//    @GetMapping("/admin-panel/add-user")
//    public ModelAndView getAddNewUserPage(@AuthenticationPrincipal CustomUserDetails details){
//
//        User user = userService.getUserById(details.getId());
//
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("addUser");
//        modelAndView.addObject("user", user);
//        modelAndView.addObject("createUserRequest", new CreateUserRequest());
//
//        return modelAndView;
//    }

//    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
//    @PostMapping("/admin/add-user")
//    public ModelAndView addNewUser(@AuthenticationPrincipal CustomUserDetails details, @Valid CreateUserRequest createUserRequest, BindingResult result){
//
//        User user = userService.getUserById(details.getId());
//
//        if (result.hasErrors()){
//            ModelAndView modelAndView = new ModelAndView();
//            modelAndView.setViewName("addUser");
//            modelAndView.addObject("user", user);
//            return modelAndView;
//        }
//
//        userService.createNewUser(createUserRequest);
//
//        ModelAndView modelAndView = new ModelAndView("redirect:/home");
//        modelAndView.addObject("user", user);
//
//        return modelAndView;
//    }

    @GetMapping("/users/list")
    public ModelAndView getActiveUsersPage(@AuthenticationPrincipal CustomUserDetails details){

        User user = userService.getUserById(details.getId());

        List<User> allActiveUsers = userService.getAllActiveUsers();

        Map<UUID, Integer> userAges = userService.getUserAges(allActiveUsers);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("users-list");
        modelAndView.addObject("user", user);
        modelAndView.addObject("allActiveUsers", allActiveUsers);
        modelAndView.addObject("userAges",userAges);

        return modelAndView;


    }

    @GetMapping("/users/details/{id}")
    public ModelAndView getUserDetailsPage(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());
        User user = userService.getUserById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("user-details");
        modelAndView.addObject("user", user);
        modelAndView.addObject("currentUser", currentUser);

        return modelAndView;

    }

}
