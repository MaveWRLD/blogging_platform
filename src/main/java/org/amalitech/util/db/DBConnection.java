package org.amalitech.util.db;



import org.amalitech.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;

public final class DBConnection {

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        return DatabaseConfig.getConnection();
    }


}

