package com.example.gsb.indexhint;

public record IndexEvaluation(
        Index index,
        int matchedColumns,
        double filterRatio,
        long estimatedRows,
        boolean usable,
        String reason) {
}
