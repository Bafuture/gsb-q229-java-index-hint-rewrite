package com.example.gsb.indexhint.model;

public enum Operator {
    EQ("="),
    NE("<>"),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<="),
    IN("IN"),
    BETWEEN("BETWEEN"),
    LIKE("LIKE");

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
