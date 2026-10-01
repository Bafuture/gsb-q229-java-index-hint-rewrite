package com.example.gsb.indexhint.model;

import java.util.ArrayList;
import java.util.List;

public final class Conditions {

    private Conditions() {
    }

    public static List<Comparison> flattenComparisons(Condition condition) {
        List<Comparison> result = new ArrayList<>();
        collect(condition, result);
        return result;
    }

    private static void collect(Condition condition, List<Comparison> out) {
        if (condition instanceof Comparison cmp) {
            out.add(cmp);
        } else if (condition instanceof AndCondition and) {
            and.conditions().forEach(c -> collect(c, out));
        }
    }
}
