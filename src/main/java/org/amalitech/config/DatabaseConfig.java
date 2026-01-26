package org.amalitech.config;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.amalitech.util.exception.DatabaseException;
import org.amalitech.util.exception.NotFoundException;
import org.amalitech.service.MetricsService;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConfig {

    private static final Properties properties = new Properties();
    private static final DataSource dataSource;

    static {
        loadProperties();
        loadDriver();
        dataSource = createDataSource();
    }

    private DatabaseConfig() {}

    private static void loadProperties() {
        try (InputStream input = DatabaseConfig.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                throw new RuntimeException("db.properties file not found in resources folder");
            }

            properties.load(input);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }

    private static void loadDriver() {
        try {
            Class.forName(properties.getProperty("db.driver"));
        } catch (ClassNotFoundException e) {
            throw new NotFoundException("PostgreSQL JDBC Driver not found");
        }
    }

    private static DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getProperty("db.url"));
        config.setUsername(properties.getProperty("db.username"));
        config.setPassword(properties.getProperty("db.password"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("BloggingPlatformPool");

        config.setMetricRegistry(MetricsService.getRegistry());

        return new HikariDataSource(config);
    }

    public static Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get connection from pool", e);
        }
    }
}

