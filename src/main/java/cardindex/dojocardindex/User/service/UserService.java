package cardindex.dojocardindex.User.service;


import cardindex.dojocardindex.User.models.Degree;
import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserStatus;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.Utils.PasswordGenerator;
import cardindex.dojocardindex.exceptions.EmailAlreadyInUseException;
import cardindex.dojocardindex.exceptions.UserAlreadyExistException;
import cardindex.dojocardindex.exceptions.UserNotFoundException;
import cardindex.dojocardindex.exceptions.WrongPasswordException;
import cardindex.dojocardindex.notification.service.NotificationService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.*;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder(toBuilder = true)
@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    public void register(RegisterRequest registerRequest){
        User user = userRepository.findByEmail(registerRequest.getEmail())
                .orElseThrow(UserNotFoundException::new);

        if (user.getStatus() == UserStatus.INACTIVE){
            throw new UserAlreadyExistException("Не можете да регистрирате деактивиран потребител!");
        }

        if (user.getRegistrationStatus() == RegistrationStatus.REGISTERED) {
            throw new UserAlreadyExistException();
        }

        if (user.getRegistrationStatus() == RegistrationStatus.PENDING){
            throw new UserAlreadyExistException("Този потребител е регистриран , но изчаква потвърждение от Администратор.");
        }

        if(user.getRegistrationStatus() == RegistrationStatus.NOT_REGISTERED) {
            user = user.toBuilder()
                    .password(passwordEncoder.encode(registerRequest.getPassword()))
                    .firstName(registerRequest.getFirstName())
                    .lastName(registerRequest.getLastName())
                    .reachedDegree(Degree.NONE)
                    .registrationStatus(RegistrationStatus.PENDING).build();

         userRepository.save(user);

//         String emailBody = "Вашата заявка за регистрация беше получена и изпратена за потвърждение към Администратор.Ще получите известие когато заявката Ви бъде обработена.";
//         notificationService.sendNotification(user.getId(), user.getFirstName(), user.getLastName(), "Заявка за регистрация",emailBody);

        }

    }

    public void createNewUser(CreateUserRequest createUserRequest){

        Optional<User>userByEmail = userRepository.findByEmail(createUserRequest.getEmail());

        if (userByEmail.isPresent()){
            throw new EmailAlreadyInUseException();
        }

        String generatedPassword = PasswordGenerator.generateRandomPassword(12);

        User user = User.builder()
                .email(createUserRequest.getEmail())
                .password(passwordEncoder.encode(generatedPassword))
                .firstName(createUserRequest.getFirstName())
                .lastName(createUserRequest.getLastName())
                .userPhone(createUserRequest.getUserPhone())
                .profilePicture(createUserRequest.getProfilePicture())
                .birthDate(createUserRequest.getBirthDate())
                .reachedDegree(createUserRequest.getReachedDegree()== null ? Degree.NONE : createUserRequest.getReachedDegree() )
                .ageGroup(createUserRequest.getAgeGroup())
                .isCompetitor(createUserRequest.getIsCompetitor())
                .height(createUserRequest.getHeight())
                .weight(createUserRequest.getWeight())
                .contactPerson(createUserRequest.getContactPerson())
                .role(createUserRequest.getRole())
                .status(UserStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.NOT_REGISTERED).build();

        userRepository.save(user);
    }

    public void editUserProfile(UUID userId, EditUserProfileRequest editUserProfileRequest){

        User user = getUserById(userId);

        user = user.toBuilder()
                .firstName(editUserProfileRequest.getFirstName())
                .lastName(editUserProfileRequest.getLastName())
                .userPhone(editUserProfileRequest.getUserPhone())
                .profilePicture(editUserProfileRequest.getProfilePicture())
                .birthDate(editUserProfileRequest.getBirthDate())
                .interests(editUserProfileRequest.getInterests())
                .height(editUserProfileRequest.getHeight())
                .weight(editUserProfileRequest.getWeight())
                .contactPerson(editUserProfileRequest.getContactPerson())
                .contactPersonPhone(editUserProfileRequest.getContactPersonPhone())
                .build();

        userRepository.save(user);
    }

    public void editUserProfileByAdmin(UUID userId, UserEditAdminRequest userEditAdminRequest) {

        User user = getUserById(userId);

        user = user.toBuilder()
                .userPhone(userEditAdminRequest.getUserPhone())
                .birthDate(userEditAdminRequest.getBirthDate())
                .role(userEditAdminRequest.getRole())
                .isCompetitor(userEditAdminRequest.getIsCompetitor())
                .status(userEditAdminRequest.getStatus())
                .registrationStatus(userEditAdminRequest.getRegistrationStatus())
                .reachedDegree(userEditAdminRequest.getReachedDegree())
                .ageGroup(userEditAdminRequest.getAgeGroup())
                .height(userEditAdminRequest.getHeight())
                .weight(userEditAdminRequest.getWeight())
                .medicalExamsPassed(userEditAdminRequest.getMedicalExamsPassed())
                .contactPerson(userEditAdminRequest.getContactPerson())
                .contactPersonPhone(userEditAdminRequest.getContactPersonPhone())
                .build();

        userRepository.save(user);
    }

    public User findUserByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(()-> new UserNotFoundException("Потребител с електронна поща %s не съществува".formatted(email)));
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Потребителят не е намерен в базата данни!"));
    }

    public User getUserById(UUID userId){
       return userRepository.findById(userId)
               .orElseThrow(()-> new UserNotFoundException("Потребител с идентификация: [%s] не е намерен."
                       .formatted(userId)));
    }

    public void modifyAccStatus(UUID userId){

        User user = getUserById(userId);

        if (user.getStatus()== UserStatus.ACTIVE){

            user = user.toBuilder()
                    .status(UserStatus.INACTIVE)
                    .build();
        } else if (user.getStatus()== UserStatus.INACTIVE){

            user = user.toBuilder()
                    .status(UserStatus.ACTIVE)
                    .build();
        }

        userRepository.save(user);
    }

    public void approveRequest(UUID userId){

        User user = getUserById(userId);

        user = user.toBuilder()
                .registrationStatus(RegistrationStatus.REGISTERED)
                .build();

        userRepository.save(user);

        try {
            // заявка към mail-svc за задаване настройки за мейл известията (default = true)
            notificationService.saveNotificationPreference(userId, true, user.getEmail());

            // изпращаме известие на потребителя за успешна регистрация
            String emailContent = "Вашата заявка за регистрация беше потвърдена. Вече можете да влезете в профила си.";
            notificationService.sendNotification(userId, user.getFirstName(), user.getLastName(), "Одобрена заявка за регистрация", emailContent);

        } catch (Exception e) {
            // логваме грешката, ако не успеем да изпратим мейл или да запишем настройките
            log.error("Грешка при изпращане на мейл известие или запазване на настройките за мейл известията за потребител с ID: {}", userId, e);

        }
    }

    public void denyRequest(UUID userId){

        User user = getUserById(userId);

        user = user.toBuilder()
                .registrationStatus(RegistrationStatus.NOT_REGISTERED)
                .build();

        userRepository.save(user);


    }

    public void changePassword(ChangePasswordRequest request, String email){

        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();

        User user = getCurrentUser();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())){
            throw new WrongPasswordException();
        }

        if (oldPassword.equals(newPassword)){
            throw new WrongPasswordException("Новата парола трябва да е различна от настоящата!");
        }

        user = user.toBuilder()
                .password(passwordEncoder.encode(newPassword))
                .build();

        userRepository.save(user);

        try{
//            notificationService.checkNotificationPreference(user.getId(), user.getEmail());

            String emailContent = "Паролата на Вашия потребителски профил беще успешно променена.";
            notificationService.sendNotification(user.getId(),user.getFirstName(), user.getLastName(),"Сменена парола", emailContent);
        }catch (Exception e) {

            log.error("Грешка при изпращане на за променена парола за потребител с ID: {}", user.getId(), e);
        }


    }

    public List<User> getAllUsers(){
        return userRepository.findAll(Sort.by(
                                        Sort.Order.asc("status"),
                                        Sort.Order.desc("registrationStatus"),
                                        Sort.Order.asc("firstName"),
                                        Sort.Order.asc("lastName")));
    }

    public List<User> getActiveUsersList(){
        return  userRepository.findAllByStatus(UserStatus.ACTIVE,
                                                Sort.by("firstName")
                                                .and(Sort.by("lastName")
                                                .and(Sort.by("status"))));
    }

    public List<User> getAllActiveUsers() {

        return userRepository.findByStatusAndRegistrationStatus(UserStatus.ACTIVE,RegistrationStatus.REGISTERED);
    }

    public List<User> getRegisterRequests() {

        return userRepository.findByRegistrationStatus(RegistrationStatus.PENDING);
    }

    public List<User>getRecipients(UUID senderId){

        return userRepository.findAllByIdIsNotAndRegistrationStatusAndStatus(senderId,RegistrationStatus.REGISTERED,UserStatus.ACTIVE);
    }


    public Map<UUID, Integer> getUserAges(List<User> users) {
        return users.stream().distinct()
                .collect(Collectors.toMap(User::getId, user -> calculateAge(user.getBirthDate())));
    }

    public int calculateAge(LocalDate birthdate) {
        return birthdate != null ? Period.between(birthdate, LocalDate.now()).getYears() : 0;
    }

    public void saveUser(User user) {

        userRepository.save(user);
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

            User user = userRepository.findByEmail(username).orElseThrow(() -> new UserNotFoundException("Потребител с мейл: %s не е намерен!".formatted(username)));

        return new CustomUserDetails(user.getId(), user.getEmail(),user.getPassword(),user.getRole(),user.getRegistrationStatus(),user.getStatus());
    }


}
