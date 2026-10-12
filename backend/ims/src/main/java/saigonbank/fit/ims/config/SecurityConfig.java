package saigonbank.fit.ims.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import saigonbank.fit.ims.config.security.JwtAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

// Cấu hình bảo mật JWT stateless, CORS và phân quyền theo Role
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        // Bỏ qua lỗi unauthorized cho các tài nguyên Camunda webapp
                        String uri = request.getRequestURI();
                        if (uri.startsWith("/camunda") || uri.startsWith("/app") || uri.startsWith("/lib") || uri.startsWith("/api/engine")) {
                            response.sendRedirect("/camunda/app/welcome/default/");
                            return;
                        }
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write(
                            "{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"Bạn chưa đăng nhập\"}"
                        );
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write(
                            "{\"status\":403,\"code\":\"ACCESS_DENIED\",\"message\":\"Bạn không có quyền thực hiện thao tác này\"}"
                        );
                    })
            )
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/ws/**").permitAll()
                    .requestMatchers(
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/swagger-resources/**",
                            "/webjars/**"
                    ).permitAll()
                    // Camunda 7 Webapp & REST Engine
                    .requestMatchers(
                            "/camunda/**",
                            "/app/**",
                            "/lib/**",
                            "/api/engine/**",
                            "/api/camunda/**"
                    ).permitAll()
                    .requestMatchers("/api/auth/login", "/api/auth/verify-otp", "/api/auth/logout").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                    .requestMatchers("/api/auth/register").hasRole("ADMIN")
                    .requestMatchers("/api/users/**").hasRole("ADMIN")
                    .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")
                    .requestMatchers("/api/monthly-closings/lock").hasRole("CONTROLLER")
                    .requestMatchers("/api/monthly-closings/unlock").hasRole("CONTROLLER")
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated origins từ config
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(s -> s.trim())
                .filter(StringUtils::hasText)
                .toList();
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
