package com.example.gsb.indexhint.exec;

import com.example.gsb.indexhint.model.Condition;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EquivalenceChecker {

    public EquivalenceResult verify(Condition before, Condition after,
                                    List<Map<String, Object>> dataset) {
        Set<Integer> beforeHits = matchRows(before, dataset);
        Set<Integer> afterHits = matchRows(after, dataset);
        List<String> mismatches = new ArrayList<>();
        for (int i = 0; i < dataset.size(); i++) {
            boolean inBefore = beforeHits.contains(i);
            boolean inAfter = afterHits.contains(i);
            if (inBefore != inAfter) {
                mismatches.add("第 " + i + " 行 " + dataset.get(i)
                        + (inBefore ? " 仅被重写前查询命中" : " 仅被重写后查询命中"));
            }
        }
        return new EquivalenceResult(mismatches.isEmpty(), beforeHits.size(),
                afterHits.size(), mismatches);
    }

    private Set<Integer> matchRows(Condition condition, List<Map<String, Object>> dataset) {
        Set<Integer> hits = new LinkedHashSet<>();
        for (int i = 0; i < dataset.size(); i++) {
            if (ConditionEvaluator.matches(condition, dataset.get(i))) {
                hits.add(i);
            }
        }
        return hits;
    }
}
