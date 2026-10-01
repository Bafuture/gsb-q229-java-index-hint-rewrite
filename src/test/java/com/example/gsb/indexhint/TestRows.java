package com.example.gsb.indexhint;

import java.util.LinkedHashMap;
import java.util.Map;

final class TestRows {

    private TestRows() {
    }

    static Map<String, Object> row(Object... kv) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            row.put((String) kv[i], kv[i + 1]);
        }
        return row;
    }
}
