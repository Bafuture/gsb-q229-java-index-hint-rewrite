package com.example.gsb.indexhint;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.gsb.indexhint.EquivalenceChecker.EquivalenceResult;

public final class QueryOptimizer {

    private final List<RewriteRule> rules;

    public QueryOptimizer() {
        this(List.of(
                new FunctionToRangeRule(),
                new OrToUnionRule(),
                new CompositeIndexReorderRule()));
    }

    public QueryOptimizer(List<RewriteRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public OptimizationResult optimize(Query query, List<Index> indexes, TableStats stats) {
        return optimize(query, indexes, stats, null, null);
    }

    public OptimizationResult optimize(Query query, List<Index> indexes, TableStats stats,
                                       String forcedIndex) {
        return optimize(query, indexes, stats, forcedIndex, null);
    }

    public OptimizationResult optimizeWithSample(Query query, List<Index> indexes, TableStats stats,
                                                 String forcedIndex,
                                                 List<Map<String, Object>> sampleRows) {
        return optimize(query, indexes, stats, forcedIndex, sampleRows);
    }

    public OptimizationResult optimize(Query query, List<Index> indexes, TableStats stats,
                                       String forcedIndex, List<Map<String, Object>> sampleRows) {
        RewriteStatistics statistics = new RewriteStatistics();
        QueryLike rewritten = query;
        for (RewriteRule rule : rules) {
            RuleApplication application = rule.apply(rewritten, indexes);
            if (application.applied()) {
                statistics.recordRuleHit(rule.name());
            }
            rewritten = application.query();
        }

        List<Query> branches = rewritten instanceof Query single
                ? List.of(single)
                : ((UnionQuery) rewritten).branches();

        IndexSelector selector = new IndexSelector();
        List<IndexSelection> selections = branches.stream()
                .map(branch -> selectForBranch(branch, indexes, stats, forcedIndex, selector))
                .toList();
        selections.forEach(statistics::recordSelection);

        EquivalenceResult equivalence = sampleRows == null
                ? null
                : EquivalenceChecker.verify(query, rewritten, sampleRows);

        return new OptimizationResult(query, rewritten, selections, statistics, equivalence, forcedIndex);
    }

    private IndexSelection selectForBranch(Query branch, List<Index> indexes, TableStats stats,
                                           String forcedIndex, IndexSelector selector) {
        if (forcedIndex == null) {
            return selector.select(branch, indexes, stats);
        }
        boolean exists = indexes.stream().anyMatch(index -> index.name().equals(forcedIndex));
        if (!exists) {
            String available = indexes.stream().map(Index::name).collect(Collectors.joining(", "));
            throw new IndexHintException("强制索引 '" + forcedIndex + "' 不存在于可用索引清单中"
                    + (available.isEmpty() ? "" : "，可用索引: " + available));
        }
        List<IndexEvaluation> evaluations = indexes.stream()
                .map(index -> selector.evaluate(branch, index, stats))
                .toList();
        IndexEvaluation forced = evaluations.stream()
                .filter(evaluation -> evaluation.index().name().equals(forcedIndex))
                .findFirst()
                .orElseThrow();
        if (!forced.usable()) {
            throw new IndexHintException("强制索引 '" + forcedIndex + "' 无法覆盖查询条件: "
                    + forced.reason() + "；查询条件为 [" + branch.describe() + "]");
        }
        return new IndexSelection(forced, evaluations);
    }
}
