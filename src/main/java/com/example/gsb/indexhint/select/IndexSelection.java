package com.example.gsb.indexhint.select;

import com.example.gsb.indexhint.model.IndexDefinition;
import java.util.List;

public record IndexSelection(String branchLabel, IndexDefinition chosen, String reason,
                             List<IndexCandidate> candidates, List<IndexCandidate> excluded) {

    public IndexSelection {
        candidates = List.copyOf(candidates);
        excluded = List.copyOf(excluded);
    }
}
