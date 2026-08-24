package com.aieoms.kafka.event;

public class IncidentEvent {

    private Long incidentId;
    private String title;
    private String severity;
    private String status;

    public IncidentEvent() {
    }

    public IncidentEvent(Long incidentId, String title, String severity, String status) {
        this.incidentId = incidentId;
        this.title = title;
        this.severity = severity;
        this.status = status;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}