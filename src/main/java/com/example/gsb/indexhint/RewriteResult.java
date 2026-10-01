package com.example.gsb.indexhint;

import com.example.gsb.indexhint.exec.ConditionEvaluator;
import com.example.gsb.indexhint.exec.EquivalenceChecker;
import com.example.gsb.indexhint.exec.EquivalenceResult;
import com.example.gsb.indexhint.model.Condition;
import java.util.List;
import java.util.Map;

public record RewriteResult(Condition original, Condition rewritten, RewriteReport report,
                            RewriteStatistics statistics,
                            EquivalenceChecker equivalenceChecker) {

    public List<Map<String, Object>> execute(List<Map<String, Object>> dataset) {
        return dataset.stream()
                .filter(row -> ConditionEvaluator.matches(rewritten, row))
                .toList();
    }

    public EquivalenceResult verifyEquivalence(List<Map<String, Object>> dataset) {
        return equivalenceChecker.verify(original, rewritten, dataset);
    }
}
