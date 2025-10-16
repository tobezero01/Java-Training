package com.ducnhu.common.events.settings;

public record EmailSettingsResponse(
        String correlationId,
        // server
        String host, Integer port, String username, String password,
        String smtpAuth, String smtpSecured,
        // from
        String mailFrom, String senderName,
        // templates
        String orderConfirmSubject, String orderConfirmContent,
        String customerVerifySubject, String customerVerifyContent
) {
}
