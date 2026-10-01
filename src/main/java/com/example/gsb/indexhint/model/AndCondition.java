package com.example.gsb.indexhint.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record AndCondition(List<Condition> conditions) implements Condition {

    public AndCondition {
        conditions = List.copyOf(conditions);
    }

    public static AndCondition of(Condition... conditions) {
        return new AndCondition(Arrays.asList(conditions));
    }

    @Override
    public String toString() {
        return conditions.stream()
                .map(AndCondition::formatChild)
                .collect(Collectors.joining(" AND "));
    }

    static String formatChild(Condition c) {
        if (c instanceof OrCondition || c instanceof UnionCondition) {
            return "(" + c + ")";
        }
        return c.toString();
    }
}
