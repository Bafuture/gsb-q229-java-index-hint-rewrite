package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gsb.indexhint.model.AndCondition;
import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.model.IndexDefinition;
import com.example.gsb.indexhint.rule.CompositeIndexOrderRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompositeIndexOrderRuleTest {

    private final CompositeIndexOrderRule rule = new CompositeIndexOrderRule();
    private final List<IndexDefinition> indexes = List.of(
            IndexDefinition.of("idx_status_created", "status", "created_at"));

    @Test
    void reordersConditionsToMatchCompositeIndexPrefix() {
        RewriteStatistics stats = new RewriteStatistics();
        Condition condition = AndCondition.of(
                Comparison.ge("created_at", "2024-01-01"),
                Comparison.eq("status", "PAID"));

        Condition rewritten = rule.apply(condition, indexes, stats);

        assertThat(rewritten).isInstanceOf(AndCondition.class);
        assertThat(((AndCondition) rewritten).conditions()).containsExactly(
                Comparison.eq("status", "PAID"),
                Comparison.ge("created_at", "2024-01-01"));
        assertThat(stats.ruleHitCount(CompositeIndexOrderRule.NAME)).isEqualTo(1);
    }

    @Test
    void keepsOrderWhenAlreadyAligned() {
        RewriteStatistics stats = new RewriteStatistics();
        Condition condition = AndCondition.of(
                Comparison.eq("status", "PAID"),
                Comparison.ge("created_at", "2024-01-01"));

        Condition rewritten = rule.apply(condition, indexes, stats);

        assertThat(((AndCondition) rewritten).conditions()).containsExactly(
                Comparison.eq("status", "PAID"),
                Comparison.ge("created_at", "2024-01-01"));
        assertThat(stats.ruleHitCount(CompositeIndexOrderRule.NAME)).isZero();
    }
}
