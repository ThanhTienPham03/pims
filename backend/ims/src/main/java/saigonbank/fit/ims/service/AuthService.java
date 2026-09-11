package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.config.JwtTokenProvider;
import saigonbank.fit.ims.config.security.UserDetailsImpl;
import saigonbank.fit.ims.dto.request.LoginRequest;
import saigonbank.fit.ims.dto.request.RegisterRequest;
import saigonbank.fit.ims.dto.response.InitiateLoginResponse;
import saigonbank.fit.ims.dto.response.JwtResponse;
import saigonbank.fit.ims.dto.response.UserResponse;
import saigonbank.fit.ims.entity.Role;
import saigonbank.fit.ims.entity.User;
import saigonbank.fit.ims.exception.ResourceNotFoundException;
import saigonbank.fit.ims.repository.RoleRepository;
import saigonbank.fit.ims.repository.UserRepository;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Xác thực 2 bước: kiểm tra credentials -> gửi OTP qua email -> xác thực OTP & cấp JWT (3h)
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final OtpStore otpStore;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public InitiateLoginResponse initiateLogin(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findByEmail(email)
                .or(() -> userRepository.findByUsername(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + email));

        String userEmail = user.getEmail();
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalStateException(
                    "Tài khoản chưa có email. Vui lòng liên hệ quản trị viên để cập nhật.");
        }

        String otpCode = generateOtp();
        otpStore.save(userEmail.toLowerCase(), otpCode);
        log.info("OTP generated for email '{}' (username: {}): {} (email: {})", userEmail, user.getUsername(), otpCode, maskEmail(userEmail));

        String recipientName = user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getUsername();
        emailService.sendOtpEmail(userEmail, recipientName, otpCode);

        return InitiateLoginResponse.builder()
                .maskedEmail(maskEmail(userEmail))
                .message("Mã OTP 6 số đã được gửi đến email của bạn. Mã có hiệu lực trong 5 phút.")
                .build();
    }

    public JwtResponse verifyOtp(String email, String otpCode) {
        String cleanEmail = email.trim().toLowerCase();
        if (!otpStore.verify(cleanEmail, otpCode)) {
            throw new BadCredentialsException(
                    "Mã OTP không đúng hoặc đã hết hạn. Vui lòng thử lại.");
        }

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + cleanEmail));

        String jwt = jwtTokenProvider.generateTokenForUser(user);

        log.info("User '{}' (email: '{}') completed 2FA login. JWT issued (3h expiry).", user.getUsername(), cleanEmail);
        auditLogService.logAction(user.getUsername(), "LOGIN", "USER", user.getUsername(), "Người dùng đăng nhập hệ thống qua email (xác thực OTP thành công)");

        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
                .toList();

        return JwtResponse.builder()
                .token(jwt)
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(roleNames)
                .build();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại: " + request.getUsername());
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new ResourceNotFoundException("Role USER không tồn tại"));
            roles.add(userRole);
        } else {
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại: " + roleName));
                roles.add(role);
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .roles(roles)
                .build();

        userRepository.save(user);
        return mapToUserResponse(user);
    }

    private String generateOtp() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    // Che email hiển thị (ví dụ: ng*****@gmail.com)
    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 2) return email;
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        String visible = local.substring(0, Math.min(2, local.length()));
        return visible + "*".repeat(Math.max(3, local.length() - 2)) + domain;
    }

    private UserResponse mapToUserResponse(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
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
