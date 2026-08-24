package com.aieoms.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
public class AiAnalysisService {

    /*
     * Spring AI 2.0's application.yml timeout properties
     * (spring.ai.openai.chat.options.*-timeout) are not honoured by the
     * underlying HTTP client (known upstream issue). Without an explicit
     * bound, a slow/unresponsive Groq endpoint blocks the calling thread
     * indefinitely, which is why "Generate AI Analysis" was hanging for
     * several minutes instead of failing fast.
     *
     * We enforce the timeout ourselves at the call site instead.
     */
    private static final long AI_TIMEOUT_SECONDS = 25;

    private static final ExecutorService AI_EXECUTOR =
            Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "ai-analysis-worker");
                thread.setDaemon(true);
                return thread;
            });

    private final ChatClient chatClient;

    public AiAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String analyzeIncident(
            String title,
            String description,
            String severity
    ) {

        String safeTitle =
                title == null || title.isBlank()
                        ? "Unknown incident"
                        : title;

        String safeDescription =
                description == null || description.isBlank()
                        ? "No description provided"
                        : description;

        String safeSeverity =
                severity == null || severity.isBlank()
                        ? "Unknown"
                        : severity;

        String prompt = """
                You are an AI Enterprise Operations assistant.

                Analyze the following enterprise incident.

                Title: %s
                Description: %s
                Severity: %s

                Return a concise operational analysis containing:

                1. Root cause possibilities
                2. Business impact
                3. Immediate action
                4. Recommended severity
                5. Prevention recommendation

                Rules:
                - Be practical and concise.
                - Do not invent facts.
                - Clearly distinguish possibilities from confirmed facts.
                - Do not assume information that is not provided.
                - Keep the response concise.
                """.formatted(
                safeTitle,
                safeDescription,
                safeSeverity
        );

        Future<String> future = AI_EXECUTOR.submit(() ->
                chatClient
                        .prompt()
                        .user(prompt)
                        .call()
                        .content()
        );

        String result;

        try {
            result = future.get(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException timeoutException) {
            future.cancel(true);
            throw new IllegalStateException(
                    "AI analysis timed out after "
                            + AI_TIMEOUT_SECONDS
                            + " seconds. The AI provider (Groq) did not "
                            + "respond in time. Please try again."
            );
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "AI analysis was interrupted.",
                    interruptedException
            );
        } catch (ExecutionException executionException) {
            String causeMessage =
                    executionException.getCause() != null
                            ? executionException.getCause().getMessage()
                            : executionException.getMessage();

            throw new IllegalStateException(
                    "AI analysis failed: " + causeMessage,
                    executionException.getCause()
            );
        }

        if (result == null || result.isBlank()) {
            throw new IllegalStateException(
                    "AI returned an empty response."
            );
        }

        return result.trim();
    }
}
