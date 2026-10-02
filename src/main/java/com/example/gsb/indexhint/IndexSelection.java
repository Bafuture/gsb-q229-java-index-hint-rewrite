package com.example.gsb.indexhint;

import java.util.List;

public record IndexSelection(IndexEvaluation chosen, List<IndexEvaluation> evaluations) {

    public List<IndexEvaluation> candidates() {
        return evaluations.stream()
                .filter(IndexEvaluation::usable)
                .toList();
    }

    public List<IndexEvaluation> excluded() {
        return evaluations.stream()
                .filter(evaluation -> !evaluation.usable())
                .toList();
    }
}
