package com.example.gsb.indexhint;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

public record FieldCondition(String field, Operator operator, Object value) implements Condition {

    public static FieldCondition eq(String field, Object value) {
        return new FieldCondition(field, Operator.EQ, value);
    }

    @Override
    public boolean test(Map<String, Object> row) {
        Object actual = row.get(field);
        return switch (operator) {
            case EQ -> Values.compare(actual, value) == 0;
            case NE -> Values.compare(actual, value) != 0;
            case GT -> Values.compare(actual, value) > 0;
            case GE -> Values.compare(actual, value) >= 0;
            case LT -> Values.compare(actual, value) < 0;
            case LE -> Values.compare(actual, value) <= 0;
            case IN -> matchesIn(actual);
            case LIKE -> matchesLike(actual);
        };
    }

    private boolean matchesIn(Object actual) {
        if (!(value instanceof Collection<?> candidates)) {
            return false;
        }
        for (Object candidate : candidates) {
            if (Values.compare(actual, candidate) == 0) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesLike(Object actual) {
        if (actual == null) {
            return false;
        }
        StringBuilder regex = new StringBuilder();
        for (char c : String.valueOf(value).toCharArray()) {
            switch (c) {
                case '%' -> regex.append(".*");
                case '_' -> regex.append('.');
                default -> regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString()).matcher(String.valueOf(actual)).matches();
    }

    public boolean isIndexUsable() {
        return switch (operator) {
            case EQ, IN, GT, GE, LT, LE -> true;
            case LIKE -> !String.valueOf(value).startsWith("%");
            case NE -> false;
        };
    }

    public boolean isEquality() {
        return operator == Operator.EQ || operator == Operator.IN;
    }

    @Override
    public String describe() {
        return field + " " + operator.symbol() + " " + Values.format(value);
    }
}
