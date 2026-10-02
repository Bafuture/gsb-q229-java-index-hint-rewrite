package com.example.gsb.indexhint;

import java.util.List;
import java.util.stream.Collectors;

public final class UnionQuery implements QueryLike {

    private final List<Query> branches;

    public UnionQuery(List<Query> branches) {
        this.branches = List.copyOf(branches);
    }

    public List<Query> branches() {
        return branches;
    }

    @Override
    public String describe() {
        return branches.stream()
                .map(Query::describe)
                .collect(Collectors.joining(" UNION ALL ", "(", ")"));
    }
}
