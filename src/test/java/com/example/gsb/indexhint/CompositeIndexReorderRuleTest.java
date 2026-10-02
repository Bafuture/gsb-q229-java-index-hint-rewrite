package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CompositeIndexReorderRuleTest {

    private final CompositeIndexReorderRule rule = new CompositeIndexReorderRule();
    private final Index compositeIndex =
            Index.of("idx_merchant_status", "merchant_id", "status");

    @Test
    void movesCompositeIndexPrefixColumnsToFront() {
        Query query = Query.on("orders",
                FieldCondition.eq("status", "PAID"),
                new FieldCondition("created_at", Operator.GT, "2024-01-01 00:00:00"),
                FieldCondition.eq("merchant_id", 7));

        RuleApplication result = rule.apply(query, List.of(compositeIndex));

        assertThat(result.applied()).isTrue();
        Query rewritten = (Query) result.query();
        assertThat(rewritten.conditions())
                .extracting(condition -> ((FieldCondition) condition).field())
                .containsExactly("merchant_id", "status", "created_at");
        assertThat(result.detail()).contains("idx_merchant_status", "merchant_id", "status");
    }

    @Test
    void keepsAlreadyOptimalOrderUntouched() {
        Query query = Query.on("orders",
                FieldCondition.eq("merchant_id", 7),
                FieldCondition.eq("status", "PAID"),
                new FieldCondition("created_at", Operator.GT, "2024-01-01 00:00:00"));

        RuleApplication result = rule.apply(query, List.of(compositeIndex));

        assertThat(result.applied()).isFalse();
        assertThat(result.query()).isSameAs(query);
    }

    @Test
    void doesNotReorderWhenOnlySinglePrefixColumnMatched() {
        Query query = Query.on("orders",
                FieldCondition.eq("status", "PAID"),
                FieldCondition.eq("merchant_id", 7));

        RuleApplication result = rule.apply(query, List.of(Index.of("idx_merchant", "merchant_id")));

        assertThat(result.applied()).isFalse();
    }
}
