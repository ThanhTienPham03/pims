package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.dto.request.AdminResetPasswordRequest;
import saigonbank.fit.ims.dto.request.RegisterRequest;
import saigonbank.fit.ims.dto.request.UpdateUserRequest;
import saigonbank.fit.ims.dto.response.PageResponse;
import saigonbank.fit.ims.dto.response.UserResponse;
import saigonbank.fit.ims.entity.Role;
import saigonbank.fit.ims.entity.User;
import saigonbank.fit.ims.exception.ResourceNotFoundException;
import saigonbank.fit.ims.repository.RoleRepository;
import saigonbank.fit.ims.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(String search, Boolean isActive, Pageable pageable) {
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<User> userPage = userRepository.findWithFilters(searchParam, isActive, pageable);
        Page<UserResponse> dtoPage = userPage.map(this::mapToUserResponse);
        return PageResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại: " + request.getUsername());
        }

        Set<Role> roles = resolveRoles(request.getRoles());

        User user = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName() != null ? request.getFullName().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .isActive(true)
                .roles(roles)
                .build();

        userRepository.save(user);
        log.info("Admin created new user: {} with roles: {}", user.getUsername(), roles);
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = resolveRoles(request.getRoles());
            user.setRoles(roles);
        }

        userRepository.save(user);
        log.info("Admin updated user ID {}: fullName={}, roles={}", id, user.getFullName(), user.getRoles());
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        boolean newStatus = !Boolean.TRUE.equals(user.getIsActive());
        user.setIsActive(newStatus);
        userRepository.save(user);
        log.info("Admin toggled user ID {} status to: {}", id, newStatus ? "ACTIVE" : "INACTIVE");
        return mapToUserResponse(user);
    }

    @Transactional
    public void resetPassword(Long id, AdminResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Admin reset password for user ID {}: {}", id, user.getUsername());
    }

    private Set<Role> resolveRoles(List<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        if (roleNames == null || roleNames.isEmpty()) {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new ResourceNotFoundException("Role USER không tồn tại"));
            roles.add(userRole);
        } else {
            for (String roleName : roleNames) {
                String cleanName = roleName.replace("ROLE_", "").trim().toUpperCase();
                Role role = roleRepository.findByName(cleanName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại: " + cleanName));
                roles.add(role);
            }
        }
        return roles;
    }

    public UserResponse mapToUserResponse(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
