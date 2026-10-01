package com.example.gsb.indexhint.exec;

import com.example.gsb.indexhint.model.AndCondition;
import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.model.FunctionCondition;
import com.example.gsb.indexhint.model.Operator;
import com.example.gsb.indexhint.model.OrCondition;
import com.example.gsb.indexhint.model.UnionCondition;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class ConditionEvaluator {

    private ConditionEvaluator() {
    }

    public static boolean matches(Condition condition, Map<String, Object> row) {
        if (condition instanceof Comparison cmp) {
            return compare(cmp.operator(), row.get(cmp.field()), cmp.value());
        }
        if (condition instanceof FunctionCondition fn) {
            Object actual = applyFunction(fn.function(), row.get(fn.field()));
            return compare(fn.operator(), actual, fn.value());
        }
        if (condition instanceof AndCondition and) {
            return and.conditions().stream().allMatch(c -> matches(c, row));
        }
        if (condition instanceof OrCondition or) {
            return or.conditions().stream().anyMatch(c -> matches(c, row));
        }
        if (condition instanceof UnionCondition union) {
            return union.branches().stream().anyMatch(b -> matches(b, row));
        }
        throw new IllegalArgumentException("未知条件类型: " + condition);
    }

    private static Object applyFunction(String function, Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return switch (function.toUpperCase()) {
            case "DATE" -> text.substring(0, Math.min(10, text.length()));
            case "YEAR" -> Integer.valueOf(text.substring(0, 4));
            case "LOWER" -> text.toLowerCase();
            default -> throw new IllegalArgumentException("不支持的函数: " + function);
        };
    }

    private static boolean compare(Operator op, Object actual, Object expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return switch (op) {
            case EQ -> compareTo(actual, expected) == 0;
            case NE -> compareTo(actual, expected) != 0;
            case GT -> compareTo(actual, expected) > 0;
            case GE -> compareTo(actual, expected) >= 0;
            case LT -> compareTo(actual, expected) < 0;
            case LE -> compareTo(actual, expected) <= 0;
            case IN -> expected instanceof List<?> list
                    && list.stream().anyMatch(e -> compareTo(actual, e) == 0);
            case BETWEEN -> expected instanceof List<?> list && list.size() == 2
                    && compareTo(actual, list.get(0)) >= 0 && compareTo(actual, list.get(1)) <= 0;
            case LIKE -> likeMatches(actual.toString(), expected.toString());
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compareTo(Object a, Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        if (a instanceof Comparable c && a.getClass().isInstance(b)) {
            return c.compareTo(b);
        }
        return a.toString().compareTo(b.toString());
    }

    private static boolean likeMatches(String actual, String pattern) {
        StringBuilder regex = new StringBuilder();
        for (char ch : pattern.toCharArray()) {
            switch (ch) {
                case '%' -> regex.append(".*");
                case '_' -> regex.append('.');
                default -> regex.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        return actual.matches(regex.toString());
    }
}
