package com.aieoms.user.service;

import com.aieoms.auth.dto.AuthResponse;
import com.aieoms.auth.dto.LoginResponse;
import com.aieoms.auth.dto.LoginRequest;
import com.aieoms.auth.dto.RegisterRequest;
import com.aieoms.security.JwtService;
import com.aieoms.security.LoginRateLimiter;
import com.aieoms.rbac.service.RbacService;
import com.aieoms.user.entity.User;
import com.aieoms.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log =
            LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RbacService rbacService;
    private final LoginRateLimiter loginRateLimiter;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RbacService rbacService,
            LoginRateLimiter loginRateLimiter
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rbacService = rbacService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setActive(true);

        User savedUser = userRepository.save(user);

        rbacService.assignDefaultUserRole(savedUser.getId());

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        long start = System.currentTimeMillis();

        loginRateLimiter.checkAllowed(request.username());

        User userOrNull = userRepository.findByUsername(request.username())
                .orElse(null);

        if (userOrNull == null
                || !passwordEncoder.matches(
                        request.password(),
                        userOrNull.getPasswordHash())) {

            loginRateLimiter.recordFailure(request.username());
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOrNull;

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("User account is inactive");
        }

        loginRateLimiter.recordSuccess(request.username());

        String token = jwtService.generateToken(user.getUsername());

        log.debug(
                "Login succeeded for user '{}' in {} ms",
                user.getUsername(),
                System.currentTimeMillis() - start
        );

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                token
        );
    }
}
