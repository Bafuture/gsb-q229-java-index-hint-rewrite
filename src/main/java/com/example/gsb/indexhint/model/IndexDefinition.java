package com.example.gsb.indexhint.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record IndexDefinition(String name, List<String> columns,
                              Map<String, Double> equalitySelectivity,
                              double rangeSelectivity) {

    public static final double DEFAULT_EQUALITY_SELECTIVITY = 0.1d;
    public static final double DEFAULT_RANGE_SELECTIVITY = 0.25d;

    public IndexDefinition {
        columns = List.copyOf(columns);
        equalitySelectivity = Map.copyOf(equalitySelectivity);
    }

    public static IndexDefinition of(String name, String... columns) {
        return new IndexDefinition(name, List.of(columns), Map.of(), DEFAULT_RANGE_SELECTIVITY);
    }

    public static IndexDefinition of(String name, List<String> columns) {
        return new IndexDefinition(name, columns, Map.of(), DEFAULT_RANGE_SELECTIVITY);
    }

    public IndexDefinition withSelectivity(String column, double ratio) {
        Map<String, Double> copy = new HashMap<>(equalitySelectivity);
        copy.put(column, ratio);
        return new IndexDefinition(name, columns, copy, rangeSelectivity);
    }

    public double selectivityOf(String column) {
        return equalitySelectivity.getOrDefault(column, DEFAULT_EQUALITY_SELECTIVITY);
    }
}
