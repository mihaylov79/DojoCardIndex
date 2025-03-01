package cardindex.dojocardindex.config;

import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.UserRole;
import cardindex.dojocardindex.User.models.UserStatus;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.User.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import cardindex.dojocardindex.User.models.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SecurityConfig(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                                .requestMatchers("/", "/register").permitAll()  // Разрешаваме достъп до /, /login и /register
                                .anyRequest().authenticated() // всички останали пътища изискват аутентикация
                )
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/home",true)
                        .failureUrl("/login?error")// Тук задаваш свой URL за логин страницата
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                        .logoutSuccessUrl("/") // URL за излизане
                );

        return http.build();
    }


    @Bean
    public CommandLineRunner initializeAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {

                User admin = User.builder()
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(UserRole.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .registrationStatus(RegistrationStatus.REGISTERED)
                        .firstName("Admin")
                        .lastName("User")
                        .isCompetitor(false)
                        .build();
                userRepository.save(admin);
            }


        };
    }

}