package com.example.gsb.indexhint.rule;

import com.example.gsb.indexhint.RewriteStatistics;
import com.example.gsb.indexhint.model.Condition;
import com.example.gsb.indexhint.model.IndexDefinition;
import java.util.List;

public interface RewriteRule {

    String name();

    Condition apply(Condition condition, List<IndexDefinition> indexes, RewriteStatistics statistics);
}
