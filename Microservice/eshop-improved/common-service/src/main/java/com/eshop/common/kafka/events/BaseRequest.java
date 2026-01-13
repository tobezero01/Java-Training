package com.eshop.common.kafka.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base class for request messages in Request-Reply pattern
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseRequest {
    private String correlationId;
    private String replyTopic; // Optional: where to send response
}
