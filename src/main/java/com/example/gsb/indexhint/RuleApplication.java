package com.example.gsb.indexhint;

public record RuleApplication(QueryLike query, boolean applied, String detail) {

    public static RuleApplication unchanged(QueryLike query) {
        return new RuleApplication(query, false, "");
    }

    public static RuleApplication applied(QueryLike query, String detail) {
        return new RuleApplication(query, true, detail);
    }
}
