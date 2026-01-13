package com.eshop.common.kafka.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseEvent {
    
    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String source; // Service that produced the event
    private Integer version; // Event schema version for backward compatibility
    
    protected BaseEvent(String source) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.timestamp = Instant.now();
        this.source = source;
        this.version = 1;
    }
}
