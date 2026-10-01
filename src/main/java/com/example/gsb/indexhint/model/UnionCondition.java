package com.example.gsb.indexhint.model;

import java.util.List;
import java.util.stream.Collectors;

public record UnionCondition(List<Condition> branches) implements Condition {

    public UnionCondition {
        branches = List.copyOf(branches);
    }

    @Override
    public String toString() {
        return branches.stream()
                .map(b -> "(" + b + ")")
                .collect(Collectors.joining(" UNION "));
    }
}
