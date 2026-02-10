package org.amalitech.util.RowMappers;

import org.amalitech.models.Role;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MapRowToRole {

     public MapRowToRole() {};

    public static Role mapRowToRole(ResultSet rs) throws SQLException {
        return new Role(rs.getInt("id"), rs.getString("name"));
    }
}
