package com.example.gsb.indexhint.select;

import com.example.gsb.indexhint.IndexHintException;
import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.IndexDefinition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class IndexSelector {

    static final double ROW_BASE = 1000.0d;
    static final double RESIDUAL_PENALTY = 5.0d;

    public IndexSelection select(String branchLabel, List<Comparison> conditions,
                                 List<IndexDefinition> indexes, String forcedIndex) {
        List<IndexCandidate> usable = new ArrayList<>();
        List<IndexCandidate> excluded = new ArrayList<>();
        for (IndexDefinition index : indexes) {
            PrefixMatch match = IndexUsageAnalyzer.analyze(index, conditions);
            if (!match.usable()) {
                excluded.add(new IndexCandidate(index, false,
                        "索引首列 '" + index.columns().get(0) + "' 上没有可用条件，无法作为访问路径",
                        1.0d, Double.MAX_VALUE));
                continue;
            }
            double ratio = IndexUsageAnalyzer.estimateFilterRatio(index, match, conditions);
            long residual = conditions.stream()
                    .filter(c -> !match.prefixColumns().contains(c.field()))
                    .count();
            double cost = ratio * ROW_BASE + residual * RESIDUAL_PENALTY;
            usable.add(new IndexCandidate(index, true,
                    "命中前缀列 " + match.prefixColumns(), ratio, cost));
        }

        if (forcedIndex != null) {
            return selectForced(branchLabel, forcedIndex, indexes, usable, excluded);
        }

        if (usable.isEmpty()) {
            return new IndexSelection(branchLabel, null,
                    "没有任何索引能覆盖查询条件，只能全表扫描", usable, excluded);
        }
        IndexCandidate best = usable.stream()
                .min(Comparator.comparingDouble(IndexCandidate::cost))
                .orElseThrow();
        String reason = best.reason()
                + "，预估过滤比例 " + formatRatio(best.filterRatio())
                + "，估算代价 " + best.cost()
                + "，为 " + usable.size() + " 个候选索引中最低";
        return new IndexSelection(branchLabel, best.index(), reason, usable, excluded);
    }

    private IndexSelection selectForced(String branchLabel, String forcedIndex,
                                        List<IndexDefinition> indexes,
                                        List<IndexCandidate> usable,
                                        List<IndexCandidate> excluded) {
        boolean exists = indexes.stream().anyMatch(i -> i.name().equals(forcedIndex));
        if (!exists) {
            throw new IndexHintException(
                    "强制索引 '" + forcedIndex + "' 不存在于可用索引清单中");
        }
        IndexCandidate forced = usable.stream()
                .filter(c -> c.index().name().equals(forcedIndex))
                .findFirst()
                .orElseThrow(() -> new IndexHintException(
                        "强制索引 '" + forcedIndex + "' 无法覆盖查询条件："
                                + excludedReason(excluded, forcedIndex)));
        String reason = "调用方强制指定使用该索引；" + forced.reason()
                + "，预估过滤比例 " + formatRatio(forced.filterRatio());
        return new IndexSelection(branchLabel, forced.index(), reason, usable, excluded);
    }

    private static String excludedReason(List<IndexCandidate> excluded, String name) {
        return excluded.stream()
                .filter(c -> c.index().name().equals(name))
                .map(IndexCandidate::reason)
                .findFirst()
                .orElse("未知原因");
    }

    private static String formatRatio(double ratio) {
        return String.format(Locale.ROOT, "%.3f", ratio);
    }
}
