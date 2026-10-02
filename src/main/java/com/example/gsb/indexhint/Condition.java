package com.example.gsb.indexhint;

import java.util.Map;

public interface Condition {

    boolean test(Map<String, Object> row);

    String describe();
}
