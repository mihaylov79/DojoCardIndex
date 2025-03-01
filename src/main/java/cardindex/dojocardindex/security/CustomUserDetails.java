package cardindex.dojocardindex.security;

import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.UserRole;
import cardindex.dojocardindex.User.models.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.autoconfigure.task.TaskSchedulingProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@Getter
@AllArgsConstructor

public class CustomUserDetails implements UserDetails {

    private UUID id;
    private String email;
    private String password;
    private UserRole role;
    private RegistrationStatus registrationStatus;
    private UserStatus userStatus;



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());
        return List.of(authority);
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isEnabled() {

        return registrationStatus == RegistrationStatus.REGISTERED && userStatus == UserStatus.ACTIVE;
    }
}
