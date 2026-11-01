package org.teza.backend.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService editor(@Value("${teza.editor.username}") String username,
                             @Value("${teza.editor.password}") String password,
                             PasswordEncoder encoder) {
        if (username.isBlank() || username.length() > 255 || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Configure editor username (1–255 characters) and password (at least 12 characters, up to 72 UTF-8 bytes)");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(encoder.encode(password)).roles("EDITOR").build());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/api/v1/auth/csrf", "/login", "/error").permitAll()
                        .anyRequest().hasRole("EDITOR"))
                .csrf(Customizer.withDefaults())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/v1/auth/login")
                        .successHandler((request, response, authentication) -> {
                            if (request.getHeader("Accept") != null && request.getHeader("Accept").contains("text/html")) {
                                response.sendRedirect(request.getContextPath() + "/swagger-ui/index.html");
                            } else {
                                response.setStatus(204);
                            }
                        })
                        .failureHandler((request, response, exception) -> problem(response, mapper, HttpStatus.UNAUTHORIZED, "Invalid credentials"))
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor((request, response, exception) ->
                                problem(response, mapper, HttpStatus.UNAUTHORIZED, "Authentication required"),
                                new AntPathRequestMatcher("/api/**"))
                        .accessDeniedHandler((request, response, exception) ->
                                problem(response, mapper, HttpStatus.FORBIDDEN, "Access denied or CSRF token missing or invalid")))
                .build();
    }

    private static void problem(HttpServletResponse response, ObjectMapper mapper,
                                HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
