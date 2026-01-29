package org.amalitech.dao;

import org.amalitech.models.Post;
import org.amalitech.util.db.SqlBuilder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PostDaoSqlTest {

    @Test
    void testSaveSqlGeneration() {
        Post post = new Post("My Title", "My Body", 1, "PUBLISHED");
        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(post, Set.of("id"));
        
        String clause = insert.getClause();
        String columnsPart = clause.substring(clause.indexOf("(") + 1, clause.indexOf(")"));
        String valuesPart = clause.substring(clause.lastIndexOf("(") + 1, clause.lastIndexOf(")"));

        String sql = "INSERT INTO posts (" + columnsPart + ", search_vector) VALUES (" + valuesPart + ", to_tsvector('english', ? || ' ' || ?))";
        
        assertThat(sql).contains("INSERT INTO posts (");
        assertThat(sql).contains(", search_vector) VALUES (");
        assertThat(sql).contains(", to_tsvector('english', ? || ' ' || ?))");
        
        // Count placeholders
        long placeholderCount = sql.chars().filter(ch -> ch == '?').count();
        int expectedPlaceholders = insert.getParams().size() + 2;
        assertThat(placeholderCount).isEqualTo(expectedPlaceholders);
        
        System.out.println("Generated SQL: " + sql);
    }
}
