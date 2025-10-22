package com.ducnhu.payment.dto;

import java.util.Map;

public record PaypalCreateResult(String orderId, String approvalUrl, Map<String, Object> raw){}

