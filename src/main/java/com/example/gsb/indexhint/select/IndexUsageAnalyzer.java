package com.example.gsb.indexhint.select;

import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.IndexDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class IndexUsageAnalyzer {

    private IndexUsageAnalyzer() {
    }

    public static PrefixMatch analyze(IndexDefinition index, List<Comparison> conditions) {
        Map<String, List<Comparison>> byField = conditions.stream()
                .collect(Collectors.groupingBy(Comparison::field));
        List<String> prefix = new ArrayList<>();
        for (String column : index.columns()) {
            List<Comparison> onColumn = byField.getOrDefault(column, List.of());
            boolean hasEquality = onColumn.stream().anyMatch(Comparison::isEquality);
            boolean hasRange = onColumn.stream().anyMatch(Comparison::isRange);
            if (hasEquality) {
                prefix.add(column);
            } else if (hasRange) {
                prefix.add(column);
                break;
            } else {
                break;
            }
        }
        return new PrefixMatch(index, prefix);
    }

    public static double estimateFilterRatio(IndexDefinition index, PrefixMatch match,
                                             List<Comparison> conditions) {
        Map<String, List<Comparison>> byField = conditions.stream()
                .collect(Collectors.groupingBy(Comparison::field));
        double ratio = 1.0d;
        for (String column : match.prefixColumns()) {
            List<Comparison> onColumn = byField.getOrDefault(column, List.of());
            boolean hasEquality = onColumn.stream().anyMatch(Comparison::isEquality);
            ratio *= hasEquality ? index.selectivityOf(column) : index.rangeSelectivity();
        }
        return ratio;
    }
}
