package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class QueryOptimizerReportTest {

    private final QueryOptimizer optimizer = new QueryOptimizer();

    private final TableStats stats = new TableStats(100_000)
            .withColumn("merchant_id", 100)
            .withColumn("status", 5)
            .withColumn("type", 20)
            .withColumn("code", 1000)
            .withColumn("tenant_id", 10)
            .withColumn("amount", 10_000, 0, 100_000);

    private final List<Index> indexes = List.of(
            Index.of("idx_merchant_status", "merchant_id", "status"),
            Index.of("idx_type", "type"),
            Index.of("idx_code", "code"),
            Index.of("idx_amount", "amount"));

    private final List<Map<String, Object>> sampleRows = List.of(
            TestData.row("merchant_id", 7, "status", "PAID", "created_at", "2024-01-15 10:00:00"),
            TestData.row("merchant_id", 7, "status", "PAID", "created_at", "2024-01-15 23:59:59"),
            TestData.row("merchant_id", 7, "status", "PAID", "created_at", "2024-01-16 00:00:00"),
            TestData.row("merchant_id", 7, "status", "UNPAID", "created_at", "2024-01-15 11:00:00"),
            TestData.row("merchant_id", 8, "status", "PAID", "created_at", "2024-01-15 09:00:00"),
            TestData.row("merchant_id", 7, "status", "PAID", "created_at", null));

    @Test
    void fullPipelineRewritesSelectsIndexAndVerifiesEquivalence() {
        Query query = Query.on("orders",
                FieldCondition.eq("status", "PAID"),
                new FunctionCondition("DATE", "created_at", Operator.EQ, "2024-01-15"),
                FieldCondition.eq("merchant_id", 7));

        OptimizationResult result =
                optimizer.optimizeWithSample(query, indexes, stats, null, sampleRows);

        Query rewritten = (Query) result.rewrittenQuery();
        assertThat(rewritten.conditions())
                .extracting(condition -> ((FieldCondition) condition).field())
                .containsExactly("merchant_id", "status", "created_at", "created_at");

        assertThat(result.chosenIndex().index().name()).isEqualTo("idx_merchant_status");
        assertThat(result.chosenIndex().matchedColumns()).isEqualTo(2);
        assertThat(result.equivalence().equivalent()).isTrue();

        RewriteStatistics statistics = result.statistics();
        assertThat(statistics.ruleHits())
                .containsEntry("FUNCTION_TO_RANGE", 1)
                .containsEntry("COMPOSITE_INDEX_REORDER", 1)
                .doesNotContainKey("OR_TO_UNION");
        assertThat(statistics.candidateIndexCount()).isEqualTo(1);
        assertThat(statistics.excludedIndexes())
                .extracting(RewriteStatistics.ExcludedIndex::indexName)
                .containsExactly("idx_type", "idx_code", "idx_amount");
        assertThat(statistics.excludedIndexes())
                .allSatisfy(excluded -> assertThat(excluded.reason()).contains("缺少可用的等值或范围条件"));
    }

    @Test
    void reportContainsBeforeAfterConditionsIndexAndStatistics() {
        Query query = Query.on("orders",
                FieldCondition.eq("status", "PAID"),
                new FunctionCondition("DATE", "created_at", Operator.EQ, "2024-01-15"),
                FieldCondition.eq("merchant_id", 7));

        String report = optimizer
                .optimizeWithSample(query, indexes, stats, null, sampleRows)
                .report();

        assertThat(report).contains(
                "原始条件: status = 'PAID' AND DATE(created_at) = '2024-01-15' AND merchant_id = 7",
                "重写后条件:",
                "created_at >= '2024-01-15 00:00:00'",
                "created_at < '2024-01-16 00:00:00'",
                "选中索引: idx_merchant_status",
                "原因:",
                "规则命中:",
                "FUNCTION_TO_RANGE",
                "COMPOSITE_INDEX_REORDER",
                "可选索引数: 1",
                "被排除索引: idx_type",
                "被排除索引: idx_code",
                "被排除索引: idx_amount",
                "等价性验证: 通过");
    }

    @Test
    void orRewriteProducesPerBranchIndexSelection() {
        List<Index> unionIndexes = List.of(
                Index.of("idx_tenant_type", "tenant_id", "type"),
                Index.of("idx_tenant_code", "tenant_id", "code"),
                Index.of("idx_amount", "amount"));
        Query query = Query.on("orders",
                FieldCondition.eq("tenant_id", 1),
                OrCondition.of(FieldCondition.eq("type", "VIP"), FieldCondition.eq("code", "X")));

        List<Map<String, Object>> rows = List.of(
                TestData.row("tenant_id", 1, "type", "VIP", "code", "A"),
                TestData.row("tenant_id", 1, "type", "NORMAL", "code", "X"),
                TestData.row("tenant_id", 1, "type", "NORMAL", "code", "A"),
                TestData.row("tenant_id", 2, "type", "VIP", "code", "X"));

        OptimizationResult result =
                optimizer.optimizeWithSample(query, unionIndexes, stats, null, rows);

        assertThat(result.rewrittenQuery()).isInstanceOf(UnionQuery.class);
        assertThat(result.selections()).hasSize(2);
        assertThat(result.selections().get(0).chosen().index().name())
                .isEqualTo("idx_tenant_type");
        assertThat(result.selections().get(1).chosen().index().name())
                .isEqualTo("idx_tenant_code");
        assertThat(result.statistics().ruleHits()).containsEntry("OR_TO_UNION", 1);
        assertThat(result.statistics().candidateIndexCount()).isEqualTo(2);
        assertThat(result.statistics().excludedIndexes())
                .extracting(RewriteStatistics.ExcludedIndex::indexName)
                .containsExactly("idx_amount");
        assertThat(result.equivalence().equivalent()).isTrue();
        assertThat(result.equivalence().originalCount()).isEqualTo(2);
        assertThat(result.report()).contains("选中索引(分支1)", "选中索引(分支2)");
    }
}
