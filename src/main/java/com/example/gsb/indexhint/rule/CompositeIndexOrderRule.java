package com.example.gsb.indexhint.rule;

import com.example.gsb.indexhint.RewriteStatistics;
import com.example.gsb.indexhint.model.AndCondition;
import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.model.IndexDefinition;
import com.example.gsb.indexhint.model.OrCondition;
import com.example.gsb.indexhint.model.UnionCondition;
import com.example.gsb.indexhint.select.IndexUsageAnalyzer;
import com.example.gsb.indexhint.select.PrefixMatch;
import java.util.ArrayList;
import java.util.List;

public class CompositeIndexOrderRule implements RewriteRule {

    public static final String NAME = "条件顺序调整以匹配组合索引";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Condition apply(Condition condition, List<IndexDefinition> indexes,
                           RewriteStatistics statistics) {
        return transform(condition, indexes, statistics);
    }

    private Condition transform(Condition condition, List<IndexDefinition> indexes,
                                RewriteStatistics statistics) {
        if (condition instanceof AndCondition and) {
            List<Condition> children = transformAll(and.conditions(), indexes, statistics);
            return reorder(children, indexes, statistics);
        }
        if (condition instanceof OrCondition or) {
            return new OrCondition(transformAll(or.conditions(), indexes, statistics));
        }
        if (condition instanceof UnionCondition union) {
            return new UnionCondition(transformAll(union.branches(), indexes, statistics));
        }
        return condition;
    }

    private List<Condition> transformAll(List<Condition> conditions, List<IndexDefinition> indexes,
                                         RewriteStatistics statistics) {
        List<Condition> result = new ArrayList<>();
        for (Condition c : conditions) {
            result.add(transform(c, indexes, statistics));
        }
        return result;
    }

    private Condition reorder(List<Condition> children, List<IndexDefinition> indexes,
                              RewriteStatistics statistics) {
        List<Comparison> comparisons = children.stream()
                .filter(Comparison.class::isInstance)
                .map(Comparison.class::cast)
                .toList();
        if (comparisons.size() < 2 || indexes.isEmpty()) {
            return new AndCondition(children);
        }

        PrefixMatch best = null;
        double bestRatio = Double.MAX_VALUE;
        for (IndexDefinition index : indexes) {
            PrefixMatch match = IndexUsageAnalyzer.analyze(index, comparisons);
            if (!match.usable()) {
                continue;
            }
            double ratio = IndexUsageAnalyzer.estimateFilterRatio(index, match, comparisons);
            if (best == null
                    || match.prefixColumns().size() > best.prefixColumns().size()
                    || (match.prefixColumns().size() == best.prefixColumns().size()
                            && ratio < bestRatio)) {
                best = match;
                bestRatio = ratio;
            }
        }
        if (best == null || best.prefixColumns().size() < 2) {
            return new AndCondition(children);
        }

        List<Condition> ordered = new ArrayList<>();
        for (String column : best.prefixColumns()) {
            children.stream()
                    .filter(c -> c instanceof Comparison cmp
                            && cmp.field().equals(column) && cmp.isEquality())
                    .forEach(ordered::add);
            children.stream()
                    .filter(c -> c instanceof Comparison cmp
                            && cmp.field().equals(column) && !cmp.isEquality())
                    .forEach(ordered::add);
        }
        for (Condition c : children) {
            if (!ordered.contains(c)) {
                ordered.add(c);
            }
        }
        if (ordered.equals(children)) {
            return new AndCondition(children);
        }
        statistics.recordRuleHit(name());
        return new AndCondition(ordered);
    }
}
