package com.example.gsb.indexhint.exec;

import java.util.List;

public record EquivalenceResult(boolean equivalent, int beforeCount, int afterCount,
                                List<String> mismatches) {

    public EquivalenceResult {
        mismatches = List.copyOf(mismatches);
    }
}
