package cardindex.dojocardindex.web;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreateUserRequest;
import cardindex.dojocardindex.web.dto.UserEditAdminRequest;
import cardindex.dojocardindex.web.mapper.DTOMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @GetMapping("/add-user")
    public ModelAndView getAddNewUserPage(@AuthenticationPrincipal CustomUserDetails details){

        User user = userService.getUserById(details.getId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("addUser");
        modelAndView.addObject("user", user);
        modelAndView.addObject("createUserRequest", new CreateUserRequest());

        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PostMapping("/add-user")
    public ModelAndView addNewUser(@AuthenticationPrincipal CustomUserDetails details, @Valid CreateUserRequest createUserRequest, BindingResult result){

        User user = userService.getUserById(details.getId());

        if (result.hasErrors()){
            System.out.println("Грешки при валидация: " + result.getAllErrors());
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("addUser");
            modelAndView.addObject("user", user);
            return modelAndView;
        }

        userService.createNewUser(createUserRequest);

        ModelAndView modelAndView = new ModelAndView("redirect:/home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }


    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @GetMapping("/register-requests")
    public ModelAndView getRegisterRequests(@AuthenticationPrincipal CustomUserDetails details){

        User user = userService.getUserById(details.getId());

        List<User> registerRequests = userService.getRegisterRequests();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register-requests");
        modelAndView.addObject("user",user);
        modelAndView.addObject("registerRequests", registerRequests);

        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PostMapping("/register-requests/approve")
    public ModelAndView approveRegisterRequest(@RequestParam UUID id, @AuthenticationPrincipal CustomUserDetails details ) {

        User currentUser = userService.getUserById(details.getId());

        User request = userService.getUserById(id);

        userService.approveRequest(request.getId());

        ModelAndView modelAndView = new ModelAndView("redirect:/admin/register-requests");
        modelAndView.addObject("currentUser",currentUser);

        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PostMapping("/register-requests/deny")
    public ModelAndView denyRegisterRequest(@RequestParam UUID id, @AuthenticationPrincipal CustomUserDetails details ) {

        User currentUser = userService.getUserById(details.getId());

        User request = userService.getUserById(id);

        userService.denyRequest(request.getId());

        ModelAndView modelAndView = new ModelAndView("redirect:/admin/register-requests");
        modelAndView.addObject("currentUser",currentUser);

        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @GetMapping("/users/list/all-users")
    public ModelAndView getAllUsersPage(@AuthenticationPrincipal CustomUserDetails details) {

        User user = userService.getUserById(details.getId());

        List<User> allUsers = userService.getAllUsers();
        Map<UUID, Integer> userAges = userService.getUserAges(allUsers);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("all-users-list");
        modelAndView.addObject("user", user );
        modelAndView.addObject("userAges", userAges);
        modelAndView.addObject("allUsers", allUsers);

        return modelAndView;

    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PostMapping("/users/modify-status")
    public ModelAndView modifyUserStatus(@RequestParam UUID id, @AuthenticationPrincipal CustomUserDetails details, HttpServletRequest request){

        User currentUser = userService.getUserById(details.getId());

        userService.modifyAccStatus(id);

        String referer = request.getHeader("Referer");

        ModelAndView modelAndView = new ModelAndView("redirect:" + referer);
        modelAndView.addObject("currentUser", currentUser);

        return modelAndView;

    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @GetMapping("/users/details/edit/{id}")
    public ModelAndView getEditUserDetailsByAdminPage(@PathVariable UUID id,
                                                      @AuthenticationPrincipal CustomUserDetails details){

        User adminUser = userService.getUserById(details.getId());
        User user = userService.getUserById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-user-edit");
        modelAndView.addObject("user", user);
        modelAndView.addObject("userEditAdminRequest", DTOMapper.mapUserToUserEditAdminRequest(user));
        modelAndView.addObject("adminUser",adminUser);

        return modelAndView;

    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PostMapping("/users/details/edit/{id}")
    public ModelAndView editUserDetailsByAdmin(@PathVariable UUID id,
                                               @AuthenticationPrincipal CustomUserDetails details,
                                               @Valid UserEditAdminRequest userEditAdminRequest,
                                               BindingResult result){

        User adminUser = userService.getUserById(details.getId());

        if (result.hasErrors()) {

            User user = userService.getUserById(id);

            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("admin-user-edit");
            modelAndView.addObject("user", user);
            modelAndView.addObject("userEditAdminRequest", userEditAdminRequest);
            modelAndView.addObject("adminUser", adminUser);
            return modelAndView;
        }

        userService.editUserProfileByAdmin(id,userEditAdminRequest);

//        String referer = request.getHeader("Referer");

        ModelAndView modelAndView = new ModelAndView("redirect:/users/list");
        modelAndView.addObject(adminUser);

        return modelAndView;
    }

}
