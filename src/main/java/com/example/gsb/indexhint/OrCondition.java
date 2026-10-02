package com.example.gsb.indexhint;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record OrCondition(List<Condition> branches) implements Condition {

    public OrCondition {
        branches = List.copyOf(branches);
    }

    public static OrCondition of(Condition... branches) {
        return new OrCondition(List.of(branches));
    }

    @Override
    public boolean test(Map<String, Object> row) {
        for (Condition branch : branches) {
            if (branch.test(row)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String describe() {
        return branches.stream()
                .map(Condition::describe)
                .collect(Collectors.joining(" OR ", "(", ")"));
    }
}
