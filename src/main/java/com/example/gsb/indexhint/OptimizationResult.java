package com.example.gsb.indexhint;

import java.util.List;
import java.util.Map;

import com.example.gsb.indexhint.EquivalenceChecker.EquivalenceResult;

public final class OptimizationResult {

    private final QueryLike originalQuery;
    private final QueryLike rewrittenQuery;
    private final List<IndexSelection> selections;
    private final RewriteStatistics statistics;
    private final EquivalenceResult equivalence;
    private final String forcedIndex;

    public OptimizationResult(QueryLike originalQuery, QueryLike rewrittenQuery,
                              List<IndexSelection> selections, RewriteStatistics statistics,
                              EquivalenceResult equivalence, String forcedIndex) {
        this.originalQuery = originalQuery;
        this.rewrittenQuery = rewrittenQuery;
        this.selections = List.copyOf(selections);
        this.statistics = statistics;
        this.equivalence = equivalence;
        this.forcedIndex = forcedIndex;
    }

    public QueryLike originalQuery() {
        return originalQuery;
    }

    public QueryLike rewrittenQuery() {
        return rewrittenQuery;
    }

    public List<IndexSelection> selections() {
        return selections;
    }

    public RewriteStatistics statistics() {
        return statistics;
    }

    public EquivalenceResult equivalence() {
        return equivalence;
    }

    public String forcedIndex() {
        return forcedIndex;
    }

    public IndexEvaluation chosenIndex() {
        return selections.get(0).chosen();
    }

    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("==== 查询重写与索引选择报告 ====\n");
        sb.append("原始条件: ").append(originalQuery.describe()).append('\n');
        sb.append("重写后条件: ").append(rewrittenQuery.describe()).append('\n');

        boolean multipleBranches = selections.size() > 1;
        for (int i = 0; i < selections.size(); i++) {
            IndexSelection selection = selections.get(i);
            IndexEvaluation chosen = selection.chosen();
            String prefix = multipleBranches ? "选中索引(分支" + (i + 1) + "): " : "选中索引: ";
            if (chosen == null) {
                sb.append(prefix).append("无（全部索引均被排除，将全表扫描）\n");
            } else {
                sb.append(prefix)
                        .append(chosen.index().name())
                        .append("，原因: ")
                        .append(chosen.reason())
                        .append("，在 ")
                        .append(selection.candidates().size())
                        .append(" 个候选索引中估算代价最低")
                        .append(forcedIndex != null ? "（调用方强制提示）" : "")
                        .append('\n');
            }
        }

        sb.append("规则命中: ");
        if (statistics.ruleHits().isEmpty()) {
            sb.append("{}");
        } else {
            sb.append(statistics.ruleHits());
        }
        sb.append('\n');
        sb.append("可选索引数: ").append(statistics.candidateIndexCount());
        if (!statistics.candidateIndexes().isEmpty()) {
            sb.append(' ').append(statistics.candidateIndexes());
        }
        sb.append('\n');
        for (RewriteStatistics.ExcludedIndex excluded : statistics.excludedIndexes()) {
            sb.append("被排除索引: ").append(excluded.indexName())
                    .append("，原因: ").append(excluded.reason()).append('\n');
        }
        if (forcedIndex != null) {
            sb.append("强制索引提示: ").append(forcedIndex).append('\n');
        }
        if (equivalence != null) {
            sb.append("等价性验证: ").append(equivalence.summary()).append('\n');
        }
        return sb.toString();
    }

    public Map<String, Object> asMap() {
        return Map.of(
                "original", originalQuery.describe(),
                "rewritten", rewrittenQuery.describe(),
                "chosenIndex", chosenIndex() == null ? null : chosenIndex().index().name());
    }
}
