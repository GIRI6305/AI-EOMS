package com.aieoms.user;

import com.aieoms.user.entity.User;
import com.aieoms.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    private int rolePriority(String role) {
        return switch (role) {
            case "ROLE_ADMIN" -> 3;
            case "ROLE_OPERATOR" -> 2;
            case "ROLE_USER" -> 1;
            default -> 0;
        };
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(
            Authentication authentication
    ) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        String role = authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority ->
                        authority.equals("ROLE_ADMIN")
                                || authority.equals("ROLE_OPERATOR")
                                || authority.equals("ROLE_USER")
                )
                .sorted((a, b) -> Integer.compare(
                        rolePriority(b),
                        rolePriority(a)
                ))
                .findFirst()
                .orElse("ROLE_USER");

        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "active", user.getActive(),
                "role", role,
                "createdAt", user.getCreatedAt(),
                "updatedAt", user.getUpdatedAt()
        );
    }
}
