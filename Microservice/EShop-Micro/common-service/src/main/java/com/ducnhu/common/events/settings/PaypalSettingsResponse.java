package com.ducnhu.common.events.settings;

public record PaypalSettingsResponse(
        String correlationId, String baseUrl, String clientId, String clientSecret
) {
}
