package org.amalitech.util;

import com.github.javafaker.Faker;
import org.amalitech.util.db.DBConnection;
import org.amalitech.models.Post;
import org.amalitech.util.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class DummyPostGenerator {

    private static final String[] STATUSES = { "PUBLISHED"};


    private static final Random RANDOM = new Random();
    private static final Faker FAKER = new Faker();


    public static void main(String[] args) {
        try {
            batchInsertPosts(generate(100000));
        } catch (SQLException e) {
            throw new DatabaseException(e.getMessage());
        }

    }

    public static void batchInsertPosts(List<Post> posts) throws SQLException {
        String sql = """
        INSERT INTO posts (title, body, user_id, status)
        VALUES (?, ?, ?, ?)
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (Post p : posts) {
                ps.setString(1, p.getTitle());
                ps.setString(2, p.getBody());
                ps.setInt(3, p.getUserId());
                ps.setString(4, p.getStatus());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();
        }
    }

    public static List<Post> generate(int count) {
        List<Post> posts = new ArrayList<>(count);

        for (int i = 1; i <= count; i++) {

            String title = generateTitle();
            String body = generateBody();

            int authorId = FAKER.number().numberBetween(1, 10);
            String status = STATUSES[RANDOM.nextInt(STATUSES.length)];

            posts.add(new Post(title, body, authorId, status));
        }
        return posts;
    }

    private static String generateTitle() {
        return switch (RANDOM.nextInt(5)) {
            case 0 -> FAKER.hacker().ingverb() + " " + FAKER.hacker().noun()
                    + " with " + FAKER.app().name();
            case 1 -> "Scaling " + FAKER.hacker().noun() + " for "
                    + FAKER.file().extension().toUpperCase() + " workloads";
            case 2 -> FAKER.hacker().adjective() + " " + FAKER.hacker().noun() + " patterns";
            case 3 -> FAKER.company().industry() + " " + FAKER.hacker().abbreviation()
                    + " best practices";
            default -> "From " + FAKER.hacker().abbreviation() + " to "
                    + FAKER.hacker().abbreviation() + ": "
                    + capitalize(FAKER.lorem().sentence(FAKER.number().numberBetween(3, 6)));
        };
    }

    private static String generateBody() {
        StringBuilder sb = new StringBuilder();
        int paragraphs = FAKER.number().numberBetween(2, 8);
        for (int i = 0; i < paragraphs; i++) {
            int sentences = FAKER.number().numberBetween(2, 6);
            sb.append(FAKER.lorem().paragraph(sentences)).append("\n\n");
        }
        sb.append("Discussion includes ")
                .append(FAKER.hacker().noun()).append(", ")
                .append(FAKER.hacker().abbreviation()).append(", and ")
                .append(FAKER.hacker().verb()).append(".\n");
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
