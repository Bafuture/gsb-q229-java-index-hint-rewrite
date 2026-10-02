package com.example.gsb.indexhint;

import java.util.Locale;
import java.util.Map;

public record FunctionCondition(String function, String field, Operator operator, Object value)
        implements Condition {

    public FunctionCondition {
        function = function.toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean test(Map<String, Object> row) {
        Object transformed = apply(row.get(field));
        return switch (operator) {
            case EQ -> Values.compare(transformed, value) == 0;
            case NE -> Values.compare(transformed, value) != 0;
            case GT -> Values.compare(transformed, value) > 0;
            case GE -> Values.compare(transformed, value) >= 0;
            case LT -> Values.compare(transformed, value) < 0;
            case LE -> Values.compare(transformed, value) <= 0;
            case IN, LIKE -> throw new UnsupportedOperationException(
                    "函数条件暂不支持操作符: " + operator);
        };
    }

    Object apply(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = String.valueOf(raw);
        return switch (function) {
            case "DATE" -> text.length() >= 10 ? text.substring(0, 10) : text;
            case "YEAR" -> parseYear(text);
            default -> throw new IllegalArgumentException("不支持的函数: " + function);
        };
    }

    private static Integer parseYear(String text) {
        if (text.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(text.substring(0, 4));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public String describe() {
        return function + "(" + field + ") " + operator.symbol() + " " + Values.format(value);
    }
}
