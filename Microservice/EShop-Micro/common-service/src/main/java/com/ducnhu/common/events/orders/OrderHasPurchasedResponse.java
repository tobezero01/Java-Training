package com.ducnhu.common.events.orders;

public record OrderHasPurchasedResponse(String correlationId, Integer customerId,
                                        Integer productId, boolean purchased) {
}
