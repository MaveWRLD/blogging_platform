package org.amalitech.dao;

import org.amalitech.util.db.DBConnection;
import org.amalitech.util.exception.DatabaseException;
import org.amalitech.models.PostTag;
import org.amalitech.util.CamelToSnake;
import org.amalitech.util.db.DBExecutor;
import org.amalitech.util.db.SqlBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostTagDaoTest {

    @InjectMocks
    private PostTagDao postTagDao;

    private MockedStatic<DBExecutor> dbExecutorMock;
    private MockedStatic<SqlBuilder> sqlBuilderMock;
    private MockedStatic<DBConnection> dbConnectionMock;
    private MockedStatic<CamelToSnake> camelToSnakeMock;

    @BeforeEach
    void setUp() {
        dbExecutorMock = mockStatic(DBExecutor.class);
        sqlBuilderMock = mockStatic(SqlBuilder.class);
        dbConnectionMock = mockStatic(DBConnection.class);
        camelToSnakeMock = mockStatic(CamelToSnake.class);
    }

    @AfterEach
    void tearDown() {
        dbExecutorMock.close();
        sqlBuilderMock.close();
        dbConnectionMock.close();
        camelToSnakeMock.close();
    }

    @Test
    void save_ShouldExecuteInsertStatement_WhenPostTagIsValid() {
        PostTag postTag = new PostTag(1, 5);
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(post_id, tag_id) VALUES (?, ?)",
                List.of(1, 5)
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(postTag), eq(Set.of("id"))))
                .thenReturn(mockFragment);
        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenReturn(0);

        postTagDao.save(postTag);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                eq("INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?)"),
                eq(List.of(1, 5))
        ));
    }

    @Test
    void save_ShouldExcludeIdField_WhenBuildingInsertClause() {
        PostTag postTag = new PostTag(2, 3);
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(post_id, tag_id) VALUES (?, ?)",
                List.of(2, 3)
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(any(PostTag.class), eq(Set.of("id"))))
                .thenReturn(mockFragment);
        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenReturn(0);

        postTagDao.save(postTag);

        sqlBuilderMock.verify(() -> SqlBuilder.buildInsertClause(
                eq(postTag),
                eq(Set.of("id"))
        ));
    }

    @Test
    void save_ShouldThrowDatabaseException_WhenExecuteFails() {
        PostTag postTag = new PostTag(1, 5);
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(post_id, tag_id) VALUES (?, ?)",
                List.of(1, 5)
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(postTag), eq(Set.of("id"))))
                .thenReturn(mockFragment);

        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenThrow(new DatabaseException("Insert failed", null));

        assertThatThrownBy(() -> postTagDao.save(postTag))
                .isInstanceOf(DatabaseException.class)
                .hasMessage("Insert failed");
    }

    @Test
    void delete_ShouldExecuteDeleteStatement_WhenPostTagIsValid() {
        PostTag postTag = new PostTag(1, 5);

        postTagDao.delete(postTag);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                eq("DELETE FROM post_tags WHERE post_id = ? AND tag_id = ?"),
                eq(List.of(1, 5))
        ));
    }

    @Test
    void delete_ShouldUsePostIdAndTagId_AsParameters() {
        PostTag postTag = new PostTag(10, 20);

        postTagDao.delete(postTag);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                anyString(),
                argThat(params -> params.size() == 2
                        && params.get(0).equals(10)
                        && params.get(1).equals(20))
        ));
    }

    @Test
    void delete_ShouldThrowDatabaseException_WhenExecuteFails() {
        PostTag postTag = new PostTag(1, 5);

        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenThrow(new DatabaseException("Delete failed", null));

        assertThatThrownBy(() -> postTagDao.delete(postTag))
                .isInstanceOf(DatabaseException.class)
                .hasMessage("Delete failed");
    }

    @Test
    void deleteAllTagsForPost_ShouldExecuteDeleteStatement_WithConvertedColumnName() {
        int postId = 1;
        camelToSnakeMock.when(() -> CamelToSnake.camelToSnake("postId")).thenReturn("post_id");

        postTagDao.deleteAllTagsForPost(postId);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                eq("DELETE FROM post_tags WHERE post_id = ?"),
                eq(List.of(postId))
        ));
    }

    @Test
    void deleteAllTagsForPost_ShouldConvertCamelCaseToSnakeCase() {
        int postId = 5;
        camelToSnakeMock.when(() -> CamelToSnake.camelToSnake("postId")).thenReturn("post_id");

        postTagDao.deleteAllTagsForPost(postId);

        camelToSnakeMock.verify(() -> CamelToSnake.camelToSnake("postId"));
    }

    @Test
    void deleteAllTagsForPost_ShouldThrowDatabaseException_WhenExecuteFails() {
        int postId = 1;
        camelToSnakeMock.when(() -> CamelToSnake.camelToSnake("postId")).thenReturn("post_id");

        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenThrow(new DatabaseException("Delete failed", null));

        assertThatThrownBy(() -> postTagDao.deleteAllTagsForPost(postId))
                .isInstanceOf(DatabaseException.class)
                .hasMessage("Delete failed");
    }
}
