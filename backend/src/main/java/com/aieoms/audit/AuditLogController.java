package com.aieoms.audit;

import com.aieoms.audit.entity.AuditLog;
import com.aieoms.audit.repository.AuditLogRepository;
import com.aieoms.user.entity.User;
import com.aieoms.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogController(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> myLogs(
            Authentication authentication
    ) {
        User user = currentUser(authentication);

        return ResponseEntity.ok(
                auditLogRepository.findByUserIdOrderByCreatedAtDesc(
                        user.getId()
                )
        );
    }

    @GetMapping("/all")
    public ResponseEntity<List<AuditLog>> allLogs() {
        return ResponseEntity.ok(
                auditLogRepository.findAllByOrderByCreatedAtDesc()
        );
    }
}
