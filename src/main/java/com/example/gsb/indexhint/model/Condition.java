package com.example.gsb.indexhint.model;

public sealed interface Condition
        permits Comparison, FunctionCondition, AndCondition, OrCondition, UnionCondition {
}
