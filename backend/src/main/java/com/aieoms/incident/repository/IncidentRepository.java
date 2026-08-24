package com.aieoms.incident.repository;

import com.aieoms.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    List<Incident> findAllByOrderByCreatedAtDesc();
}
