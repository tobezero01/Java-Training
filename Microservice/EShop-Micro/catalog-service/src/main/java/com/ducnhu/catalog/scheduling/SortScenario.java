package com.ducnhu.catalog.scheduling;


import org.springframework.data.domain.Sort;

public record SortScenario(String fieldName, String direction, Sort sortLogic) {
}