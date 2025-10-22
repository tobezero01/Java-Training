package com.ducnhu.payment.dto;

public record Summary(float productTotal, float shipping, float paymentTotal, boolean codSupported) {}

