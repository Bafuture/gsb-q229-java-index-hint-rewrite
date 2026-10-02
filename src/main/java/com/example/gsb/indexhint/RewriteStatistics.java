package com.example.gsb.indexhint;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RewriteStatistics {

    public record ExcludedIndex(String indexName, String reason) {
    }

    private final Map<String, Integer> ruleHits = new LinkedHashMap<>();
    private final Set<String> candidateIndexes = new LinkedHashSet<>();
    private final List<ExcludedIndex> excludedIndexes = new java.util.ArrayList<>();

    public void recordRuleHit(String ruleName) {
        ruleHits.merge(ruleName, 1, Integer::sum);
    }

    public void recordSelection(IndexSelection selection) {
        for (IndexEvaluation evaluation : selection.candidates()) {
            candidateIndexes.add(evaluation.index().name());
        }
        for (IndexEvaluation evaluation : selection.excluded()) {
            boolean alreadyRecorded = excludedIndexes.stream()
                    .anyMatch(excluded -> excluded.indexName().equals(evaluation.index().name()));
            if (!alreadyRecorded) {
                excludedIndexes.add(new ExcludedIndex(evaluation.index().name(), evaluation.reason()));
            }
        }
    }

    public Map<String, Integer> ruleHits() {
        return Collections.unmodifiableMap(ruleHits);
    }

    public int candidateIndexCount() {
        return candidateIndexes.size();
    }

    public Set<String> candidateIndexes() {
        return Collections.unmodifiableSet(candidateIndexes);
    }

    public List<ExcludedIndex> excludedIndexes() {
        return List.copyOf(excludedIndexes);
    }
}
