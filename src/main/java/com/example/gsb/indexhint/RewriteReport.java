package com.example.gsb.indexhint;

import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.select.IndexSelection;
import java.util.List;
import java.util.stream.Collectors;

public record RewriteReport(Condition original, Condition rewritten,
                            List<IndexSelection> selections, String forcedIndex) {

    public RewriteReport {
        selections = List.copyOf(selections);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== 查询重写报告 ==========\n");
        sb.append("[重写前] ").append(original).append('\n');
        sb.append("[重写后] ").append(rewritten).append('\n');
        sb.append("[强制索引] ").append(forcedIndex == null ? "无" : forcedIndex).append('\n');
        for (IndexSelection selection : selections) {
            sb.append("[选中索引] ").append(selection.branchLabel()).append(" -> ")
                    .append(selection.chosen() == null ? "无（全表扫描）" : selection.chosen().name())
                    .append('\n');
            sb.append("  选择理由: ").append(selection.reason()).append('\n');
            if (!selection.excluded().isEmpty()) {
                sb.append("  被排除索引: ");
                sb.append(selection.excluded().stream()
                        .map(c -> c.index().name() + "(" + c.reason() + ")")
                        .collect(Collectors.joining(", ")));
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
