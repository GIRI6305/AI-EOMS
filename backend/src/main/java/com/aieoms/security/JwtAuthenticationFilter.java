
package com.aieoms.security;

import com.aieoms.rbac.repository.UserRoleRepository;
import com.aieoms.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(7).trim();

        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username =
                    jwtService.extractUsername(token);

            if (username == null || username.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder
                    .getContext()
                    .getAuthentication() != null) {

                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.isTokenValid(token, username)) {
                log.debug(
                        "Rejected invalid JWT for user '{}'",
                        username
                );

                filterChain.doFilter(request, response);
                return;
            }

            var userOptional =
                    userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                log.debug(
                        "JWT user '{}' does not exist",
                        username
                );

                filterChain.doFilter(request, response);
                return;
            }

            var user = userOptional.get();

            if (!Boolean.TRUE.equals(user.getActive())) {
                log.debug(
                        "Inactive user '{}' attempted authentication",
                        username
                );

                filterChain.doFilter(request, response);
                return;
            }

            /*
             * Load ALL roles assigned to this user directly from
             * user_roles + roles through the repository query.
             *
             * Example:
             * ROLE_USER
             * ROLE_OPERATOR
             */
            List<String> roleNames =
                    userRoleRepository
                            .findRoleNamesByUserId(user.getId())
                            .stream()
                            .filter(roleName ->
                                    roleName != null
                                            && !roleName.isBlank())
                            .toList();

            List<SimpleGrantedAuthority> authorities =
                    roleNames.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

            log.info(
        "RBAC DATABASE ROLES FOR USER {} = {}",
        user.getId(),
        roleNames
);

            log.info(
                    "JWT AUTH USER={} USER_ID={} AUTHORITIES={}",
                    user.getUsername(),
                    user.getId(),
                    authorities
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            log.debug(
                    "Authenticated user '{}' with authorities {}",
                    username,
                    authorities
            );

        } catch (Exception exception) {

            /*
             * Never expose JWT validation details to the client.
             * The request simply remains unauthenticated.
             */
            SecurityContextHolder.clearContext();

            log.debug(
                    "JWT authentication failed for request {} {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getMessage()
            );
        }

        filterChain.doFilter(request, response);
    }
}
