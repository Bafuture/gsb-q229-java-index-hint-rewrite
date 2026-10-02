package com.example.gsb.indexhint;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TableStats {

    public record ColumnStats(long distinctCount, Object min, Object max) {
    }

    private final long rowCount;
    private final Map<String, ColumnStats> columns;

    public TableStats(long rowCount) {
        this(rowCount, Map.of());
    }

    private TableStats(long rowCount, Map<String, ColumnStats> columns) {
        this.rowCount = rowCount;
        this.columns = columns;
    }

    public TableStats withColumn(String field, long distinctCount) {
        return withColumn(field, distinctCount, null, null);
    }

    public TableStats withColumn(String field, long distinctCount, Object min, Object max) {
        Map<String, ColumnStats> copy = new LinkedHashMap<>(columns);
        copy.put(field, new ColumnStats(distinctCount, min, max));
        return new TableStats(rowCount, copy);
    }

    public long rowCount() {
        return rowCount;
    }

    public ColumnStats column(String field) {
        return columns.get(field);
    }
}
