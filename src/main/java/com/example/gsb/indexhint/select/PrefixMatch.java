package com.example.gsb.indexhint.select;

import com.example.gsb.indexhint.model.IndexDefinition;
import java.util.List;

public record PrefixMatch(IndexDefinition index, List<String> prefixColumns) {

    public PrefixMatch {
        prefixColumns = List.copyOf(prefixColumns);
    }

    public boolean usable() {
        return !prefixColumns.isEmpty();
    }
}
