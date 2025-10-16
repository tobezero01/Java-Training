package com.ducnhu.payment.dto;

public record PaypalValidation(String status, Float amount, String currency, String referenceId, String captureId) {
}
