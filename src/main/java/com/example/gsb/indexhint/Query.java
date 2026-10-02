package com.example.gsb.indexhint;

import java.util.List;
import java.util.stream.Collectors;

public final class Query implements QueryLike {

    private final String table;
    private final List<Condition> conditions;

    public Query(String table, List<Condition> conditions) {
        this.table = table;
        this.conditions = List.copyOf(conditions);
    }

    public static Query on(String table, Condition... conditions) {
        return new Query(table, List.of(conditions));
    }

    public String table() {
        return table;
    }

    public List<Condition> conditions() {
        return conditions;
    }

    public Query withConditions(List<Condition> newConditions) {
        return new Query(table, newConditions);
    }

    @Override
    public String describe() {
        if (conditions.isEmpty()) {
            return "(无条件)";
        }
        return conditions.stream()
                .map(Condition::describe)
                .collect(Collectors.joining(" AND "));
    }
}
