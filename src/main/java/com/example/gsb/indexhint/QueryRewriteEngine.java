package com.example.gsb.indexhint;

import com.example.gsb.indexhint.exec.EquivalenceChecker;
import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.model.Conditions;
import com.example.gsb.indexhint.model.UnionCondition;
import com.example.gsb.indexhint.rule.CompositeIndexOrderRule;
import com.example.gsb.indexhint.rule.FunctionToRangeRule;
import com.example.gsb.indexhint.rule.OrToUnionRule;
import com.example.gsb.indexhint.rule.RewriteRule;
import com.example.gsb.indexhint.select.IndexSelection;
import com.example.gsb.indexhint.select.IndexSelector;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class QueryRewriteEngine {

    private final List<RewriteRule> rules;
    private final IndexSelector selector = new IndexSelector();
    private final EquivalenceChecker equivalenceChecker = new EquivalenceChecker();

    public QueryRewriteEngine() {
        this(List.of(new FunctionToRangeRule(), new OrToUnionRule(),
                new CompositeIndexOrderRule()));
    }

    public QueryRewriteEngine(List<RewriteRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public RewriteResult rewrite(RewriteRequest request) {
        RewriteStatistics statistics = new RewriteStatistics();
        Condition original = request.condition();
        Condition rewritten = original;
        for (RewriteRule rule : rules) {
            rewritten = rule.apply(rewritten, request.indexes(), statistics);
        }

        List<IndexSelection> selections = new ArrayList<>();
        if (rewritten instanceof UnionCondition union) {
            int branchNo = 1;
            for (Condition branch : union.branches()) {
                selections.add(selector.select("UNION 分支 " + branchNo++,
                        Conditions.flattenComparisons(branch),
                        request.indexes(), request.forcedIndex()));
            }
        } else {
            selections.add(selector.select("主查询",
                    Conditions.flattenComparisons(rewritten),
                    request.indexes(), request.forcedIndex()));
        }

        Set<String> candidateNames = new LinkedHashSet<>();
        for (IndexSelection selection : selections) {
            selection.candidates().forEach(c -> candidateNames.add(c.index().name()));
            selection.excluded().forEach(
                    c -> statistics.recordExcludedIndex(c.index().name(), c.reason()));
        }
        statistics.candidateIndexCount(candidateNames.size());

        RewriteReport report = new RewriteReport(original, rewritten, selections,
                request.forcedIndex());
        return new RewriteResult(original, rewritten, report, statistics, equivalenceChecker);
    }
}
