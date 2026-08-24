package com.aieoms.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    private AiAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);

        aiAnalysisService =
                new AiAnalysisService(chatClientBuilder);
    }

    @Test
    void analyzeIncident_shouldReturnAiAnalysis() {

        String expected =
                "Root cause: database connectivity issue.";

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(expected);

        String result =
                aiAnalysisService.analyzeIncident(
                        "Database outage",
                        "Production database is unavailable",
                        "CRITICAL"
                );

        assertEquals(expected, result);

        verify(chatClient).prompt();
        verify(requestSpec).user(anyString());
        verify(requestSpec).call();
        verify(responseSpec).content();
    }

    @Test
    void analyzeIncident_shouldRejectEmptyAiResponse() {

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("");

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> aiAnalysisService.analyzeIncident(
                                "Database outage",
                                "Production database is unavailable",
                                "HIGH"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("empty response")
        );
    }

    @Test
    void analyzeIncident_shouldHandleAiClientFailure() {

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call())
                .thenThrow(new RuntimeException("Connection refused"));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> aiAnalysisService.analyzeIncident(
                                "API outage",
                                "API requests are failing",
                                "HIGH"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("AI analysis failed")
        );

        assertNotNull(exception.getCause());
        assertEquals(
                "Connection refused",
                exception.getCause().getMessage()
        );
    }
}
