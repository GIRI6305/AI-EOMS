package com.aieoms.incident.service;

import com.aieoms.incident.entity.Incident;
import com.aieoms.incident.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public Incident create(
            String title,
            String description,
            String severity,
            Long userId
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (severity == null || severity.isBlank()) {
            severity = "MEDIUM";
        }

        if (!severity.matches("LOW|MEDIUM|HIGH|CRITICAL")) {
            throw new IllegalArgumentException("Invalid severity");
        }

        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDescription(description);
        incident.setSeverity(severity);
        incident.setStatus("OPEN");
        incident.setCreatedBy(userId);

        return incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public List<Incident> findForUser(Long userId) {
        return incidentRepository.findByCreatedByOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Incident> findAll() {
        return incidentRepository.findAllByOrderByCreatedAtDesc();
    }
}
