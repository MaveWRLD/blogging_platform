package org.amalitech.util.db;

import lombok.Getter;

import java.util.*;

public class SqlBuilder {

    private String baseSelect;
    private final List<Object> params = new ArrayList<>();
    private final List<String> whereClauses = new ArrayList<>();
    private final List<String> havingClauses = new ArrayList<>();
    ArrayList<String> groupByColumns = new ArrayList<>();
    private String orderByClause = null;
    private Integer limitValue = null;
    private Long offsetValue = null;

    public SqlBuilder(String baseSql) {
        this.baseSelect = baseSql;
    }

    public SqlBuilder groupBy(String... columns) {

        groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }

    public SqlBuilder where(String condition, Object... values) {
        whereClauses.add(condition);
        Collections.addAll(params, values);
        return this;
    }

    public SqlBuilder and(String condition, Object... values) {
        return where(condition, values);
    }

    public void having(String condition, Object... values) {
        havingClauses.add(condition);
        Collections.addAll(params, values);
    }

    public void orderBy(String orderBy) {
        this.orderByClause = orderBy;
    }

    public void limit(int size, long offset) {
        this.limitValue = size;
        this.offsetValue = offset;
    }

    public String getSql() {
        StringBuilder sql = new StringBuilder(baseSelect);

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }

        if (!groupByColumns.isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", groupByColumns));
        }

        if (!havingClauses.isEmpty()) {
            sql.append(" HAVING ").append(String.join(" AND ", havingClauses));
        }

        if (orderByClause != null) {
            sql.append(" ORDER BY ").append(orderByClause);
        }

        if (limitValue != null) {
            sql.append(" LIMIT ?");
            if (offsetValue != null) {
                sql.append(" OFFSET ?");
            }
        }

        return sql.toString();
    }


    public List<Object> getParams() {
        List<Object> allParams = new ArrayList<>(params);
        if (limitValue != null) {
            allParams.add(limitValue);
            if (offsetValue != null) {
                allParams.add(offsetValue);
            }
        }
        return allParams;
    }


    @Getter
    public static class SqlFragment {
        private final String clause;
        private final List<Object> params;

        public SqlFragment(String clause, List<Object> params) {
            this.clause = clause;
            this.params = params;
        }

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

    @Override
    public String toString() {
        return "SqlBuilder{" +
                "sql=" + getSql() +
                ", params=" + getParams() +
                '}';
    }
}

