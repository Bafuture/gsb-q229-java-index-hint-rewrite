package com.example.gsb.indexhint.model;

import java.util.List;
import java.util.stream.Collectors;

public record Comparison(String field, Operator operator, Object value) implements Condition {

    public static Comparison eq(String field, Object value) {
        return new Comparison(field, Operator.EQ, value);
    }

    public static Comparison ne(String field, Object value) {
        return new Comparison(field, Operator.NE, value);
    }

    public static Comparison gt(String field, Object value) {
        return new Comparison(field, Operator.GT, value);
    }

    public static Comparison ge(String field, Object value) {
        return new Comparison(field, Operator.GE, value);
    }

    public static Comparison lt(String field, Object value) {
        return new Comparison(field, Operator.LT, value);
    }

    public static Comparison le(String field, Object value) {
        return new Comparison(field, Operator.LE, value);
    }

    public boolean isEquality() {
        return operator == Operator.EQ || operator == Operator.IN;
    }

    public boolean isRange() {
        return operator == Operator.GT || operator == Operator.GE
                || operator == Operator.LT || operator == Operator.LE
                || operator == Operator.BETWEEN || operator == Operator.LIKE;
    }

    public boolean isIndexUsable() {
        return isEquality() || isRange();
    }

    @Override
    public String toString() {
        if (operator == Operator.IN && value instanceof List<?> list) {
            return field + " IN (" + list.stream()
                    .map(Comparison::formatValue)
                    .collect(Collectors.joining(", ")) + ")";
        }
        if (operator == Operator.BETWEEN && value instanceof List<?> list && list.size() == 2) {
            return field + " BETWEEN " + formatValue(list.get(0)) + " AND " + formatValue(list.get(1));
        }
        return field + " " + operator.symbol() + " " + formatValue(value);
    }

    static String formatValue(Object v) {
        if (v instanceof String s) {
            return "'" + s + "'";
        }
        return String.valueOf(v);
    }
}
