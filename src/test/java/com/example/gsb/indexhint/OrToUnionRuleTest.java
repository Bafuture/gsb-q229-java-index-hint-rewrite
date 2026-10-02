package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class OrToUnionRuleTest {

    private final OrToUnionRule rule = new OrToUnionRule();
    private final List<Index> indexes = List.of(
            Index.of("idx_type", "type"),
            Index.of("idx_code", "code"));

    @Test
    void splitsOrIntoIndexableUnionBranches() {
        Query query = Query.on("orders",
                FieldCondition.eq("tenant_id", 1),
                OrCondition.of(FieldCondition.eq("type", "VIP"), FieldCondition.eq("code", "X")));

        RuleApplication result = rule.apply(query, indexes);

        assertThat(result.applied()).isTrue();
        assertThat(result.query()).isInstanceOf(UnionQuery.class);
        UnionQuery union = (UnionQuery) result.query();
        assertThat(union.branches()).hasSize(2);
        assertThat(union.branches().get(0).conditions())
                .extracting(Condition::describe)
                .containsExactly("tenant_id = 1", "type = 'VIP'");
        assertThat(union.branches().get(1).conditions())
                .extracting(Condition::describe)
                .containsExactly("tenant_id = 1", "code = 'X'");
        assertThat(union.describe()).contains("UNION ALL");
    }

    @Test
    void keepsOrWhenNoBranchCanUseAnIndex() {
        Query query = Query.on("orders",
                OrCondition.of(FieldCondition.eq("type", "VIP"), FieldCondition.eq("remark", "hello")));

        RuleApplication result = rule.apply(query, indexes);

        assertThat(result.applied()).isFalse();
        assertThat(result.query()).isSameAs(query);
    }

    @Test
    void keepsOrWhenBranchUsesNonUsableOperator() {
        Query query = Query.on("orders",
                OrCondition.of(new FieldCondition("type", Operator.NE, "VIP"),
                        FieldCondition.eq("code", "X")));

        RuleApplication result = rule.apply(query, indexes);

        assertThat(result.applied()).isFalse();
    }
}
