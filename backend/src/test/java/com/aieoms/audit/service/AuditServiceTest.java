package com.aieoms.audit.service;

import com.aieoms.audit.entity.AuditLog;
import com.aieoms.audit.repository.AuditLogRepository;
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
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void record_shouldCreateAndSaveAuditLog() {

        AuditLog savedLog = new AuditLog();

        when(repository.save(any(AuditLog.class)))
                .thenReturn(savedLog);

        AuditLog result =
                auditService.record(
                        1L,
                        "INCIDENT_CREATED",
                        "INCIDENT",
                        "3",
                        "Created incident: Production API latency detected",
                        "127.0.0.1",
                        "JUnit-Test-Agent"
                );

        assertSame(savedLog, result);

        ArgumentCaptor<AuditLog> captor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(repository).save(captor.capture());

        AuditLog log = captor.getValue();

        assertEquals(1L, log.getUserId());
        assertEquals(
                "INCIDENT_CREATED",
                log.getAction()
        );
        assertEquals(
                "INCIDENT",
                log.getEntityType()
        );
        assertEquals("3", log.getEntityId());
        assertEquals(
                "Created incident: Production API latency detected",
                log.getDescription()
        );
        assertEquals(
                "127.0.0.1",
                log.getIpAddress()
        );
        assertEquals(
                "JUnit-Test-Agent",
                log.getUserAgent()
        );
    }

    @Test
    void findAll_shouldReturnAuditLogs() {

        AuditLog first = new AuditLog();
        AuditLog second = new AuditLog();

        when(repository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(first, second));

        List<AuditLog> result =
                auditService.findAll();

        assertEquals(2, result.size());
        assertSame(first, result.get(0));
        assertSame(second, result.get(1));

        verify(repository)
                .findAllByOrderByCreatedAtDesc();
    }

    @Test
    void findByUser_shouldReturnOnlyUserAuditLogs() {

        AuditLog log = new AuditLog();

        when(
                repository.findByUserIdOrderByCreatedAtDesc(1L)
        ).thenReturn(List.of(log));

        List<AuditLog> result =
                auditService.findByUser(1L);

        assertEquals(1, result.size());
        assertSame(log, result.get(0));

        verify(repository)
                .findByUserIdOrderByCreatedAtDesc(1L);
    }
}
