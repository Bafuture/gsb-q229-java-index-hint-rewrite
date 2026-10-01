package com.example.gsb.indexhint.rule;

import com.example.gsb.indexhint.RewriteStatistics;
import com.example.gsb.indexhint.model.AndCondition;
import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.model.Conditions;
import com.example.gsb.indexhint.model.IndexDefinition;
import com.example.gsb.indexhint.model.OrCondition;
import com.example.gsb.indexhint.model.UnionCondition;
import com.example.gsb.indexhint.select.IndexUsageAnalyzer;
import java.util.ArrayList;
import java.util.List;

public class OrToUnionRule implements RewriteRule {

    public static final String NAME = "OR 条件转 UNION";

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
            return new AndCondition(transformAll(and.conditions(), indexes, statistics));
        }
        if (condition instanceof UnionCondition union) {
            return new UnionCondition(transformAll(union.branches(), indexes, statistics));
        }
        if (condition instanceof OrCondition or) {
            List<Condition> branches = transformAll(or.conditions(), indexes, statistics);
            boolean allIndexable = branches.stream().allMatch(b -> canUseIndex(b, indexes));
            if (allIndexable) {
                statistics.recordRuleHit(name());
                return new UnionCondition(branches);
            }
            return new OrCondition(branches);
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

    private boolean canUseIndex(Condition branch, List<IndexDefinition> indexes) {
        List<Comparison> comparisons = Conditions.flattenComparisons(branch);
        if (comparisons.isEmpty()) {
            return false;
        }
        return indexes.stream()
                .anyMatch(idx -> IndexUsageAnalyzer.analyze(idx, comparisons).usable());
    }
}
