package com.ducnhu.payment.dto;

import java.util.Map;

public record PaypalCaptureResult(String captureId, String status, Map<String, Object> raw) {
}

