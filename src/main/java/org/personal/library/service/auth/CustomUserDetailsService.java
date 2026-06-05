package org.personal.library.service.auth;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.UserRepository;
import org.personal.library.model.Permission;
import org.personal.library.model.Role;
import org.personal.library.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user from the database by their username to perform authentication and authorization.
     * This method maps the user's roles and permissions to Spring Security GrantedAuthorities,
     * and sets account status flags like 'disabled' (for pending users) or 'accountLocked' (for banned users).
     *
     * @param username the username of the user attempting to authenticate
     * @return a UserDetails object containing the user's credentials, authorities, and account status
     * @throws UsernameNotFoundException if no user with the given username exists in the database
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        Set<GrantedAuthority> authorities = new HashSet<>();
        
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName().name()));
            }
        }

        boolean isAccountLocked = user.getStatus() == User.UserStatus.BANNED 
                || (user.getBannedUntil() != null && user.getBannedUntil().isAfter(java.time.LocalDateTime.now()));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(user.getStatus() == User.UserStatus.PENDING)
                .accountLocked(isAccountLocked)
                .authorities(authorities)
                .build();
    }
}
