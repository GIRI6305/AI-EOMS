package com.aieoms.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/info")
    public Map<String, Object> systemInfo() {

        return Map.of(
                "application", "AI-EOMS",
                "description",
                "AI-Powered Enterprise Operations & Incident Management System",
                "version", "1.0.0",
                "status", "RUNNING",
                "timestamp", Instant.now()
        );
    }
}
