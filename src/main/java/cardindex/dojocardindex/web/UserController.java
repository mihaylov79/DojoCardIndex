package cardindex.dojocardindex.web;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.ChangePasswordRequest;
import cardindex.dojocardindex.web.dto.CreateUserRequest;
import cardindex.dojocardindex.web.dto.EditUserProfileRequest;
import cardindex.dojocardindex.web.dto.UserEditAdminRequest;
import cardindex.dojocardindex.web.mapper.DTOMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PostMapping("/users/upload-profile-picture/{userId}")
    public ModelAndView uploadProfilePicture(
            @PathVariable UUID userId,
            @RequestParam("image")MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails details,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        userService.updateProfilePicture(userId, details.getId(), image);

        redirectAttributes.addFlashAttribute("successMessage",
                "Профилната снимка беше обновена успешно!");

        // Smart redirect - връща към страницата откъдето е дошъл
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            // Ако има referer, redirect-ваме там
            return new ModelAndView("redirect:" + referer.substring(referer.indexOf("/", 8)));
        } else {
            // Fallback: ако е собствен профил -> /home, иначе -> /users/details/{userId}
            if (userId.equals(details.getId())) {
                return new ModelAndView("redirect:/home");
            } else {
                return new ModelAndView("redirect:/users/details/" + userId);
            }
        }
    }

    @PostMapping("/users/remove-profile-picture/{userId}")
    public ModelAndView removeProfilePicture(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails details,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        userService.removeProfilePicture(userId, details.getId());

        redirectAttributes.addFlashAttribute("successMessage",
                "Профилната снимка беше премахната успешно!");

        // Smart redirect - връща към страницата откъдето е дошъл
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            // Ако има referer, redirect-ваме там
            return new ModelAndView("redirect:" + referer.substring(referer.indexOf("/", 8)));
        } else {
            // Fallback: ако е собствен профил -> /home, иначе -> /users/details/{userId}
            if (userId.equals(details.getId())) {
                return new ModelAndView("redirect:/home");
            } else {
                return new ModelAndView("redirect:/users/details/" + userId);
            }
        }
    }


// --- Преместени от AdminController методи, с абсолютни пътища /admin/... ---

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @GetMapping("/admin/add-user")
    public ModelAndView getAddNewUserPage(@AuthenticationPrincipal CustomUserDetails details){
        User user = userService.getUserById(details.getId());
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("addUser");
        modelAndView.addObject("user", user);
        modelAndView.addObject("createUserRequest", new CreateUserRequest());
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PostMapping("/admin/add-user")
    public ModelAndView addNewUser(@AuthenticationPrincipal CustomUserDetails details,
                                   @Valid CreateUserRequest createUserRequest,
                                   BindingResult result){
        User user = userService.getUserById(details.getId());
        if (result.hasErrors()){
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
    @GetMapping("/admin/register-requests")
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
    @PostMapping("/admin/register-requests/approve")
    public ModelAndView approveRegisterRequest(@RequestParam UUID id,
                                               @AuthenticationPrincipal CustomUserDetails details ) {
        User currentUser = userService.getUserById(details.getId());
        User request = userService.getUserById(id);
        userService.approveRequest(request.getId());
        ModelAndView modelAndView = new ModelAndView("redirect:/admin/register-requests");
        modelAndView.addObject("currentUser",currentUser);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PostMapping("/admin/register-requests/deny")
    public ModelAndView denyRegisterRequest(@RequestParam UUID id,
                                            @AuthenticationPrincipal CustomUserDetails details ) {
        User currentUser = userService.getUserById(details.getId());
        User request = userService.getUserById(id);
        userService.denyRequest(request.getId());
        ModelAndView modelAndView = new ModelAndView("redirect:/admin/register-requests");
        modelAndView.addObject("currentUser",currentUser);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @GetMapping("/admin/users/list/all-users")
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
    @PostMapping("/admin/users/modify-status")
    public ModelAndView modifyUserStatus(@RequestParam UUID id,
                                         @AuthenticationPrincipal CustomUserDetails details,
                                         HttpServletRequest request){
        User currentUser = userService.getUserById(details.getId());
        userService.modifyAccStatus(id);
        String referer = request.getHeader("Referer");
        ModelAndView modelAndView = new ModelAndView("redirect:" + referer);
        modelAndView.addObject("currentUser", currentUser);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @GetMapping("/admin/users/details/edit/{id}")
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
    @PostMapping("/admin/users/details/edit/{id}")
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
        ModelAndView modelAndView = new ModelAndView("redirect:/users/list");
        modelAndView.addObject(adminUser);
        return modelAndView;
    }
}
