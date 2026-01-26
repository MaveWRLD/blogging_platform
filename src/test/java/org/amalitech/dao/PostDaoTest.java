package org.amalitech.dao;

import org.amalitech.util.db.DBConnection;
import org.amalitech.util.exception.DatabaseException;
import org.amalitech.util.exception.NotFoundException;
import org.amalitech.models.Post;
import org.amalitech.models.SortOrder;
import org.amalitech.util.DBExecutor;
import org.amalitech.util.SqlBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostDaoTest {

    @InjectMocks
    private PostDao postDao;

    private MockedStatic<DBExecutor> dbExecutorMock;
    private MockedStatic<SqlBuilder> sqlBuilderMock;
    private MockedStatic<DBConnection> dbConnectionMock;

    @BeforeEach
    void setUp() {
        dbExecutorMock = mockStatic(DBExecutor.class);
        sqlBuilderMock = mockStatic(SqlBuilder.class);
        dbConnectionMock = mockStatic(DBConnection.class);
    }

    @AfterEach
    void tearDown() {
        dbExecutorMock.close();
        sqlBuilderMock.close();
        dbConnectionMock.close();
    }

    @Test
    void save_ShouldExecuteInsertStatement_WhenPostIsValid() {
        Post post = new Post("Test Title", "Test Body", 1, "DRAFT");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(title, body, user_id, status) VALUES (?, ?, ?, ?)",
                List.of("Test Title", "Test Body", 1, "DRAFT")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(post), eq(Set.of("id"))))
                .thenReturn(mockFragment);
        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenReturn(1);

        postDao.save(post);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                contains("INSERT INTO posts"),
                anyList()
        ));
    }

    @Test
    void save_ShouldThrowDatabaseException_WhenExecuteFails() {
        Post post = new Post("Test Title", "Test Body", 1, "DRAFT");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(title, body, user_id, status) VALUES (?, ?, ?, ?)",
                List.of("Test Title", "Test Body", 1, "DRAFT")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(post), eq(Set.of("id"))))
                .thenReturn(mockFragment);

        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenThrow(new DatabaseException("Insert failed", null));

        assertThatThrownBy(() -> postDao.save(post))
                .isInstanceOf(DatabaseException.class)
                .hasMessage("Insert failed");
    }

    @Test
    void findById_ShouldReturnPost_WhenPostExists() {
        int postId = 1;
        Post expectedPost = new Post(1, "Test Title", "Test Body", 1, "PUBLISHED");
        List<Post> queryResult = List.of(expectedPost);

        dbExecutorMock.when(() -> DBExecutor.query(
                eq("SELECT * FROM posts WHERE id = ?"),
                eq(List.of(postId)),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(queryResult);

        Post result = postDao.findById(postId);

        assertThat(result)
                .isNotNull()
                .extracting(Post::getId, Post::getTitle, Post::getBody, Post::getUserId, Post::getStatus)
                .containsExactly(1, "Test Title", "Test Body", 1, "PUBLISHED");
    }

    @Test
    void findById_ShouldThrowNotFoundException_WhenPostDoesNotExist() {
        int postId = 999;
        List<Post> emptyResult = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                eq("SELECT * FROM posts WHERE id = ?"),
                eq(List.of(postId)),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(emptyResult);

        assertThatThrownBy(() -> postDao.findById(postId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Post with ID 999 not found");
    }

    @Test
    void findAll_ShouldReturnAllPosts_WhenPostsExist() {
        List<Post> expectedPosts = List.of(
                new Post(1, "Title 1", "Body 1", 1, "PUBLISHED"),
                new Post(2, "Title 2", "Body 2", 2, "DRAFT"),
                new Post(3, "Title 3", "Body 3", 1, "PUBLISHED")
        );

        dbExecutorMock.when(() -> DBExecutor.query(
                eq("SELECT * FROM posts ORDER BY created_at DESC"),
                eq(new ArrayList<>()),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        List<Post> result = postDao.findAll();

        assertThat(result)
                .isNotNull()
                .hasSize(3)
                .extracting(Post::getTitle)
                .containsExactly("Title 1", "Title 2", "Title 3");
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoPostsExist() {
        List<Post> emptyResult = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                eq("SELECT * FROM posts ORDER BY created_at DESC"),
                eq(new ArrayList<>()),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(emptyResult);


        List<Post> result = postDao.findAll();

        assertThat(result)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void update_ShouldExecuteUpdateStatement_WhenPostIsValid() {
        Post post = new Post(1, "Updated Title", "Updated Body", 1, "PUBLISHED");
        post.setUpdatedAt(LocalDateTime.now());

        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "title = ?, body = ?, user_id = ?, status = ?, updated_at = ?",
                List.of("Updated Title", "Updated Body", 1, "PUBLISHED", post.getUpdatedAt())
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildUpdateSetClause(
                eq(post),
                eq(Set.of("id", "createdAt"))
        )).thenReturn(mockFragment);

        postDao.update(post);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                contains("UPDATE posts SET"),
                argThat(params -> params.size() == 6 && params.get(params.size() - 1).equals(1))
        ));
    }

    @Test
    void update_ShouldUpdateUpdatedAtTimestamp() {
        Post post = new Post(1, "Updated Title", "Updated Body", 1, "PUBLISHED");
        LocalDateTime beforeUpdate = LocalDateTime.now();

        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "title = ?",
                List.of("Updated Title")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildUpdateSetClause(
                any(Post.class),
                eq(Set.of("id", "createdAt"))
        )).thenReturn(mockFragment);

        postDao.update(post);

        assertThat(post.getUpdatedAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    void delete_ShouldExecuteDeleteStatement_WhenIdIsValid() {
        int postId = 1;

        postDao.delete(postId);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                eq("DELETE FROM posts WHERE id = ?"),
                eq(List.of(postId))
        ));
    }

    @Test
    void delete_ShouldThrowDatabaseException_WhenDeleteFails() {
        int postId = 1;

        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenThrow(new DatabaseException("Delete failed", null));

        assertThatThrownBy(() -> postDao.delete(postId))
                .isInstanceOf(DatabaseException.class)
                .hasMessage("Delete failed");
    }

    @Test
    void search_ShouldReturnPosts_WithNewestSortOrder() {
        List<Post> expectedPosts = List.of(
                new Post(1, "Title 1", "Body 1", 1, "PUBLISHED")
        );

        dbExecutorMock.when(() -> DBExecutor.query(
                contains("ORDER BY p.created_at DESC"),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        List<Post> result = postDao.search(null, null, null, null, SortOrder.NEWEST, 0, 10);

        assertThat(result)
                .isNotNull()
                .hasSize(1);
    }

    @Test
    void search_ShouldReturnPosts_WithOldestSortOrder() {
        List<Post> expectedPosts = List.of(
                new Post(1, "Title 1", "Body 1", 1, "PUBLISHED")
        );

        dbExecutorMock.when(() -> DBExecutor.query(
                contains("ORDER BY p.created_at ASC"),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        List<Post> result = postDao.search(null, null, null, null, SortOrder.OLDEST, 0, 10);

        assertThat(result)
                .isNotNull()
                .hasSize(1);
    }

    @Test
    void search_ShouldReturnPosts_WithMostCommentedSortOrder() {
        List<Post> expectedPosts = List.of(
                new Post(1, "Title 1", "Body 1", 1, "PUBLISHED")
        );

        dbExecutorMock.when(() -> DBExecutor.query(
                contains("SELECT COUNT(*) FROM comments"),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        List<Post> result = postDao.search(null, null, null, null, SortOrder.MOST_COMMENTED, 0, 10);

        assertThat(result)
                .isNotNull()
                .hasSize(1);
    }

    @Test
    void search_ShouldApplyPagination() {
        List<Post> expectedPosts = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        postDao.search(null, null, null, null, SortOrder.NEWEST, 2, 20);

        dbExecutorMock.verify(() -> DBExecutor.query(
                contains("LIMIT ? OFFSET ?"),
                argThat(params -> {
                    int size = params.size();
                    return params.get(size - 2).equals(20) && params.get(size - 1).equals(40);
                }),
                any(DBExecutor.RowMapper.class)
        ));
    }

    @Test
    void search_ShouldFilterByTagIds() {
        Set<Integer> tagIds = Set.of(1, 2, 3);
        List<Post> expectedPosts = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        postDao.search(null, tagIds, null, null, SortOrder.NEWEST, 0, 10);

        dbExecutorMock.verify(() -> DBExecutor.query(
                contains("post_tags"),
                anyList(),
                any(DBExecutor.RowMapper.class)
        ));
    }

    @Test
    void search_ShouldFilterByStatuses() {
        Set<String> statuses = Set.of("PUBLISHED", "DRAFT");
        List<Post> expectedPosts = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        postDao.search(null, null, statuses, null, SortOrder.NEWEST, 0, 10);

        dbExecutorMock.verify(() -> DBExecutor.query(
                contains("p.status IN"),
                anyList(),
                any(DBExecutor.RowMapper.class)
        ));
    }

    @Test
    void search_ShouldFilterByAuthorId() {
        Integer authorId = 5;
        List<Post> expectedPosts = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        postDao.search(null, null, null, authorId, SortOrder.NEWEST, 0, 10);

        dbExecutorMock.verify(() -> DBExecutor.query(
                contains("p.user_id = ?"),
                anyList(),
                any(DBExecutor.RowMapper.class)
        ));
    }

    @Test
    void search_ShouldApplyAllFilters() {
        String query = "java";
        Set<Integer> tagIds = Set.of(1);
        Set<String> statuses = Set.of("PUBLISHED");
        Integer authorId = 5;
        List<Post> expectedPosts = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        postDao.search(query, tagIds, statuses, authorId, SortOrder.NEWEST, 0, 10);

        dbExecutorMock.verify(() -> DBExecutor.query(
                argThat(sql -> sql.contains("ILIKE")
                        && sql.contains("post_tags")
                        && sql.contains("p.status IN")
                        && sql.contains("p.user_id = ?")),
                anyList(),
                any(DBExecutor.RowMapper.class)
        ));
    }

    @Test
    void listByTag_ShouldCallSearchWithCorrectParameters() {
        int tagId = 1;
        List<Post> expectedPosts = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        List<Post> result = postDao.listByTag(tagId, 0, 10);

        assertThat(result).isNotNull();
        dbExecutorMock.verify(() -> DBExecutor.query(
                contains("post_tags"),
                anyList(),
                any(DBExecutor.RowMapper.class)
        ));
    }


    @Test
    void search_ShouldHandleBlankQuery() {
        List<Post> expectedPosts = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        postDao.search("   ", null, null, null, SortOrder.NEWEST, 0, 10);

        dbExecutorMock.verify(() -> DBExecutor.query(
                argThat(sql -> !sql.contains("ILIKE")),
                anyList(),
                any(DBExecutor.RowMapper.class)
        ));
    }

    @Test
    void search_ShouldHandleEmptyTagIds() {
        Set<Integer> emptyTagIds = Set.of();
        List<Post> expectedPosts = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        postDao.search(null, emptyTagIds, null, null, SortOrder.NEWEST, 0, 10);

        dbExecutorMock.verify(() -> DBExecutor.query(
                argThat(sql -> !sql.contains("post_tags")),
                anyList(),
                any(DBExecutor.RowMapper.class)
        ));
    }

    @Test
    void search_ShouldHandleEmptyStatuses() {
        Set<String> emptyStatuses = Set.of();
        List<Post> expectedPosts = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedPosts);

        postDao.search(null, null, emptyStatuses, null, SortOrder.NEWEST, 0, 10);

        dbExecutorMock.verify(() -> DBExecutor.query(
                argThat(sql -> !sql.contains("p.status IN")),
                anyList(),
                any(DBExecutor.RowMapper.class)
        ));
    }
}
