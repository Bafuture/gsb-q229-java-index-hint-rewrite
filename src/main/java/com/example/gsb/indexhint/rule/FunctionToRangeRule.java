package com.example.gsb.indexhint.rule;

import com.example.gsb.indexhint.RewriteStatistics;
import com.example.gsb.indexhint.model.AndCondition;
import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.model.FunctionCondition;
import com.example.gsb.indexhint.model.IndexDefinition;
import com.example.gsb.indexhint.model.Operator;
import com.example.gsb.indexhint.model.OrCondition;
import com.example.gsb.indexhint.model.UnionCondition;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FunctionToRangeRule implements RewriteRule {

    public static final String NAME = "函数条件转范围条件";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Condition apply(Condition condition, List<IndexDefinition> indexes,
                           RewriteStatistics statistics) {
        return transform(condition, statistics);
    }

    private Condition transform(Condition condition, RewriteStatistics statistics) {
        if (condition instanceof FunctionCondition fn) {
            Condition rewritten = tryRewrite(fn);
            if (rewritten != null) {
                statistics.recordRuleHit(name());
                return rewritten;
            }
            return condition;
        }
        if (condition instanceof AndCondition and) {
            List<Condition> flattened = new ArrayList<>();
            for (Condition child : transformAll(and.conditions(), statistics)) {
                if (child instanceof AndCondition nested) {
                    flattened.addAll(nested.conditions());
                } else {
                    flattened.add(child);
                }
            }
            return new AndCondition(flattened);
        }
        if (condition instanceof OrCondition or) {
            return new OrCondition(transformAll(or.conditions(), statistics));
        }
        if (condition instanceof UnionCondition union) {
            return new UnionCondition(transformAll(union.branches(), statistics));
        }
        return condition;
    }

    private List<Condition> transformAll(List<Condition> conditions, RewriteStatistics statistics) {
        List<Condition> result = new ArrayList<>();
        for (Condition c : conditions) {
            result.add(transform(c, statistics));
        }
        return result;
    }

    private Condition tryRewrite(FunctionCondition fn) {
        if (fn.operator() != Operator.EQ) {
            return null;
        }
        return switch (fn.function().toUpperCase()) {
            case "DATE" -> dateToRange(fn);
            case "YEAR" -> yearToRange(fn);
            default -> null;
        };
    }

    private Condition dateToRange(FunctionCondition fn) {
        LocalDate day;
        try {
            day = LocalDate.parse(fn.value().toString());
        } catch (RuntimeException e) {
            return null;
        }
        return AndCondition.of(
                new Comparison(fn.field(), Operator.GE, day.toString()),
                new Comparison(fn.field(), Operator.LT, day.plusDays(1).toString()));
    }

    private Condition yearToRange(FunctionCondition fn) {
        int year;
        try {
            year = Integer.parseInt(fn.value().toString());
        } catch (RuntimeException e) {
            return null;
        }
        return AndCondition.of(
                new Comparison(fn.field(), Operator.GE, String.valueOf(year)),
                new Comparison(fn.field(), Operator.LT, String.valueOf(year + 1)));
    }
}
