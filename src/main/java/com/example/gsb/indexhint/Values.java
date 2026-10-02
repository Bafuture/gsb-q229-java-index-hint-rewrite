package com.example.gsb.indexhint;

final class Values {

    private Values() {
    }

    static int compare(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        Double da = asNumber(a);
        Double db = asNumber(b);
        if (da != null && db != null) {
            return Double.compare(da, db);
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    static Double asNumber(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    static String format(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder("(");
            boolean first = true;
            for (Object element : iterable) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(format(element));
                first = false;
            }
            return sb.append(')').toString();
        }
        return "'" + value + "'";
    }
}
