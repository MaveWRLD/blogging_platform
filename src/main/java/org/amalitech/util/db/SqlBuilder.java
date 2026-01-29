package org.amalitech.util.db;

import java.util.*;

public class SqlBuilder {

    public static class SqlFragment {
        private final String clause;
        private final List<Object> params;

        public SqlFragment(String clause, List<Object> params) {
            this.clause = clause;
            this.params = params;
        }

        public String getClause() { return clause; }
        public List<Object> getParams() { return params; }

        @Override
        public String toString() {
            return "SqlFragment{" +
                    "clause='" + clause + '\'' +
                    ", params=" + params +
                    '}';
        }
    }

    public static SqlFragment buildUpdateSetClause(Object bean, Set<String> excludeFields) {

        List<ReflectionUtils.FieldInfo> fields = ReflectionUtils.extractNonNullFields(bean, excludeFields);

        if (fields.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        StringBuilder set = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (ReflectionUtils.FieldInfo f : fields) {
            set.append(f.column()).append(" = ?, ");
            params.add(f.value());
        }

        set.setLength(set.length() - 2);

        return new SqlFragment(set.toString(), params);
    }

    public static SqlFragment buildInsertClause(Object bean, Set<String> excludeFields) {

        List<ReflectionUtils.FieldInfo> fields = ReflectionUtils.extractNonNullFields(bean, excludeFields);

        if (fields.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for insert");
        }

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (ReflectionUtils.FieldInfo f : fields) {
            columns.append(f.column()).append(", ");
            placeholders.append("?, ");
            params.add(f.value());
        }

        columns.setLength(columns.length() - 2);
        placeholders.setLength(placeholders.length() - 2);

        String clause = "(" + columns + ") VALUES (" + placeholders + ")";

        return new SqlFragment(clause, params);
    }
}

