package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gsb.indexhint.model.AndCondition;
import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.FunctionCondition;
import com.example.gsb.indexhint.model.IndexDefinition;
import com.example.gsb.indexhint.model.Operator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportOutputTest {

    @Test
    void reportContainsBeforeAfterConditionsAndChosenIndex() {
        QueryRewriteEngine engine = new QueryRewriteEngine();
        var request = RewriteRequest.of(
                AndCondition.of(
                        new FunctionCondition("date", "created_at", Operator.EQ, "2024-01-15"),
                        Comparison.eq("status", "PAID")),
                List.of(
                        IndexDefinition.of("idx_created", "created_at"),
                        IndexDefinition.of("idx_note", "note")));

        RewriteResult result = engine.rewrite(request);
        String report = result.report().format();

        assertThat(report)
                .contains("[重写前]")
                .contains("DATE(created_at) = '2024-01-15'")
                .contains("[重写后]")
                .contains("created_at >= '2024-01-15'")
                .contains("created_at < '2024-01-16'")
                .contains("[选中索引] 主查询 -> idx_created")
                .contains("选择理由");
    }

    @Test
    void statisticsContainRuleHitsCandidateCountAndExcludedReasons() {
        QueryRewriteEngine engine = new QueryRewriteEngine();
        var request = RewriteRequest.of(
                AndCondition.of(
                        new FunctionCondition("date", "created_at", Operator.EQ, "2024-01-15"),
                        Comparison.eq("status", "PAID")),
                List.of(
                        IndexDefinition.of("idx_created", "created_at"),
                        IndexDefinition.of("idx_note", "note")));

        RewriteResult result = engine.rewrite(request);
        RewriteStatistics stats = result.statistics();

        assertThat(stats.ruleHits())
                .containsEntry("函数条件转范围条件", 1);
        assertThat(stats.candidateIndexCount()).isEqualTo(1);
        assertThat(stats.excludedIndexes())
                .containsKey("idx_note")
                .containsValue(stats.excludedIndexes().get("idx_note"));
        assertThat(stats.excludedIndexes().get("idx_note")).contains("note");
        assertThat(stats.toString())
                .contains("规则命中次数")
                .contains("可选索引数")
                .contains("被排除索引");
    }
}
