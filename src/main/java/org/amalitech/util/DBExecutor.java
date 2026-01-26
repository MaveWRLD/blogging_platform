package org.amalitech.util;


import org.amalitech.util.db.DBConnection;
import org.amalitech.util.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DBExecutor {

    public static void bindParameters(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    public static int execute(String sql, List<Object> params) {
        try (Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParameters(ps, params);
            boolean hasResultSet = ps.execute();
            if (hasResultSet) {
                try (ResultSet rs = ps.getResultSet()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Database operation failed", e);
        }
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static <T> List<T> query(String sql, List<Object> params, RowMapper<T> mapper) {
        List<T> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParameters(ps, params);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(mapper.map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error executing query", e);
        }
        return results;
    }
}

