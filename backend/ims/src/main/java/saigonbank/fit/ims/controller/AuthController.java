package saigonbank.fit.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import saigonbank.fit.ims.config.security.UserDetailsImpl;
import saigonbank.fit.ims.dto.request.LoginRequest;
import saigonbank.fit.ims.dto.request.RegisterRequest;
import saigonbank.fit.ims.dto.request.VerifyOtpRequest;
import saigonbank.fit.ims.dto.response.ApiResponse;
import saigonbank.fit.ims.dto.response.InitiateLoginResponse;
import saigonbank.fit.ims.dto.response.JwtResponse;
import saigonbank.fit.ims.dto.response.UserResponse;
import saigonbank.fit.ims.service.AuthService;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Bước 1: Xác thực credentials & gửi OTP
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<InitiateLoginResponse>> initiateLogin(
            @Valid @RequestBody LoginRequest request) {
        InitiateLoginResponse response = authService.initiateLogin(request);
        return ResponseEntity.ok(ApiResponse.success("OTP đã được gửi đến email của bạn", response));
    }

    // Bước 2: Xác thực OTP & cấp JWT
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<JwtResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        JwtResponse response = authService.verifyOtp(request.getEmail(), request.getOtpCode());
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo tài khoản thành công", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<JwtResponse>> getCurrentUser(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .toList();

        JwtResponse response = JwtResponse.builder()
                .id(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .fullName(userPrincipal.getFullName())
                .roles(roles)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
