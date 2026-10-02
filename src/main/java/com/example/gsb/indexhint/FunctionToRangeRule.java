package com.example.gsb.indexhint;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 把函数包裹的字段等值条件改写为可走索引的范围条件。
 * 例如 DATE(created_at) = '2024-01-15'
 *      -> created_at >= '2024-01-15 00:00:00' AND created_at < '2024-01-16 00:00:00'
 *      YEAR(created_at) = 2024
 *      -> created_at >= '2024-01-01 00:00:00' AND created_at < '2025-01-01 00:00:00'
 */
public final class FunctionToRangeRule implements RewriteRule {

    public static final String NAME = "FUNCTION_TO_RANGE";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public RuleApplication applyOnQuery(Query query, List<Index> indexes) {
        List<Condition> rewritten = new ArrayList<>();
        List<String> details = new ArrayList<>();
        for (Condition condition : query.conditions()) {
            List<Condition> replacement = tryRewrite(condition);
            if (replacement == null) {
                rewritten.add(condition);
            } else {
                rewritten.addAll(replacement);
                details.add(condition.describe() + " -> "
                        + replacement.get(0).describe() + " AND " + replacement.get(1).describe());
            }
        }
        if (details.isEmpty()) {
            return RuleApplication.unchanged(query);
        }
        return RuleApplication.applied(query.withConditions(rewritten),
                "函数包裹字段改写为范围条件: " + String.join("; ", details));
    }

    private List<Condition> tryRewrite(Condition condition) {
        if (!(condition instanceof FunctionCondition function)) {
            return null;
        }
        if (function.operator() != Operator.EQ) {
            return null;
        }
        return switch (function.function()) {
            case "DATE" -> dateRange(function);
            case "YEAR" -> yearRange(function);
            default -> null;
        };
    }

    private List<Condition> dateRange(FunctionCondition function) {
        LocalDate day;
        try {
            day = LocalDate.parse(String.valueOf(function.value()));
        } catch (DateTimeParseException ignored) {
            return null;
        }
        return List.of(
                new FieldCondition(function.field(), Operator.GE, day + " 00:00:00"),
                new FieldCondition(function.field(), Operator.LT, day.plusDays(1) + " 00:00:00"));
    }

    private List<Condition> yearRange(FunctionCondition function) {
        Double year = Values.asNumber(function.value());
        if (year == null || year != Math.floor(year)) {
            return null;
        }
        int value = year.intValue();
        return List.of(
                new FieldCondition(function.field(), Operator.GE, value + "-01-01 00:00:00"),
                new FieldCondition(function.field(), Operator.LT, (value + 1) + "-01-01 00:00:00"));
    }
}
