package cardindex.dojocardindex.web;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.ChangePasswordRequest;
import cardindex.dojocardindex.web.dto.EditUserProfileRequest;
import cardindex.dojocardindex.web.mapper.DTOMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping("/users/details/edit/{id}")
    public ModelAndView getUserEditPage(@PathVariable UUID id) {

            User user = userService.getUserById(id);


            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("EditUserProfileUser");
            modelAndView.addObject("user", user);
            modelAndView.addObject("editUserProfileRequest", DTOMapper.mapUserToEditUserRequest(user));
            return modelAndView;

    }


    @PutMapping("/users/details/edit/{id}")
    public ModelAndView editProfileDetails(@PathVariable UUID id, @Valid EditUserProfileRequest editUserProfileRequest, BindingResult result){

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
        modelAndView.setViewName("user-details-test");
        modelAndView.addObject("user", user);
        modelAndView.addObject("currentUser", currentUser);

        return modelAndView;

    }

    @GetMapping("/users/details/edit/password")
    public ModelAndView getPassWordChangePage(@AuthenticationPrincipal CustomUserDetails details) {

        User user = userService.getUserById(details.getId());

        ModelAndView modelAndView = new ModelAndView("password-change");
        modelAndView.addObject(user);
        modelAndView.addObject("changePasswordRequest", new ChangePasswordRequest());

        return modelAndView;
    }


    @PutMapping("/users/details/edit/password")
    public ModelAndView changeUserPassword(@AuthenticationPrincipal CustomUserDetails details,
                                           @Valid ChangePasswordRequest changePasswordRequest,
                                           BindingResult result) {

        User user = userService.getUserById(details.getId());

        if (result.hasErrors()){
            ModelAndView modelAndView = new ModelAndView("password-change");
            modelAndView.addObject(user);

            return modelAndView;
        }

        userService.changePassword(changePasswordRequest,user.getEmail());
        ModelAndView modelAndView = new ModelAndView("redirect:/home");
        modelAndView.addObject(user);

        return modelAndView;
    }
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @GetMapping("/users/list/active")
    public ModelAndView getActiveUsersListPag(@AuthenticationPrincipal CustomUserDetails details){

        User currentuser = userService.getUserById(details.getId());
         List<User>active = userService.getActiveUsersList();
         Map<UUID,Integer> userAges = userService.getUserAges(active);

        ModelAndView modelAndView = new ModelAndView("active-users");
        modelAndView.addObject("currentUser",currentuser);
        modelAndView.addObject("active",active);
        modelAndView.addObject("userAges",userAges);

        return modelAndView;
    }

}
