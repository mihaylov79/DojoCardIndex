package cardindex.dojocardindex.User.service;


import cardindex.dojocardindex.User.models.*;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.Utils.PasswordGenerator;
import cardindex.dojocardindex.exceptions.*;
import cardindex.dojocardindex.imageUpload.ImageUploadService;
import cardindex.dojocardindex.notification.service.NotificationService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.*;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Builder(toBuilder = true)
@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final ImageUploadService imageUploadService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, NotificationService notificationService, ImageUploadService imageUploadService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.imageUploadService = imageUploadService;
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
                .profilePicture(null)  // Снимката се добавя отделно чрез upload endpoint
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
                .birthDate(editUserProfileRequest.getBirthDate())
                .interests(editUserProfileRequest.getInterests())
                .height(editUserProfileRequest.getHeight())
                .weight(editUserProfileRequest.getWeight())
                .contactPerson(editUserProfileRequest.getContactPerson())
                .contactPersonPhone(editUserProfileRequest.getContactPersonPhone())
                .build();

        userRepository.save(user);
    }

    public void updateProfilePicture(UUID targetUserId, UUID currentUserId, MultipartFile image) {

        if (image == null || image.isEmpty()){
            throw new IllegalArgumentException("Подайте валидно изображение");
        }

        // 🔒 SECURITY CHECK: Потребителят може да променя САМО своята снимка
        // освен ако не е ADMIN или TRAINER
        User currentUser = getUserById(currentUserId);

        if (!targetUserId.equals(currentUserId) &&
            currentUser.getRole() != UserRole.ADMIN &&
            currentUser.getRole() != UserRole.TRAINER) {

            log.warn("Потребител {} се опита да промени снимката на потребител {} без права!",
                     currentUserId, targetUserId);
            throw new AccessDeniedException("Нямате права да променяте снимката на този потребител!");
        }

        log.info("Обновяване снимката на потребител {} от потребител {}", targetUserId, currentUserId);

        User user = getUserById(targetUserId);
        String oldProfilePicture = user.getProfilePicture();

        // Опитваме да изтрием старата снимка (ако има)
        if (oldProfilePicture != null && !oldProfilePicture.isEmpty()) {
            try {
                imageUploadService.deleteImage(oldProfilePicture);
                log.info("Старата снимка е изтрита: {}", oldProfilePicture);
            } catch (Exception e) {
                // Не позволяваме грешка при изтриване да спре upload-а на новата снимка
                log.warn("Не успяхме да изтрием старата снимка ({}): {}",
                         oldProfilePicture, e.getMessage());
            }
        }
        log.info("Започваме качване на новата снимка...");
        String newImageUrl = imageUploadService.uploadImage(image);
        log.info("Качена е нова снимка: {}", newImageUrl);

        try {
            user = user.toBuilder()
                    .profilePicture(newImageUrl)
                    .build();

            userRepository.save(user);
            log.info("Профилната снимка на потребител : [{}] беше обновена успешно.", targetUserId);
        } catch (Exception e) {
            // Ако save-ът fail-не, изтриваме новата снимка от Cloudinary
            log.error("Грешка при запазване в базата данни. Изтриваме новата снимка от Cloudinary...", e);
            try {
                imageUploadService.deleteImage(newImageUrl);
                log.info("Новата снимка беше изтрита от Cloudinary след DB грешка");
            } catch (Exception cleanupError) {
                log.error("Не успяхме да изтрием снимката от Cloudinary: {}", cleanupError.getMessage());
            }
            throw new RuntimeException("Грешка при обновяване на профилната снимка", e);
        }
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

    public boolean checkMedicalExamExpiration(User user){

        if (user.getMedicalExamsPassed() == null){
            return true;
        }

        LocalDate expiationDate = user.getMedicalExamsPassed().plusYears(1).minusMonths(1);

        return !LocalDate.now().isBefore(expiationDate);
    }

    public List<User> medicalExamRenewalUsersList(){

        return getAllActiveUsers().stream().filter(this::checkMedicalExamExpiration).sorted(Comparator
                                                            .comparing(User::getMedicalExamsPassed,
                                                                    Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public Map<UUID,Long> daysLeftToListNextExam(List<User> users){

        return users.stream().distinct()
                .collect(Collectors.toMap(User::getId,user -> daysLeftToUserNextExam(user.getMedicalExamsPassed())));
    }

    public Long daysLeftToUserNextExam(LocalDate lastMedicalExam){

        if (lastMedicalExam == null){
            return 0L;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), lastMedicalExam.plusYears(1));
    }

//    @Scheduled(fixedDelay = 60000)
    @Scheduled(cron = "0 0 0 * * MON ")
    public void notifyMedicalExamExpiration(){

        List<User>notificationList = getAllActiveUsers().stream()
                                                        .filter(user-> user.getMedicalExamsPassed() != null)
                                                        .filter(this::checkMedicalExamExpiration)
                                                        .filter(user -> LocalDate.now().isBefore(user.getMedicalExamsPassed().plusYears(1)))
                                                        .toList();

        notificationList.forEach(user -> {
            String content = "Наближава време за подновяване на медицинския Ви преглед! Желателно е това да се случи до %s. Моля уведомете треньорите за този мейл."
                                                                        .formatted(user.getMedicalExamsPassed().plusYears(1)
                                                                        .format(DateTimeFormatter.ofPattern("dd-MM-yyy ' г.'")));
            try {
                notificationService.sendNotification(user.getId(), user.getFirstName(), user.getLastName(), "Предстоящи медицински прегледи!",content);
            } catch (Exception e) {
                log.error("{} не беше предупреден за предстоящ медицински прегред поради неуспешно изпращане на мейл-а",user.getEmail(),e);
            }

        });
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
