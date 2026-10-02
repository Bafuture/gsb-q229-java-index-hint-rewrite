package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class FunctionToRangeRuleTest {

    private final FunctionToRangeRule rule = new FunctionToRangeRule();

    @Test
    void rewritesDateEqualityIntoHalfOpenRange() {
        Query query = Query.on("orders",
                new FunctionCondition("date", "created_at", Operator.EQ, "2024-01-15"));

        RuleApplication result = rule.apply(query, List.of());

        assertThat(result.applied()).isTrue();
        Query rewritten = (Query) result.query();
        assertThat(rewritten.conditions()).containsExactly(
                new FieldCondition("created_at", Operator.GE, "2024-01-15 00:00:00"),
                new FieldCondition("created_at", Operator.LT, "2024-01-16 00:00:00"));
        assertThat(result.detail()).contains("DATE(created_at) = '2024-01-15'");
    }

    @Test
    void rewritesYearEqualityIntoYearRange() {
        Query query = Query.on("orders",
                new FunctionCondition("YEAR", "created_at", Operator.EQ, 2024));

        RuleApplication result = rule.apply(query, List.of());

        assertThat(result.applied()).isTrue();
        Query rewritten = (Query) result.query();
        assertThat(rewritten.conditions()).containsExactly(
                new FieldCondition("created_at", Operator.GE, "2024-01-01 00:00:00"),
                new FieldCondition("created_at", Operator.LT, "2025-01-01 00:00:00"));
    }

    @Test
    void leavesPlainFieldConditionsUntouched() {
        Query query = Query.on("orders", FieldCondition.eq("status", "PAID"));

        RuleApplication result = rule.apply(query, List.of());

        assertThat(result.applied()).isFalse();
        assertThat(result.query()).isSameAs(query);
    }

    @Test
    void rewrittenRangeIsExecutableAndBoundaryAware() {
        Query query = Query.on("orders",
                new FunctionCondition("DATE", "created_at", Operator.EQ, "2024-01-15"));
        Query rewritten = (Query) rule.apply(query, List.of()).query();

        assertThat(rewritten.conditions().get(0).test(
                TestData.row("created_at", "2024-01-15 00:00:00"))).isTrue();
        assertThat(rewritten.conditions().get(0).test(
                TestData.row("created_at", "2024-01-15 23:59:59"))).isTrue();
        assertThat(rewritten.conditions().get(1).test(
                TestData.row("created_at", "2024-01-16 00:00:00"))).isFalse();
        assertThat(rewritten.conditions().get(0).test(
                TestData.row("created_at", "2024-01-14 23:59:59"))).isFalse();
    }
}
