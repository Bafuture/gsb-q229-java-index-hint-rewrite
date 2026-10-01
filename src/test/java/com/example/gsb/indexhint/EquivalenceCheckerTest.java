package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gsb.indexhint.exec.EquivalenceChecker;
import com.example.gsb.indexhint.exec.EquivalenceResult;
import com.example.gsb.indexhint.model.AndCondition;
import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.FunctionCondition;
import com.example.gsb.indexhint.model.IndexDefinition;
import com.example.gsb.indexhint.model.Operator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EquivalenceCheckerTest {

    private final EquivalenceChecker checker = new EquivalenceChecker();

    private final List<Map<String, Object>> dataset = List.of(
            TestRows.row("created_at", "2024-01-15 08:00:00"),
            TestRows.row("created_at", "2024-01-15"),
            TestRows.row("created_at", "2024-01-15 23:59:59"),
            TestRows.row("created_at", "2024-01-16 00:00:00"),
            TestRows.row("created_at", "2024-01-14 23:59:59"));

    @Test
    void dateRewriteIsEquivalentOnDataset() {
        var original = new FunctionCondition("date", "created_at", Operator.EQ, "2024-01-15");
        var rewritten = AndCondition.of(
                Comparison.ge("created_at", "2024-01-15"),
                Comparison.lt("created_at", "2024-01-16"));

        EquivalenceResult result = checker.verify(original, rewritten, dataset);

        assertThat(result.equivalent()).isTrue();
        assertThat(result.beforeCount()).isEqualTo(3);
        assertThat(result.afterCount()).isEqualTo(3);
        assertThat(result.mismatches()).isEmpty();
    }

    @Test
    void detectsNonEquivalentRewrite() {
        var original = Comparison.eq("created_at", "2024-01-15");
        var wrong = Comparison.eq("created_at", "2024-01-16");

        EquivalenceResult result = checker.verify(original, wrong, dataset);

        assertThat(result.equivalent()).isFalse();
        assertThat(result.mismatches()).isNotEmpty();
    }

    @Test
    void engineRewriteVerifiesEquivalentIncludingBoundaries() {
        QueryRewriteEngine engine = new QueryRewriteEngine();
        var request = RewriteRequest.of(
                AndCondition.of(
                        new FunctionCondition("date", "created_at", Operator.EQ, "2024-01-15"),
                        Comparison.eq("status", "PAID")),
                List.of(IndexDefinition.of("idx_created", "created_at")));

        RewriteResult result = engine.rewrite(request);
        List<Map<String, Object>> rows = List.of(
                TestRows.row("created_at", "2024-01-15 08:00:00", "status", "PAID"),
                TestRows.row("created_at", "2024-01-16 00:00:00", "status", "PAID"),
                TestRows.row("created_at", "2024-01-15 12:00:00", "status", "DONE"));

        EquivalenceResult equivalence = result.verifyEquivalence(rows);
        assertThat(equivalence.equivalent()).isTrue();
        assertThat(result.execute(rows)).hasSize(1);
    }
}
