package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class IndexSelectorTest {

    private final IndexSelector selector = new IndexSelector();

    private final TableStats stats = new TableStats(100_000)
            .withColumn("merchant_id", 100)
            .withColumn("status", 5)
            .withColumn("amount", 10_000, 0, 100_000);

    private final List<Index> indexes = List.of(
            Index.of("idx_status", "status"),
            Index.of("idx_merchant_status", "merchant_id", "status"),
            Index.of("idx_amount", "amount"));

    @Test
    void picksLowestCostCompositeIndexAndExplainsWhy() {
        Query query = Query.on("orders",
                FieldCondition.eq("status", "PAID"),
                FieldCondition.eq("merchant_id", 7));

        IndexSelection selection = selector.select(query, indexes, stats);

        assertThat(selection.chosen().index().name()).isEqualTo("idx_merchant_status");
        assertThat(selection.chosen().matchedColumns()).isEqualTo(2);
        assertThat(selection.chosen().estimatedRows()).isEqualTo(200);
        assertThat(selection.chosen().filterRatio()).isCloseTo(0.002, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(selection.chosen().reason())
                .contains("覆盖前 2 列", "[merchant_id, status]", "扫描约 200 行");
    }

    @Test
    void singleColumnIndexIsChosenWhenMoreSelective() {
        Query query = Query.on("orders",
                FieldCondition.eq("status", "PAID"));

        IndexSelection selection = selector.select(query, indexes, stats);

        assertThat(selection.chosen().index().name()).isEqualTo("idx_status");
        assertThat(selection.chosen().estimatedRows()).isEqualTo(20_000);
    }

    @Test
    void rangeConditionOnAmountUsesMinMaxForEstimation() {
        Query query = Query.on("orders",
                new FieldCondition("amount", Operator.GT, 90_000));

        IndexSelection selection = selector.select(query, indexes, stats);

        assertThat(selection.chosen().index().name()).isEqualTo("idx_amount");
        assertThat(selection.chosen().estimatedRows()).isEqualTo(10_000);
        assertThat(selection.chosen().matchedColumns()).isEqualTo(1);
    }

    @Test
    void excludesIndexesWithoutConditionOnFirstColumnWithReason() {
        Query query = Query.on("orders",
                FieldCondition.eq("status", "PAID"),
                FieldCondition.eq("merchant_id", 7));

        IndexSelection selection = selector.select(query, indexes, stats);

        assertThat(selection.candidates()).hasSize(2);
        assertThat(selection.excluded()).hasSize(1);
        IndexEvaluation excluded = selection.excluded().get(0);
        assertThat(excluded.index().name()).isEqualTo("idx_amount");
        assertThat(excluded.usable()).isFalse();
        assertThat(excluded.reason()).contains("amount", "缺少可用的等值或范围条件");
    }

    @Test
    void noUsableIndexMeansFullTableScan() {
        Query query = Query.on("orders", FieldCondition.eq("unknown_col", 1));

        IndexSelection selection = selector.select(query, indexes, stats);

        assertThat(selection.chosen()).isNull();
        assertThat(selection.excluded()).hasSize(3);
    }
}
