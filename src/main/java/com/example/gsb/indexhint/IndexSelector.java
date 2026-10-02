package com.example.gsb.indexhint;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.example.gsb.indexhint.TableStats.ColumnStats;

public final class IndexSelector {

    private static final double DEFAULT_EQ_SELECTIVITY = 0.1;
    private static final double DEFAULT_RANGE_SELECTIVITY = 0.25;
    private static final double LIKE_PREFIX_SELECTIVITY = 0.1;

    public IndexSelection select(Query query, List<Index> indexes, TableStats stats) {
        List<IndexEvaluation> evaluations = indexes.stream()
                .map(index -> evaluate(query, index, stats))
                .toList();
        IndexEvaluation best = evaluations.stream()
                .filter(IndexEvaluation::usable)
                .min(Comparator.comparingLong(IndexEvaluation::estimatedRows)
                        .thenComparing(Comparator.comparingInt(IndexEvaluation::matchedColumns).reversed())
                        .thenComparing(evaluation -> evaluation.index().name()))
                .orElse(null);
        return new IndexSelection(best, evaluations);
    }

    public IndexEvaluation evaluate(Query query, Index index, TableStats stats) {
        List<FieldCondition> fieldConditions = query.conditions().stream()
                .filter(FieldCondition.class::isInstance)
                .map(FieldCondition.class::cast)
                .toList();

        double filterRatio = 1.0;
        int matched = 0;
        for (String column : index.columns()) {
            FieldCondition condition = findUsableCondition(fieldConditions, column);
            if (condition == null) {
                break;
            }
            matched++;
            filterRatio *= selectivity(condition, stats);
            if (!condition.isEquality()) {
                break;
            }
        }

        if (matched == 0) {
            return new IndexEvaluation(index, 0, 1.0, stats.rowCount(), false,
                    "首列 '" + index.firstColumn() + "' 缺少可用的等值或范围条件");
        }
        long estimatedRows = Math.max(1, Math.round(filterRatio * stats.rowCount()));
        return new IndexEvaluation(index, matched, filterRatio, estimatedRows, true,
                "覆盖前 " + matched + " 列" + index.columns().subList(0, matched)
                        + "，估算过滤比例 " + String.format("%.4f", filterRatio)
                        + "，扫描约 " + estimatedRows + " 行");
    }

    private FieldCondition findUsableCondition(List<FieldCondition> conditions, String column) {
        FieldCondition rangeCondition = null;
        for (FieldCondition condition : conditions) {
            if (!condition.field().equals(column) || !condition.isIndexUsable()) {
                continue;
            }
            if (condition.isEquality()) {
                return condition;
            }
            if (rangeCondition == null) {
                rangeCondition = condition;
            }
        }
        return rangeCondition;
    }

    static double selectivity(FieldCondition condition, TableStats stats) {
        ColumnStats columnStats = stats.column(condition.field());
        return switch (condition.operator()) {
            case EQ -> columnStats != null
                    ? 1.0 / Math.max(1, columnStats.distinctCount())
                    : DEFAULT_EQ_SELECTIVITY;
            case NE -> 0.9;
            case IN -> inSelectivity(condition, columnStats);
            case GT, GE, LT, LE -> rangeSelectivity(condition, columnStats);
            case LIKE -> LIKE_PREFIX_SELECTIVITY;
        };
    }

    private static double inSelectivity(FieldCondition condition, ColumnStats columnStats) {
        int size = (condition.value() instanceof Collection<?> collection) ? collection.size() : 1;
        long distinct = columnStats != null ? Math.max(1, columnStats.distinctCount()) : 10;
        return Math.min(1.0, (double) size / distinct);
    }

    private static double rangeSelectivity(FieldCondition condition, ColumnStats columnStats) {
        if (columnStats != null) {
            Double min = Values.asNumber(columnStats.min());
            Double max = Values.asNumber(columnStats.max());
            Double value = Values.asNumber(condition.value());
            if (min != null && max != null && value != null && max > min) {
                double fraction = switch (condition.operator()) {
                    case GT, GE -> (max - value) / (max - min);
                    default -> (value - min) / (max - min);
                };
                return Math.min(1.0, Math.max(0.001, fraction));
            }
        }
        return DEFAULT_RANGE_SELECTIVITY;
    }
}
