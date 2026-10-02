package com.example.gsb.indexhint;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EquivalenceChecker {

    public record EquivalenceResult(boolean equivalent, int originalCount, int rewrittenCount, String detail) {

        public String summary() {
            if (equivalent) {
                return "通过（原始 " + originalCount + " 行，重写后 " + rewrittenCount + " 行一致）";
            }
            return "不一致：原始 " + originalCount + " 行，重写后 " + rewrittenCount + " 行；" + detail;
        }
    }

    private EquivalenceChecker() {
    }

    public static EquivalenceResult verify(QueryLike original, QueryLike rewritten,
                                           List<Map<String, Object>> rows) {
        List<Map<String, Object>> originalRows = RowExecutor.execute(original, rows);
        List<Map<String, Object>> rewrittenRows = RowExecutor.execute(rewritten, rows);
        Set<Map<String, Object>> originalSet = new HashSet<>(originalRows);
        Set<Map<String, Object>> rewrittenSet = new HashSet<>(rewrittenRows);

        if (originalSet.equals(rewrittenSet)) {
            return new EquivalenceResult(true, originalRows.size(), rewrittenRows.size(), "");
        }
        Set<Map<String, Object>> missing = new HashSet<>(originalSet);
        missing.removeAll(rewrittenSet);
        Set<Map<String, Object>> unexpected = new HashSet<>(rewrittenSet);
        unexpected.removeAll(originalSet);
        String detail = "重写后丢失 " + missing.size() + " 行，多出 " + unexpected.size() + " 行";
        return new EquivalenceResult(false, originalRows.size(), rewrittenRows.size(), detail);
    }
}
