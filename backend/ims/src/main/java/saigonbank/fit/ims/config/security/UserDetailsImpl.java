package saigonbank.fit.ims.config.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import saigonbank.fit.ims.entity.User;

import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails implementation wrapping the User entity.
 */
@Getter
public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final boolean active;  // phản ánh User.isActive
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String username, String password,
                           String fullName, boolean active,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id          = id;
        this.username    = username;
        this.password    = password;
        this.fullName    = fullName;
        this.active      = active;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getFullName(),
                Boolean.TRUE.equals(user.getIsActive()),  // chuyển sang primitive boolean
                authorities
        );
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    /**
     * Trả về trạng thái active của User entity.
     * UserDetailsServiceImpl đã kiểm tra isActive trước khi build,
     * nhưng isEnabled() cần phản ánh đúng để Spring Security có thể dùng.
     */
    @Override public boolean isEnabled() { return this.active; }
}
