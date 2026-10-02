package com.example.gsb.indexhint;

import java.util.ArrayList;
import java.util.List;

/**
 * 把 OR 条件拆分为 UNION ALL 分支，使每个分支都能分别命中索引。
 * 判定方式：对每个 OR 分支字段，必须存在一个索引，使该字段之前的
 * 所有前缀列在其他 AND 条件中都有可用条件（该字段本身由分支条件提供）。
 */
public final class OrToUnionRule implements RewriteRule {

    public static final String NAME = "OR_TO_UNION";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public RuleApplication applyOnQuery(Query query, List<Index> indexes) {
        List<Condition> conditions = query.conditions();
        for (int i = 0; i < conditions.size(); i++) {
            Condition condition = conditions.get(i);
            if (!(condition instanceof OrCondition or)) {
                continue;
            }
            List<Condition> siblings = without(conditions, i);
            if (allBranchesIndexable(or, siblings, indexes)) {
                List<Query> branches = new ArrayList<>();
                for (Condition branch : or.branches()) {
                    List<Condition> branchConditions = new ArrayList<>(siblings);
                    branchConditions.add(branch);
                    branches.add(query.withConditions(branchConditions));
                }
                return RuleApplication.applied(new UnionQuery(branches),
                        "OR 条件拆分为 " + branches.size() + " 个 UNION ALL 分支，各分支可分别使用索引");
            }
        }
        return RuleApplication.unchanged(query);
    }

    private boolean allBranchesIndexable(OrCondition or, List<Condition> siblings,
                                         List<Index> indexes) {
        for (Condition branch : or.branches()) {
            if (!(branch instanceof FieldCondition fieldCondition) || !fieldCondition.isIndexUsable()) {
                return false;
            }
            if (!hasSupportingIndex(fieldCondition, siblings, indexes)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSupportingIndex(FieldCondition branchCondition, List<Condition> siblings,
                                       List<Index> indexes) {
        for (Index index : indexes) {
            int position = index.columns().indexOf(branchCondition.field());
            if (position < 0) {
                continue;
            }
            if (prefixCovered(index.columns().subList(0, position), siblings)) {
                return true;
            }
        }
        return false;
    }

    private boolean prefixCovered(List<String> prefixColumns, List<Condition> siblings) {
        for (String column : prefixColumns) {
            boolean covered = siblings.stream()
                    .anyMatch(condition -> condition instanceof FieldCondition fieldCondition
                            && fieldCondition.field().equals(column)
                            && fieldCondition.isIndexUsable());
            if (!covered) {
                return false;
            }
        }
        return true;
    }

    private List<Condition> without(List<Condition> conditions, int index) {
        List<Condition> result = new ArrayList<>(conditions);
        result.remove(index);
        return result;
    }
}
