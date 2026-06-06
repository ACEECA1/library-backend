package org.personal.library.config;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.PermissionRepository;
import org.personal.library.dao.RoleRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.model.Permission;
import org.personal.library.model.Role;
import org.personal.library.model.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import org.personal.library.model.PermissionType;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissionsAndRoles();
        if (userRepository.count() == 0) {
            seedAdminUser();
        }
    }

    private void seedPermissionsAndRoles() {
        Set<Permission> allPermissions = new HashSet<>();
        for (PermissionType pName : PermissionType.values()) {
            Permission permission = permissionRepository.findByName(pName).orElseGet(() -> {
                Permission p = new Permission();
                p.setName(pName);
                return permissionRepository.save(p);
            });
            allPermissions.add(permission);
        }

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setName("ADMIN");
            return r;
        });
        adminRole.setPermissions(allPermissions);
        roleRepository.save(adminRole);

        Role moderatorRole = roleRepository.findByName("MODERATOR").orElseGet(() -> {
            Role r = new Role();
            r.setName("MODERATOR");
            return r;
        });
        
        Set<Permission> modPermissions = new HashSet<>();
        for (Permission p : allPermissions) {
            if (p.getName() == PermissionType.MODERATE_COMMENT || 
                p.getName() == PermissionType.BAN_USER || 
                p.getName() == PermissionType.APPROVE_USER) {
                modPermissions.add(p);
            }
        }
        moderatorRole.setPermissions(modPermissions);
        roleRepository.save(moderatorRole);
    }

    private void seedAdminUser() {
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setStatus(User.UserStatus.ACTIVE);
        
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);

        userRepository.save(admin);
    }
}
