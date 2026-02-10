package org.amalitech.util.RowMappers;

import org.amalitech.models.User;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Array;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("status")
        );

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }

        Array sqlArray = rs.getArray("roles");
        if (sqlArray != null) {
            String[] roleNames = (String[]) sqlArray.getArray();
            List<String> roles = Arrays.stream(roleNames)
                    .filter(Objects::nonNull)
                    .toList();
            user.setRoles(roles);
        } else {
            user.setRoles(List.of());
        }

        return user;
    }
}