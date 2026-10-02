package com.example.gsb.indexhint;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RowExecutor {

    private RowExecutor() {
    }

    public static List<Map<String, Object>> execute(QueryLike query, List<Map<String, Object>> rows) {
        if (query instanceof Query single) {
            return executeQuery(single, rows);
        }
        UnionQuery union = (UnionQuery) query;
        Set<Map<String, Object>> seen = new LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Query branch : union.branches()) {
            for (Map<String, Object> row : executeQuery(branch, rows)) {
                if (seen.add(row)) {
                    result.add(row);
                }
            }
        }
        return result;
    }

    private static List<Map<String, Object>> executeQuery(Query query, List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (matchesAll(query, row)) {
                result.add(row);
            }
        }
        return result;
    }

    private static boolean matchesAll(Query query, Map<String, Object> row) {
        for (Condition condition : query.conditions()) {
            if (!condition.test(row)) {
                return false;
            }
        }
        return true;
    }
}
