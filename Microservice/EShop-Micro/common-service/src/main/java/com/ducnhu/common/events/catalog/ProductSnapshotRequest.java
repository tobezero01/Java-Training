package com.ducnhu.common.events.catalog;

public record ProductSnapshotRequest(String correlationId, String replyTo, java.util.List<Integer> productIds) {
}

