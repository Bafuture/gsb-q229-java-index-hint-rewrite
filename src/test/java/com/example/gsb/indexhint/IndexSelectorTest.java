package com.example.gsb.indexhint;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gsb.indexhint.model.Comparison;
import com.example.gsb.indexhint.model.IndexDefinition;
import com.example.gsb.indexhint.select.IndexSelection;
import com.example.gsb.indexhint.select.IndexSelector;
import java.util.List;
import org.junit.jupiter.api.Test;

class IndexSelectorTest {

    private final IndexSelector selector = new IndexSelector();

    private final IndexDefinition idxStatus =
            IndexDefinition.of("idx_status", "status").withSelectivity("status", 0.5);
    private final IndexDefinition idxCustomer =
            IndexDefinition.of("idx_customer", "customer_id").withSelectivity("customer_id", 0.01);
    private final IndexDefinition idxNote = IndexDefinition.of("idx_note", "note");

    @Test
    void picksLowestCostIndexAndExplainsWhy() {
        List<Comparison> conditions = List.of(
                Comparison.eq("status", "X"),
                Comparison.eq("customer_id", 5));

        IndexSelection selection = selector.select("主查询", conditions,
                List.of(idxStatus, idxCustomer, idxNote), null);

        assertThat(selection.chosen().name()).isEqualTo("idx_customer");
        assertThat(selection.reason())
                .contains("customer_id")
                .contains("候选索引中最低");
        assertThat(selection.candidates()).hasSize(2);
        assertThat(selection.excluded()).hasSize(1);
        assertThat(selection.excluded().get(0).index().name()).isEqualTo("idx_note");
        assertThat(selection.excluded().get(0).reason()).contains("note");
    }

    @Test
    void prefersCompositeIndexCoveringMoreColumns() {
        IndexDefinition composite = IndexDefinition.of("idx_status_created", "status", "created_at")
                .withSelectivity("status", 0.2);
        List<Comparison> conditions = List.of(
                Comparison.eq("status", "X"),
                Comparison.ge("created_at", "2024-01-01"));

        IndexSelection selection = selector.select("主查询", conditions,
                List.of(idxStatus, composite), null);

        assertThat(selection.chosen().name()).isEqualTo("idx_status_created");
        assertThat(selection.reason()).contains("[status, created_at]");
    }

    @Test
    void reportsFullScanWhenNoIndexUsable() {
        List<Comparison> conditions = List.of(Comparison.eq("note", "x"));

        IndexSelection selection = selector.select("主查询", conditions,
                List.of(idxStatus), null);

        assertThat(selection.chosen()).isNull();
        assertThat(selection.reason()).contains("全表扫描");
    }
}
