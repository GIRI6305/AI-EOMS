package com.aieoms.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "application", "AI-EOMS",
                "status", "RUNNING",
                "message", "AI Enterprise Operations Management System",
                "frontend", "http://localhost:5173",
                "api", "http://localhost:8080/api/system/info",
                "health", "http://localhost:8080/actuator/health"
        );
    }
}
