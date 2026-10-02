package com.example.gsb.indexhint;

import java.util.List;

public record Index(String name, List<String> columns) {

    public Index {
        columns = List.copyOf(columns);
    }

    public static Index of(String name, String... columns) {
        return new Index(name, List.of(columns));
    }

    public boolean isComposite() {
        return columns.size() > 1;
    }

    public String firstColumn() {
        return columns.get(0);
    }
}
