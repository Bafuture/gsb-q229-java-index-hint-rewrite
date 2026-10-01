package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gsb.indexhint.exec.EquivalenceResult;
import com.example.gsb.indexhint.model.AndCondition;
import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.FunctionCondition;
import com.example.gsb.indexhint.model.IndexDefinition;
import com.example.gsb.indexhint.model.Operator;
import com.example.gsb.indexhint.model.OrCondition;
import com.example.gsb.indexhint.model.UnionCondition;
import com.example.gsb.indexhint.rule.CompositeIndexOrderRule;
import com.example.gsb.indexhint.rule.FunctionToRangeRule;
import com.example.gsb.indexhint.rule.OrToUnionRule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QueryRewriteEngineEndToEndTest {

    @Test
    void rewritesSelectsIndexesAndStaysEquivalentEndToEnd() {
        IndexDefinition composite = IndexDefinition.of("idx_status_created", "status", "created_at");
        IndexDefinition idxCustomer = IndexDefinition.of("idx_customer", "customer_id");
        IndexDefinition idxNote = IndexDefinition.of("idx_note", "note");

        var request = RewriteRequest.of(
                OrCondition.of(
                        AndCondition.of(
                                new FunctionCondition("date", "created_at", Operator.EQ, "2024-01-15"),
                                Comparison.eq("status", "PAID")),
                        AndCondition.of(
                                Comparison.eq("customer_id", 7),
                                Comparison.gt("amount", 100))),
                List.of(composite, idxCustomer, idxNote));

        QueryRewriteEngine engine = new QueryRewriteEngine();
        RewriteResult result = engine.rewrite(request);

        assertThat(result.rewritten()).isInstanceOf(UnionCondition.class);
        var union = (UnionCondition) result.rewritten();
        assertThat(((AndCondition) union.branches().get(0)).conditions()).containsExactly(
                Comparison.eq("status", "PAID"),
                Comparison.ge("created_at", "2024-01-15"),
                Comparison.lt("created_at", "2024-01-16"));

        assertThat(result.report().selections()).hasSize(2);
        assertThat(result.report().selections().get(0).chosen().name())
                .isEqualTo("idx_status_created");
        assertThat(result.report().selections().get(1).chosen().name())
                .isEqualTo("idx_customer");

        RewriteStatistics stats = result.statistics();
        assertThat(stats.ruleHitCount(FunctionToRangeRule.NAME)).isEqualTo(1);
        assertThat(stats.ruleHitCount(OrToUnionRule.NAME)).isEqualTo(1);
        assertThat(stats.ruleHitCount(CompositeIndexOrderRule.NAME)).isEqualTo(1);
        assertThat(stats.candidateIndexCount()).isEqualTo(2);
        assertThat(stats.excludedIndexes()).containsKey("idx_note");

        List<Map<String, Object>> dataset = List.of(
                TestRows.row("status", "PAID", "created_at", "2024-01-15 08:00:00",
                        "customer_id", 1, "amount", 50),
                TestRows.row("status", "PAID", "created_at", "2024-01-16 00:00:00",
                        "customer_id", 2, "amount", 60),
                TestRows.row("status", "NEW", "created_at", "2024-01-15 10:00:00",
                        "customer_id", 3, "amount", 70),
                TestRows.row("status", "NEW", "created_at", "2024-02-01 09:00:00",
                        "customer_id", 7, "amount", 150),
                TestRows.row("status", "NEW", "created_at", "2024-02-01 09:00:00",
                        "customer_id", 7, "amount", 50),
                TestRows.row("status", "PAID", "created_at", "2024-01-15",
                        "customer_id", 9, "amount", 10),
                TestRows.row("status", "PAID", "created_at", "2024-01-14 23:59:59",
                        "customer_id", 8, "amount", 200));

        EquivalenceResult equivalence = result.verifyEquivalence(dataset);
        assertThat(equivalence.equivalent()).as(result.report().format()).isTrue();
        assertThat(result.execute(dataset)).hasSize(3);
    }
}
