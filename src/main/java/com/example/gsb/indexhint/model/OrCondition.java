package com.example.gsb.indexhint.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record OrCondition(List<Condition> conditions) implements Condition {

    public OrCondition {
        conditions = List.copyOf(conditions);
    }

    public static OrCondition of(Condition... conditions) {
        return new OrCondition(Arrays.asList(conditions));
    }

    @Override
    public String toString() {
        return conditions.stream()
                .map(OrCondition::formatChild)
                .collect(Collectors.joining(" OR "));
    }

    static String formatChild(Condition c) {
        if (c instanceof AndCondition || c instanceof UnionCondition) {
            return "(" + c + ")";
        }
        return c.toString();
    }
}
