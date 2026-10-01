package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gsb.indexhint.model.AndCondition;
import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.model.Conditions;
import com.example.gsb.indexhint.model.FunctionCondition;
import com.example.gsb.indexhint.model.Operator;
import com.example.gsb.indexhint.rule.FunctionToRangeRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class FunctionToRangeRuleTest {

    private final FunctionToRangeRule rule = new FunctionToRangeRule();

    @Test
    void rewritesDateEqualityToRangeCondition() {
        RewriteStatistics stats = new RewriteStatistics();
        Condition condition = AndCondition.of(
                new FunctionCondition("date", "created_at", Operator.EQ, "2024-01-15"),
                Comparison.eq("status", "PAID"));

        Condition rewritten = rule.apply(condition, List.of(), stats);

        assertThat(rewritten).isInstanceOf(AndCondition.class);
        assertThat(Conditions.flattenComparisons(rewritten)).containsExactly(
                new Comparison("created_at", Operator.GE, "2024-01-15"),
                new Comparison("created_at", Operator.LT, "2024-01-16"),
                Comparison.eq("status", "PAID"));
        assertThat(stats.ruleHitCount(FunctionToRangeRule.NAME)).isEqualTo(1);
    }

    @Test
    void rewritesYearEqualityToRangeCondition() {
        RewriteStatistics stats = new RewriteStatistics();
        Condition condition = new FunctionCondition("year", "created_at", Operator.EQ, 2024);

        Condition rewritten = rule.apply(condition, List.of(), stats);

        assertThat(Conditions.flattenComparisons(rewritten)).containsExactly(
                new Comparison("created_at", Operator.GE, "2024"),
                new Comparison("created_at", Operator.LT, "2025"));
        assertThat(stats.ruleHitCount(FunctionToRangeRule.NAME)).isEqualTo(1);
    }

    @Test
    void keepsUnsupportedFunctionUntouched() {
        RewriteStatistics stats = new RewriteStatistics();
        Condition condition = new FunctionCondition("lower", "email", Operator.EQ, "a@b.c");

        Condition rewritten = rule.apply(condition, List.of(), stats);

        assertThat(rewritten).isSameAs(condition);
        assertThat(stats.ruleHitCount(FunctionToRangeRule.NAME)).isZero();
    }
}
