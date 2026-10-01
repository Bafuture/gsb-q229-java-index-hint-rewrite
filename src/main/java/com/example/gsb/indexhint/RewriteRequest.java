package com.example.gsb.indexhint;

import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.model.IndexDefinition;
import java.util.List;

public record RewriteRequest(Condition condition, List<IndexDefinition> indexes,
                             String forcedIndex) {

    public RewriteRequest {
        indexes = List.copyOf(indexes);
    }

    public static RewriteRequest of(Condition condition, List<IndexDefinition> indexes) {
        return new RewriteRequest(condition, indexes, null);
    }

    public RewriteRequest forceIndex(String indexName) {
        return new RewriteRequest(condition, indexes, indexName);
    }
}
