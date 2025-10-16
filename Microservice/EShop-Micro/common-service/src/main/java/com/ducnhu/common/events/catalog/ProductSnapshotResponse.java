package com.ducnhu.common.events.catalog;

import java.util.List;

public record ProductSnapshotResponse(String correlationId, List<ProductSnapshot> products){}

