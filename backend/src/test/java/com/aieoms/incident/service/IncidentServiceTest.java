package com.aieoms.incident.service;

import com.aieoms.incident.entity.Incident;
import com.aieoms.incident.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void create_shouldCreateOpenIncidentWithProvidedValues() {

        Incident savedIncident = new Incident();

        when(incidentRepository.save(any(Incident.class)))
                .thenReturn(savedIncident);

        Incident result =
                incidentService.create(
                        "Production API latency detected",
                        "API latency increased significantly",
                        "HIGH",
                        1L
                );

        assertSame(savedIncident, result);

        ArgumentCaptor<Incident> captor =
                ArgumentCaptor.forClass(Incident.class);

        verify(incidentRepository)
                .save(captor.capture());

        Incident incident = captor.getValue();

        assertEquals(
                "Production API latency detected",
                incident.getTitle()
        );

        assertEquals(
                "API latency increased significantly",
                incident.getDescription()
        );

        assertEquals("HIGH", incident.getSeverity());
        assertEquals("OPEN", incident.getStatus());
        assertEquals(1L, incident.getCreatedBy());
    }

    @Test
    void create_shouldDefaultSeverityToMedium() {

        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        Incident result =
                incidentService.create(
                        "Test incident",
                        "Test description",
                        null,
                        1L
                );

        assertEquals("MEDIUM", result.getSeverity());
        assertEquals("OPEN", result.getStatus());
    }

    @Test
    void create_shouldRejectBlankTitle() {

        assertThrows(
                IllegalArgumentException.class,
                () -> incidentService.create(
                        " ",
                        "Description",
                        "HIGH",
                        1L
                )
        );

        verify(
                incidentRepository,
                never()
        ).save(any());
    }

    @Test
    void create_shouldRejectInvalidSeverity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> incidentService.create(
                        "Test incident",
                        "Description",
                        "URGENT",
                        1L
                )
        );

        verify(
                incidentRepository,
                never()
        ).save(any());
    }

    @Test
    void findForUser_shouldReturnUserIncidents() {

        Incident incident = new Incident();

        when(
                incidentRepository
                        .findByCreatedByOrderByCreatedAtDesc(1L)
        ).thenReturn(List.of(incident));

        List<Incident> result =
                incidentService.findForUser(1L);

        assertEquals(1, result.size());
        assertSame(incident, result.get(0));

        verify(
                incidentRepository
        ).findByCreatedByOrderByCreatedAtDesc(1L);
    }

    @Test
    void findAll_shouldReturnAllIncidents() {

        Incident first = new Incident();
        Incident second = new Incident();

        when(
                incidentRepository
                        .findAllByOrderByCreatedAtDesc()
        ).thenReturn(List.of(first, second));

        List<Incident> result =
                incidentService.findAll();

        assertEquals(2, result.size());
        assertSame(first, result.get(0));
        assertSame(second, result.get(1));

        verify(
                incidentRepository
        ).findAllByOrderByCreatedAtDesc();
    }
}
