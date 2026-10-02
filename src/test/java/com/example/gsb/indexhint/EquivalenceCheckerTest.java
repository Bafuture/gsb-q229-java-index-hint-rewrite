package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class EquivalenceCheckerTest {

    private final List<Map<String, Object>> rows = List.of(
            TestData.row("id", 1, "created_at", "2024-01-14 23:59:59", "status", "PAID"),
            TestData.row("id", 2, "created_at", "2024-01-15 00:00:00", "status", "PAID"),
            TestData.row("id", 3, "created_at", "2024-01-15 12:30:00", "status", "UNPAID"),
            TestData.row("id", 4, "created_at", "2024-01-15 23:59:59", "status", "PAID"),
            TestData.row("id", 5, "created_at", "2024-01-16 00:00:00", "status", "PAID"),
            TestData.row("id", 6, "created_at", "2025-01-01 00:00:00", "status", "PAID"),
            TestData.row("id", 7, "created_at", null, "status", "PAID"));

    @Test
    void dateFunctionRewrittenToRangeIsEquivalent() {
        Query original = Query.on("orders",
                new FunctionCondition("DATE", "created_at", Operator.EQ, "2024-01-15"));
        QueryLike rewritten = new FunctionToRangeRule().apply(original, List.of()).query();

        EquivalenceChecker.EquivalenceResult result =
                EquivalenceChecker.verify(original, rewritten, rows);

        assertThat(result.equivalent()).isTrue();
        assertThat(result.originalCount()).isEqualTo(3);
        assertThat(result.rewrittenCount()).isEqualTo(3);
        assertThat(result.summary()).contains("通过");
    }

    @Test
    void yearFunctionRewrittenToRangeIsEquivalent() {
        Query original = Query.on("orders",
                new FunctionCondition("YEAR", "created_at", Operator.EQ, 2024));
        QueryLike rewritten = new FunctionToRangeRule().apply(original, List.of()).query();

        EquivalenceChecker.EquivalenceResult result =
                EquivalenceChecker.verify(original, rewritten, rows);

        assertThat(result.equivalent()).isTrue();
        assertThat(result.originalCount()).isEqualTo(5);
    }

    @Test
    void orSplitIntoUnionIsEquivalent() {
        List<Index> indexes = List.of(
                Index.of("idx_status", "status"),
                Index.of("idx_id", "id"));
        Query original = Query.on("orders",
                OrCondition.of(FieldCondition.eq("status", "UNPAID"), FieldCondition.eq("id", 5)));
        QueryLike rewritten = new OrToUnionRule().apply(original, indexes).query();

        EquivalenceChecker.EquivalenceResult result =
                EquivalenceChecker.verify(original, rewritten, rows);

        assertThat(result.equivalent()).isTrue();
        assertThat(result.originalCount()).isEqualTo(2);
        assertThat(result.rewrittenCount()).isEqualTo(2);
    }

    @Test
    void detectsNonEquivalentRewrite() {
        Query original = Query.on("orders",
                new FunctionCondition("DATE", "created_at", Operator.EQ, "2024-01-15"));
        Query broken = Query.on("orders",
                new FieldCondition("created_at", Operator.GE, "2024-01-14 00:00:00"),
                new FieldCondition("created_at", Operator.LT, "2024-01-16 00:00:00"));

        EquivalenceChecker.EquivalenceResult result =
                EquivalenceChecker.verify(original, broken, rows);

        assertThat(result.equivalent()).isFalse();
        assertThat(result.summary()).contains("不一致", "多出");
    }
}
