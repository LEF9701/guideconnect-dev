package com.guideconnect.config;

import com.guideconnect.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration implementing session-based authentication
 * with role-based access control (FR-UA-02, FR-UA-03, NFR-SE-01, NFR-SE-02).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
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
                .requestMatchers("/", "/tours/search", "/tours/{id}", "/auth/**",
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
                .failureUrl("/auth/login?error=true")
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

        http.csrf(csrf -> csrf.disable());
        http.headers(headers -> headers.frameOptions(f -> f.disable()));
        return http.build();
    }
}
