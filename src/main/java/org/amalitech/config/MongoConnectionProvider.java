package org.amalitech.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.amalitech.util.exception.DatabaseException;
import org.amalitech.util.exception.NotFoundException;
import org.bson.Document;

public class MongoConnectionProvider {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static final java.util.Properties properties = new java.util.Properties();

    static {
        try (java.io.InputStream input = MongoConnectionProvider.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                throw new NotFoundException("db.properties file not found in resources folder");
            }
            properties.load(input);

            String connectionString = properties.getProperty("mongodb.uri", "mongodb://localhost:27017");
            String dbName = properties.getProperty("mongodb.database");

            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(dbName);
        } catch (Exception e) {
            throw new DatabaseException("Failed to load MongoDB configuration", e);
        }
    }

    public static MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    public static MongoCollection<Document> getCommentsCollection() {
        return getCollection(properties.getProperty("mongodb.collection.comments", "comments"));
    }
}
