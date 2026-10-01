package com.example.gsb.indexhint.model;

public record FunctionCondition(String function, String field, Operator operator, Object value)
        implements Condition {

    @Override
    public String toString() {
        return function.toUpperCase() + "(" + field + ") " + operator.symbol() + " "
                + Comparison.formatValue(value);
    }
}
