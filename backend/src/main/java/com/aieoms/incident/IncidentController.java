package com.aieoms.incident;

import com.aieoms.ai.service.AiAnalysisService;
import com.aieoms.audit.service.AuditLogService;
import com.aieoms.incident.entity.Incident;
import com.aieoms.incident.repository.IncidentRepository;
import com.aieoms.kafka.event.IncidentEvent;
import com.aieoms.kafka.producer.IncidentEventProducer;
import com.aieoms.user.entity.User;
import com.aieoms.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private static final Logger log =
            LoggerFactory.getLogger(IncidentController.class);

    private static final String DEFAULT_SEVERITY = "MEDIUM";
    private static final String DEFAULT_STATUS = "OPEN";

    private static final String SEVERITY_PATTERN =
            "LOW|MEDIUM|HIGH|CRITICAL";

    private static final String STATUS_PATTERN =
            "OPEN|IN_PROGRESS|RESOLVED|CLOSED";

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AiAnalysisService aiAnalysisService;

    /*
     * ObjectProvider is intentional.
     *
     * Kafka can be disabled in Render using:
     *
     * KAFKA_ENABLED=false
     *
     * Therefore IncidentEventProducer may not exist in the
     * Spring application context.
     *
     * ObjectProvider allows the application to work with or
     * without Kafka.
     */
    private final ObjectProvider<IncidentEventProducer>
            incidentEventProducerProvider;

    public IncidentController(
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            AuditLogService auditLogService,
            AiAnalysisService aiAnalysisService,
            ObjectProvider<IncidentEventProducer>
                    incidentEventProducerProvider
    ) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.aiAnalysisService = aiAnalysisService;
        this.incidentEventProducerProvider =
                incidentEventProducerProvider;
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    private User currentUser(
            Authentication authentication
    ) {

        if (authentication == null ||
                authentication.getName() == null ||
                authentication.getName().isBlank()) {

            throw new IllegalStateException(
                    "User authentication is required"
            );
        }

        return userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        )
                );
    }

    // =========================================================
    // FIND INCIDENT
    // =========================================================

    private Incident getIncident(Long id) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Invalid incident ID"
            );
        }

        return incidentRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Incident not found"
                        )
                );
    }

    // =========================================================
    // ROLE CHECKS
    // =========================================================

    private boolean hasRole(
            Authentication authentication,
            String role
    ) {

        if (authentication == null ||
                authentication.getAuthorities() == null) {

            return false;
        }

        return authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private boolean isAdmin(
            Authentication authentication
    ) {

        return hasRole(
                authentication,
                "ROLE_ADMIN"
        );
    }

    private boolean isOperator(
            Authentication authentication
    ) {

        return hasRole(
                authentication,
                "ROLE_OPERATOR"
        );
    }

    private boolean isAdminOrOperator(
            Authentication authentication
    ) {

        return isAdmin(authentication)
                || isOperator(authentication);
    }

    // =========================================================
    // INCIDENT ACCESS
    // =========================================================

    /*
     * ADMIN / OPERATOR:
     *     Can access any incident.
     *
     * USER:
     *     Can access only incidents created by themselves.
     */
    private void verifyIncidentAccess(
            Incident incident,
            User user,
            Authentication authentication
    ) {

        if (isAdminOrOperator(authentication)) {
            return;
        }

        if (incident.getCreatedBy() == null ||
                user.getId() == null ||
                !incident.getCreatedBy()
                        .equals(user.getId())) {

            throw new SecurityException(
                    "You do not have permission to access this incident"
            );
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private boolean isValidSeverity(
            String severity
    ) {

        return severity != null &&
                severity.matches(SEVERITY_PATTERN);
    }

    private boolean isValidStatus(
            String status
    ) {

        return status != null &&
                status.matches(STATUS_PATTERN);
    }

    private String clean(String value) {

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    // =========================================================
    // HTTP INFORMATION
    // =========================================================

    private String ip(
            HttpServletRequest request
    ) {

        if (request == null) {
            return null;
        }

        return request.getRemoteAddr();
    }

    private String userAgent(
            HttpServletRequest request
    ) {

        if (request == null) {
            return null;
        }

        return request.getHeader("User-Agent");
    }

    // =========================================================
    // KAFKA EVENT
    // =========================================================

    private void publishIncidentEvent(
            Incident incident
    ) {

        /*
         * If Kafka is disabled, there will be no
         * IncidentEventProducer bean.
         *
         * getIfAvailable() safely returns null.
         */
        IncidentEventProducer producer =
                incidentEventProducerProvider
                        .getIfAvailable();

        if (producer == null) {

            log.debug("Kafka is disabled. Incident event was not published.");

            return;
        }

        try {

            IncidentEvent event =
                    new IncidentEvent(
                            incident.getId(),
                            incident.getTitle(),
                            incident.getSeverity(),
                            incident.getStatus()
                    );

            producer.publish(event);

        } catch (Exception exception) {

            /*
             * Kafka failure must not roll back an incident
             * that has already been successfully stored.
             */
            log.warn(
                    "Kafka incident event publishing failed: {}",
                    exception.getMessage()
            );
        }
    }

    // =========================================================
    // AUDIT HELPER
    // =========================================================

    private void writeAudit(
            Long userId,
            String action,
            String entityId,
            String description,
            HttpServletRequest request
    ) {

        try {

            auditLogService.log(
                    userId,
                    action,
                    "INCIDENT",
                    entityId,
                    description,
                    ip(request),
                    userAgent(request)
            );

        } catch (Exception exception) {

            /*
             * Audit failure should not make the main incident
             * operation fail.
             */
            log.warn(
                    "Audit logging failed: {}",
                    exception.getMessage()
            );
        }
    }

    // =========================================================
    // CREATE INCIDENT
    // =========================================================

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody(required = false)
            Map<String, String> request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        User user = currentUser(authentication);

        if (request == null) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "Request body is required"
                    ));
        }

        String title =
                clean(request.get("title"));

        if (title == null ||
                title.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "title is required"
                    ));
        }

        String severity =
                clean(
                        request.getOrDefault(
                                "severity",
                                DEFAULT_SEVERITY
                        )
                );

        if (!isValidSeverity(severity)) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "Severity must be LOW, MEDIUM, HIGH or CRITICAL"
                    ));
        }

        String description =
                clean(request.get("description"));

        Incident incident = new Incident();

        incident.setTitle(title);
        incident.setDescription(description);
        incident.setSeverity(severity);
        incident.setStatus(DEFAULT_STATUS);
        incident.setCreatedBy(user.getId());

        Incident saved =
                incidentRepository.save(incident);

        // Publish Kafka event if Kafka is enabled.
        publishIncidentEvent(saved);

        // Write audit log.
        writeAudit(
                user.getId(),
                "INCIDENT_CREATED",
                saved.getId().toString(),
                "Created incident: "
                        + saved.getTitle(),
                httpRequest
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saved);
    }

    // =========================================================
    // GET ALL INCIDENTS
    // =========================================================

    @GetMapping("/all")
    public ResponseEntity<?> all(
            Authentication authentication
    ) {

        if (!isAdminOrOperator(authentication)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "Only ADMIN or OPERATOR can view all incidents"
                    ));
        }

        return ResponseEntity.ok(
                incidentRepository
                        .findAllByOrderByCreatedAtDesc()
        );
    }

    // =========================================================
    // GET MY INCIDENTS
    // =========================================================

    @GetMapping
    public ResponseEntity<List<Incident>> mine(
            Authentication authentication
    ) {

        User user =
                currentUser(authentication);

        return ResponseEntity.ok(
                incidentRepository
                        .findByCreatedByOrderByCreatedAtDesc(
                                user.getId()
                        )
        );
    }

    // =========================================================
    // GET INCIDENT BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User user =
                currentUser(authentication);

        Incident incident =
                getIncident(id);

        verifyIncidentAccess(
                incident,
                user,
                authentication
        );

        return ResponseEntity.ok(incident);
    }

    // =========================================================
    // AI ANALYSIS
    // =========================================================

    @PostMapping("/{id}/ai-analysis")
    public ResponseEntity<?> aiAnalysis(
            @PathVariable Long id,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        User user =
                currentUser(authentication);

        Incident incident =
                getIncident(id);

        verifyIncidentAccess(
                incident,
                user,
                authentication
        );

        try {

            String analysis =
                    aiAnalysisService.analyzeIncident(
                            incident.getTitle(),
                            incident.getDescription(),
                            incident.getSeverity()
                    );

            writeAudit(
                    user.getId(),
                    "AI_INCIDENT_ANALYSIS",
                    incident.getId().toString(),
                    "AI analysis generated for incident: "
                            + incident.getTitle(),
                    httpRequest
            );

            return ResponseEntity.ok(
                    Map.of(
                            "incidentId",
                            incident.getId(),

                            "title",
                            incident.getTitle(),

                            "analysis",
                            analysis == null
                                    ? ""
                                    : analysis
                    )
            );

        } catch (Exception exception) {

            System.err.println(
                    "AI analysis failed: "
                            + exception.getMessage()
            );

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            Map.of(
                                    "error",
                                    "AI analysis failed",

                                    "message",
                                    "Unable to generate AI analysis at this time."
                            )
                    );
        }
    }

    // =========================================================
    // UPDATE INCIDENT
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,

            @RequestBody(required = false)
            Map<String, String> request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        User user =
                currentUser(authentication);

        Incident incident =
                getIncident(id);

        verifyIncidentAccess(
                incident,
                user,
                authentication
        );

        if (request == null) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "Request body is required"
                    ));
        }

        // TITLE
        if (request.containsKey("title")) {

            String title =
                    clean(request.get("title"));

            if (title == null ||
                    title.isBlank()) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "error",
                                "title cannot be blank"
                        ));
            }

            incident.setTitle(title);
        }

        // DESCRIPTION
        if (request.containsKey("description")) {

            incident.setDescription(
                    clean(
                            request.get("description")
                    )
            );
        }

        // SEVERITY
        if (request.containsKey("severity")) {

            String severity =
                    clean(
                            request.get("severity")
                    );

            if (!isValidSeverity(severity)) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "error",
                                "Severity must be LOW, MEDIUM, HIGH or CRITICAL"
                        ));
            }

            incident.setSeverity(severity);
        }

        // STATUS
        if (request.containsKey("status")) {

            String status =
                    clean(
                            request.get("status")
                    );

            if (!isValidStatus(status)) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "error",
                                "Status must be OPEN, IN_PROGRESS, RESOLVED or CLOSED"
                        ));
            }

            incident.setStatus(status);
        }

        Incident saved =
                incidentRepository.save(incident);

        writeAudit(
                user.getId(),
                "INCIDENT_UPDATED",
                saved.getId().toString(),
                "Updated incident: "
                        + saved.getTitle(),
                httpRequest
        );

        return ResponseEntity.ok(saved);
    }

    // =========================================================
    // DELETE INCIDENT
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        User user =
                currentUser(authentication);

        Incident incident =
                getIncident(id);

        verifyIncidentAccess(
                incident,
                user,
                authentication
        );

        String title =
                incident.getTitle();

        incidentRepository.delete(incident);

        writeAudit(
                user.getId(),
                "INCIDENT_DELETED",
                id.toString(),
                "Deleted incident: "
                        + title,
                httpRequest
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Incident deleted",

                        "id",
                        id
                )
        );
    }

    // =========================================================
    // ASSIGN INCIDENT
    // =========================================================

    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assign(
            @PathVariable Long id,

            @RequestBody(required = false)
            Map<String, Object> request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        User user =
                currentUser(authentication);

        if (!isAdminOrOperator(authentication)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "Only ADMIN or OPERATOR can assign incidents"
                    ));
        }

        Incident incident =
                getIncident(id);

        if (request == null) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "Request body is required"
                    ));
        }

        Object assignedTo =
                request.get("assignedTo");

        if (assignedTo == null) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "assignedTo is required"
                    ));
        }

        Long assignedUserId;

        try {

            assignedUserId =
                    Long.valueOf(
                            assignedTo.toString()
                    );

        } catch (NumberFormatException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "assignedTo must be a valid user ID"
                    ));
        }

        if (assignedUserId <= 0) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "assignedTo must be a valid positive user ID"
                    ));
        }

        User assignedUser =
                userRepository
                        .findById(assignedUserId)
                        .orElse(null);

        if (assignedUser == null) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "Assigned user not found"
                    ));
        }

        if (!Boolean.TRUE.equals(
                assignedUser.getActive()
        )) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "Cannot assign incident to an inactive user"
                    ));
        }

        incident.setAssignedTo(
                assignedUserId
        );

        Incident saved =
                incidentRepository.save(incident);

        writeAudit(
                user.getId(),
                "INCIDENT_ASSIGNED",
                saved.getId().toString(),
                "Assigned incident to user "
                        + assignedUserId,
                httpRequest
        );

        return ResponseEntity.ok(saved);
    }

    // =========================================================
    // CHANGE STATUS
    // =========================================================

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> status(
            @PathVariable Long id,

            @RequestBody(required = false)
            Map<String, String> request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        User user =
                currentUser(authentication);

        Incident incident =
                getIncident(id);

        verifyIncidentAccess(
                incident,
                user,
                authentication
        );

        if (request == null) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "Request body is required"
                    ));
        }

        String status =
                clean(
                        request.get("status")
                );

        if (!isValidStatus(status)) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "Status must be OPEN, IN_PROGRESS, RESOLVED or CLOSED"
                    ));
        }

        incident.setStatus(status);

        Incident saved =
                incidentRepository.save(incident);

        writeAudit(
                user.getId(),
                "INCIDENT_STATUS_CHANGED",
                saved.getId().toString(),
                "Incident status changed to "
                        + status,
                httpRequest
        );

        return ResponseEntity.ok(saved);
    }

    // =========================================================
    // CHANGE SEVERITY
    // =========================================================

    @PatchMapping("/{id}/severity")
    public ResponseEntity<?> severity(
            @PathVariable Long id,

            @RequestBody(required = false)
            Map<String, String> request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        User user =
                currentUser(authentication);

        Incident incident =
                getIncident(id);

        verifyIncidentAccess(
                incident,
                user,
                authentication
        );

        if (request == null) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "Request body is required"
                    ));
        }

        String severity =
                clean(
                        request.get("severity")
                );

        if (!isValidSeverity(severity)) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            "Severity must be LOW, MEDIUM, HIGH or CRITICAL"
                    ));
        }

        incident.setSeverity(severity);

        Incident saved =
                incidentRepository.save(incident);

        writeAudit(
                user.getId(),
                "INCIDENT_SEVERITY_CHANGED",
                saved.getId().toString(),
                "Incident severity changed to "
                        + severity,
                httpRequest
        );

        return ResponseEntity.ok(saved);
    }
}