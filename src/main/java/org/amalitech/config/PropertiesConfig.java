package org.amalitech.config;

import org.amalitech.interfaces.AppConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesConfig implements AppConfig {

    private final Properties props;


    public PropertiesConfig() {
        props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                initializeDefaults();
            } else {
                props.load(input);
            }
        } catch (IOException e) {
            initializeDefaults();
        }
    }

    private void initializeDefaults() {
        props.setProperty("cache.max.size", "100");
        props.setProperty("cache.search.size", "50");
        props.setProperty("pagination.page.size", "10");
        props.setProperty("database.url", "jdbc:postgresql://localhost:5432/blog");
        props.setProperty("mongo.uri", "mongodb://localhost:27017");
        props.setProperty("mongo.database", "blog_db");
    }

    @Override
    public int getCacheMaxSize() {
        return Integer.parseInt(props.getProperty("cache.max.size", "100"));
    }

    @Override
    public int getSearchCacheSize() {
        return Integer.parseInt(props.getProperty("cache.search.size", "50"));
    }

    @Override
    public int getDefaultPageSize() {
        return Integer.parseInt(props.getProperty("pagination.page.size", "10"));
    }

    @Override
    public String getDatabaseUrl() {
        return props.getProperty("database.url", "jdbc:postgresql://localhost:5432/blog");
    }

    @Override
    public String getMongoUri() {
        return props.getProperty("mongo.uri", "mongodb://localhost:27017");
    }

    @Override
    public String getMongoDatabase() {
        return props.getProperty("mongo.database", "blog_db");
    }
}

