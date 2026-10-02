package com.example.gsb.indexhint;

import java.util.ArrayList;
import java.util.List;

public interface RewriteRule {

    String name();

    RuleApplication applyOnQuery(Query query, List<Index> indexes);

    default RuleApplication apply(QueryLike query, List<Index> indexes) {
        if (query instanceof Query single) {
            return applyOnQuery(single, indexes);
        }
        UnionQuery union = (UnionQuery) query;
        List<Query> branches = new ArrayList<>();
        boolean anyApplied = false;
        List<String> details = new ArrayList<>();
        for (Query branch : union.branches()) {
            RuleApplication result = applyOnQuery(branch, indexes);
            if (result.query() instanceof Query rewritten) {
                branches.add(rewritten);
            } else {
                branches.addAll(((UnionQuery) result.query()).branches());
            }
            if (result.applied()) {
                anyApplied = true;
                details.add(result.detail());
            }
        }
        if (!anyApplied) {
            return RuleApplication.unchanged(query);
        }
        return RuleApplication.applied(new UnionQuery(branches), String.join("; ", details));
    }
}
