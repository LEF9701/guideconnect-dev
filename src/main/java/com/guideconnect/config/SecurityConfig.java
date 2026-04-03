package com.guideconnect.config;

import com.guideconnect.model.AccountStatus;
import com.guideconnect.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security configuration implementing session-based authentication
 * with role-based access control (FR-UA-02, FR-UA-03, NFR-SE-01, NFR-SE-02).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/tours/search", "/tours/{id}", "/guides/**", "/tourists/**", "/auth/**",
                    "/css/**", "/js/**", "/images/**", "/error/**", "/h2-console/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/guide/**").hasRole("GUIDE")
                .requestMatchers("/tourist/**").hasRole("TOURIST")
                .requestMatchers("/bookings/**", "/messages/**", "/reviews/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler((request, response, authentication) -> {
                    var authorities = authentication.getAuthorities().toString();
                    if (authorities.contains("ROLE_ADMIN")) {
                        response.sendRedirect("/admin/dashboard");
                    } else if (authorities.contains("ROLE_GUIDE")) {
                        response.sendRedirect("/guide/dashboard");
                    } else {
                        response.sendRedirect("/tourist/dashboard");
                    }
                })
                .failureHandler((request, response, exception) -> {
                    String redirectUrl = "/auth/login?error=true";
                    String attemptedEmail = request.getParameter("username");

                    if (attemptedEmail != null) {
                        var user = userService.findByEmail(attemptedEmail).orElse(null);
                        if (user != null) {
                            if (user.getStatus() == AccountStatus.SUSPENDED) {
                                redirectUrl = "/auth/login?status=suspended";
                            } else if (user.getStatus() == AccountStatus.BANNED) {
                                redirectUrl = "/auth/login?status=banned";
                            }
                        }
                    }

                    response.sendRedirect(redirectUrl);
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            );

        http.addFilterAfter(accountStatusEnforcementFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http.csrf(csrf -> csrf.disable());
        http.headers(headers -> headers.frameOptions(f -> f.disable()));
        return http.build();
    }

    @Bean
    public OncePerRequestFilter accountStatusEnforcementFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();

                if (authentication != null
                        && authentication.isAuthenticated()
                        && authentication.getPrincipal() instanceof UserDetails userDetails) {
                    userService.findByEmail(userDetails.getUsername()).ifPresent(user -> {
                        if (user.getStatus() != AccountStatus.ACTIVE) {
                            try {
                                new SecurityContextLogoutHandler().logout(request, response, authentication);
                                response.sendRedirect("/auth/login?status=" + user.getStatus().name().toLowerCase());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

                    if (response.isCommitted()) {
                        return;
                    }
                }

                filterChain.doFilter(request, response);
            }
        };
    }
}
