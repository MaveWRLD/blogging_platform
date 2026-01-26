package org.amalitech.config;

/**
 * Configuration interface for application settings.
 * Allows externalization of configuration from code.
 */
public interface AppConfig {

    /**
     * Get maximum cache size for post cache.
     * @return max cache size
     */
    int getCacheMaxSize();

    /**
     * Get maximum cache size for search results.
     * @return max search results cache size
     */
    int getSearchCacheSize();

    /**
     * Get default page size for pagination.
     * @return page size
     */
    int getDefaultPageSize();

    /**
     * Get database connection URL.
     * @return database URL
     */
    String getDatabaseUrl();

    /**
     * Get MongoDB connection URI.
     * @return MongoDB URI
     */
    String getMongoUri();

    /**
     * Get MongoDB database name.
     * @return database name
     */
    String getMongoDatabase();
}

