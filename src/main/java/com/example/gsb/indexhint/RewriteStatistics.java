package com.example.gsb.indexhint;

import java.util.LinkedHashMap;
import java.util.Map;

public class RewriteStatistics {

    private final Map<String, Integer> ruleHits = new LinkedHashMap<>();
    private int candidateIndexCount;
    private final Map<String, String> excludedIndexes = new LinkedHashMap<>();

    public void recordRuleHit(String ruleName) {
        ruleHits.merge(ruleName, 1, Integer::sum);
    }

    public Map<String, Integer> ruleHits() {
        return ruleHits;
    }

    public int ruleHitCount(String ruleName) {
        return ruleHits.getOrDefault(ruleName, 0);
    }

    public int candidateIndexCount() {
        return candidateIndexCount;
    }

    public void candidateIndexCount(int count) {
        this.candidateIndexCount = count;
    }

    public Map<String, String> excludedIndexes() {
        return excludedIndexes;
    }

    public void recordExcludedIndex(String indexName, String reason) {
        excludedIndexes.putIfAbsent(indexName, reason);
    }

    @Override
    public String toString() {
        return "规则命中次数: " + ruleHits
                + ", 可选索引数: " + candidateIndexCount
                + ", 被排除索引: " + excludedIndexes;
    }
}
