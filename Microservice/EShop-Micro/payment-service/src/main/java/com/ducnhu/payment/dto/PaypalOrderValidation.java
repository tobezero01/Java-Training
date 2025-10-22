package com.ducnhu.payment.dto;

public record PaypalOrderValidation(
        String paypalOrderId,
        String intent,              // CAPTURE | AUTHORIZE
        String status,              // CREATED | APPROVED | COMPLETED | PAYER_ACTION_REQUIRED ...
        String currency,            // USD ...
        Float amount,               // purchase_units[0].amount.value
        String referenceId,         // purchase_units[0].reference_id
        String captureId,           // nếu đã capture thì có id đầu tiên
        String payerEmail,
        String payerId,
        boolean valid,              // tổng hợp các tiêu chí dưới
        boolean statusOk,           // = APPROVED hoặc COMPLETED (tuỳ requireCompleted)
        boolean amountOk,           // trùng số tiền (±0.01)
        boolean currencyOk,         // trùng currency (ignore-case)
        boolean referenceOk,        // trùng referenceId (tuỳ bạn có kiểm hay không)
        String reason               // nếu invalid -> mô tả vì sao
){}
