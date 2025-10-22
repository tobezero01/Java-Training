package com.ducnhu.common.events.catalog;

import java.util.List;

public record ProductSnapshotRequest(String correlationId, String replyTo, List<Integer> productIds) {
}

