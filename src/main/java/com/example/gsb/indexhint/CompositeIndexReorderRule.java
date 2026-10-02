package com.example.gsb.indexhint;

import java.util.ArrayList;
import java.util.List;

/**
 * 调整 AND 条件的先后顺序，使组合索引前缀列上的条件排在最前，
 * 便于优化器按前缀命中组合索引。
 */
public final class CompositeIndexReorderRule implements RewriteRule {

    public static final String NAME = "COMPOSITE_INDEX_REORDER";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public RuleApplication applyOnQuery(Query query, List<Index> indexes) {
        Index target = null;
        List<String> targetPrefix = List.of();
        for (Index index : indexes) {
            if (!index.isComposite()) {
                continue;
            }
            List<String> prefix = matchedPrefix(index, query.conditions());
            if (prefix.size() >= 2 && prefix.size() > targetPrefix.size()) {
                target = index;
                targetPrefix = prefix;
            }
        }
        if (target == null) {
            return RuleApplication.unchanged(query);
        }

        List<Condition> reordered = new ArrayList<>();
        for (String column : targetPrefix) {
            for (Condition condition : query.conditions()) {
                if (condition instanceof FieldCondition fieldCondition
                        && fieldCondition.field().equals(column)
                        && !reordered.contains(condition)) {
                    reordered.add(condition);
                }
            }
        }
        for (Condition condition : query.conditions()) {
            if (!reordered.contains(condition)) {
                reordered.add(condition);
            }
        }
        if (reordered.equals(query.conditions())) {
            return RuleApplication.unchanged(query);
        }
        return RuleApplication.applied(query.withConditions(reordered),
                "按组合索引 " + target.name() + target.columns()
                        + " 调整条件顺序，前置列: " + targetPrefix);
    }

    private List<String> matchedPrefix(Index index, List<Condition> conditions) {
        List<String> prefix = new ArrayList<>();
        for (String column : index.columns()) {
            boolean found = false;
            for (Condition condition : conditions) {
                if (condition instanceof FieldCondition fieldCondition
                        && fieldCondition.field().equals(column)
                        && fieldCondition.isIndexUsable()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                break;
            }
            prefix.add(column);
        }
        return prefix;
    }
}
