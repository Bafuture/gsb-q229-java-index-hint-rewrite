package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.IndexDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForcedIndexTest {

    private final QueryRewriteEngine engine = new QueryRewriteEngine();

    private final IndexDefinition idxStatus =
            IndexDefinition.of("idx_status", "status").withSelectivity("status", 0.5);
    private final IndexDefinition idxCustomer =
            IndexDefinition.of("idx_customer", "customer_id").withSelectivity("customer_id", 0.01);
    private final IndexDefinition idxNote = IndexDefinition.of("idx_note", "note");
    private final List<IndexDefinition> indexes = List.of(idxStatus, idxCustomer, idxNote);

    @Test
    void usesForcedIndexEvenWhenMoreCostly() {
        RewriteRequest request = RewriteRequest.of(
                com.example.gsb.indexhint.model.AndCondition.of(
                        Comparison.eq("status", "X"),
                        Comparison.eq("customer_id", 5)),
                indexes);

        RewriteResult result = engine.rewrite(request.forceIndex("idx_status"));

        assertThat(result.report().selections().get(0).chosen().name()).isEqualTo("idx_status");
        assertThat(result.report().selections().get(0).reason()).contains("强制");
    }

    @Test
    void rejectsForcedIndexThatCannotCoverConditionsWithReason() {
        RewriteRequest request = RewriteRequest.of(
                Comparison.eq("customer_id", 5), indexes);

        assertThatThrownBy(() -> engine.rewrite(request.forceIndex("idx_note")))
                .isInstanceOf(IndexHintException.class)
                .hasMessageContaining("idx_note")
                .hasMessageContaining("无法覆盖查询条件")
                .hasMessageContaining("note");
    }

    @Test
    void rejectsUnknownForcedIndex() {
        RewriteRequest request = RewriteRequest.of(
                Comparison.eq("customer_id", 5), indexes);

        assertThatThrownBy(() -> engine.rewrite(request.forceIndex("idx_missing")))
                .isInstanceOf(IndexHintException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void rejectsForcedIndexWhenAnyUnionBranchCannotUseIt() {
        RewriteRequest request = RewriteRequest.of(
                com.example.gsb.indexhint.model.OrCondition.of(
                        Comparison.eq("status", "X"),
                        Comparison.eq("customer_id", 5)),
                indexes);

        assertThatThrownBy(() -> engine.rewrite(request.forceIndex("idx_status")))
                .isInstanceOf(IndexHintException.class)
                .hasMessageContaining("idx_status")
                .hasMessageContaining("无法覆盖查询条件");
    }
}
