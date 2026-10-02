package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class ForcedHintTest {

    private final QueryOptimizer optimizer = new QueryOptimizer();
    private final TableStats stats = new TableStats(100_000)
            .withColumn("merchant_id", 100)
            .withColumn("status", 5)
            .withColumn("amount", 10_000, 0, 100_000);
    private final List<Index> indexes = List.of(
            Index.of("idx_status", "status"),
            Index.of("idx_merchant_status", "merchant_id", "status"),
            Index.of("idx_amount", "amount"));

    @Test
    void honorsForcedIndexEvenWhenNotCheapest() {
        Query query = Query.on("orders",
                FieldCondition.eq("status", "PAID"),
                FieldCondition.eq("merchant_id", 7));

        OptimizationResult result = optimizer.optimize(query, indexes, stats, "idx_status");

        assertThat(result.chosenIndex().index().name()).isEqualTo("idx_status");
        assertThat(result.forcedIndex()).isEqualTo("idx_status");
        assertThat(result.report()).contains("调用方强制提示");
    }

    @Test
    void rejectsForcedIndexThatCannotCoverConditions() {
        Query query = Query.on("orders",
                FieldCondition.eq("merchant_id", 7));

        assertThatThrownBy(() -> optimizer.optimize(query, indexes, stats, "idx_amount"))
                .isInstanceOf(IndexHintException.class)
                .hasMessageContaining("idx_amount")
                .hasMessageContaining("无法覆盖查询条件")
                .hasMessageContaining("amount")
                .hasMessageContaining("merchant_id = 7");
    }

    @Test
    void rejectsUnknownForcedIndexAndListsAvailableOnes() {
        Query query = Query.on("orders", FieldCondition.eq("status", "PAID"));

        assertThatThrownBy(() -> optimizer.optimize(query, indexes, stats, "idx_missing"))
                .isInstanceOf(IndexHintException.class)
                .hasMessageContaining("不存在于可用索引清单")
                .hasMessageContaining("idx_status")
                .hasMessageContaining("idx_amount");
    }
}
