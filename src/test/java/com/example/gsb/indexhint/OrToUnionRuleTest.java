package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.model.IndexDefinition;
import com.example.gsb.indexhint.model.OrCondition;
import com.example.gsb.indexhint.model.UnionCondition;
import com.example.gsb.indexhint.rule.OrToUnionRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrToUnionRuleTest {

    private final OrToUnionRule rule = new OrToUnionRule();
    private final List<IndexDefinition> indexes = List.of(
            IndexDefinition.of("idx_status", "status"),
            IndexDefinition.of("idx_customer", "customer_id"));

    @Test
    void rewritesOrToUnionWhenEveryBranchCanUseIndex() {
        RewriteStatistics stats = new RewriteStatistics();
        Condition condition = OrCondition.of(
                Comparison.eq("status", "NEW"),
                Comparison.eq("customer_id", 7));

        Condition rewritten = rule.apply(condition, indexes, stats);

        assertThat(rewritten).isInstanceOf(UnionCondition.class);
        assertThat(((UnionCondition) rewritten).branches()).hasSize(2);
        assertThat(stats.ruleHitCount(OrToUnionRule.NAME)).isEqualTo(1);
    }

    @Test
    void keepsOrWhenSomeBranchCannotUseAnyIndex() {
        RewriteStatistics stats = new RewriteStatistics();
        Condition condition = OrCondition.of(
                Comparison.eq("status", "NEW"),
                Comparison.eq("note", "hello"));

        Condition rewritten = rule.apply(condition, indexes, stats);

        assertThat(rewritten).isInstanceOf(OrCondition.class);
        assertThat(stats.ruleHitCount(OrToUnionRule.NAME)).isZero();
    }
}
