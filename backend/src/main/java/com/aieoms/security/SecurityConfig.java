package com.aieoms.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

        .requestMatchers(
        "/actuator/health",
        "/api/system/info",
        "/api/auth/register",
        "/api/auth/login"
).permitAll()

        .requestMatchers(
                "/api/admin/**"
        ).hasAuthority("ROLE_ADMIN")

        .requestMatchers(
                "/api/audit/**"
        ).hasAnyAuthority(
                "ROLE_ADMIN",
                "ROLE_OPERATOR"
        )

        .requestMatchers(
                "/api/incidents/all"
        ).hasAnyAuthority(
                "ROLE_ADMIN",
                "ROLE_OPERATOR"
        )

        .requestMatchers(
                "/api/incidents/*/assign"
        ).hasAnyAuthority(
                "ROLE_ADMIN",
                "ROLE_OPERATOR"
        )

        .requestMatchers(
                "/api/incidents/**"
        ).authenticated()

        .requestMatchers(
                "/api/users/me"
        ).authenticated()

        .anyRequest().authenticated()
)

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * Vercel assigns a new, randomly-hashed URL to every deployment
         * of this project (e.g. ai-eoms-j3yvska6y-giri6305s-projects.vercel.app),
         * in addition to a stable production domain. Rather than hardcoding
         * one exact URL and having to edit this file every time Vercel
         * generates a new one, we match any deployment under this specific
         * Vercel project using a pattern instead of an exact string.
         */
        configuration.setAllowedOriginPatterns(
                List.of(
                        "http://localhost:5173",
                        "https://ai-eoms.vercel.app",
                        "https://ai-eoms-*-giri6305s-projects.vercel.app"
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * This app authenticates entirely through JwtAuthenticationFilter +
     * UserRepository/UserRoleRepository — it never uses Spring Security's
     * own UserDetailsService. Without any UserDetailsService bean present,
     * Spring Boot's auto-configuration creates one in memory and logs
     * "Using generated security password: ...". Registering an empty,
     * unused one here suppresses that auto-configuration cleanly instead
     * of just ignoring the noisy log line.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}