package com.ducnhu.common.events.carts;

import java.util.List;

public record CartGetResponse(String correlationId, Integer customerId, List<CartLine> items) {
}

