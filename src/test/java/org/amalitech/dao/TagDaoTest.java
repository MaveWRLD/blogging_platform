package org.amalitech.dao;

import org.amalitech.util.exception.DatabaseException;
import org.amalitech.models.Tag;
import org.amalitech.util.db.DBExecutor;
import org.amalitech.util.db.SqlBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagDaoTest {

    @InjectMocks
    private TagDao tagDao;

    private MockedStatic<DBExecutor> dbExecutorMock;
    private MockedStatic<SqlBuilder> sqlBuilderMock;

    @BeforeEach
    void setUp() {
        dbExecutorMock = mockStatic(DBExecutor.class);
        sqlBuilderMock = mockStatic(SqlBuilder.class);
    }

    @AfterEach
    void tearDown() {
        dbExecutorMock.close();
        sqlBuilderMock.close();
    }

    @Test
    void save_ShouldExecuteInsertStatement_WhenTagIsValid() {
        Tag tag = new Tag("Java");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(name) VALUES (?)",
                List.of("Java")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(tag), eq(Set.of("id"))))
                .thenReturn(mockFragment);
        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenReturn(1);

        int generatedId = tagDao.save(tag);

        assertThat(generatedId).isEqualTo(1);
        assertThat(tag.getId()).isEqualTo(1);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                eq("INSERT INTO tags (name) VALUES (?) RETURNING id"),
                eq(List.of("Java"))
        ));
    }

    @Test
    void save_ShouldThrowDatabaseException_WhenExecuteFails() {
        Tag tag = new Tag("Java");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(name) VALUES (?)",
                List.of("Java")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(tag), eq(Set.of("id"))))
                .thenReturn(mockFragment);

        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenThrow(new DatabaseException("Insert failed", null));

        assertThatThrownBy(() -> tagDao.save(tag))
                .isInstanceOf(DatabaseException.class)
                .hasMessage("Insert failed");
    }

    @Test
    void findById_ShouldReturnTag_WhenTagExists() {
        int tagId = 1;
        Tag expectedTag = new Tag(1, "Java");
        List<Tag> queryResult = List.of(expectedTag);

        dbExecutorMock.when(() -> DBExecutor.query(
                eq("SELECT * FROM tags WHERE id = ?"),
                eq(List.of(tagId)),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(queryResult);

        Tag result = tagDao.findById(tagId);

        assertThat(result)
                .isNotNull()
                .extracting(Tag::getId, Tag::getName)
                .containsExactly(1, "Java");

        dbExecutorMock.verify(() -> DBExecutor.query(
                eq("SELECT * FROM tags WHERE id = ?"),
                eq(List.of(tagId)),
                any(DBExecutor.RowMapper.class)
        ));
    }

    @Test
    void findById_ShouldThrowException_WhenTagDoesNotExist() {
        int tagId = 999;
        List<Tag> emptyResult = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                eq("SELECT * FROM tags WHERE id = ?"),
                eq(List.of(tagId)),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(emptyResult);

        assertThatThrownBy(() -> tagDao.findById(tagId))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void findAll_ShouldReturnAllTags_WhenTagsExist() {
        List<Tag> expectedTags = List.of(
                new Tag(1, "Java"),
                new Tag(2, "Spring"),
                new Tag(3, "Testing")
        );

        dbExecutorMock.when(() -> DBExecutor.query(
                eq("SELECT * FROM tags ORDER BY name"),
                eq(new ArrayList<>()),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(expectedTags);

        List<Tag> result = tagDao.findAll();

        assertThat(result)
                .isNotNull()
                .hasSize(3)
                .extracting(Tag::getName)
                .containsExactly("Java", "Spring", "Testing");
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoTagsExist() {
        List<Tag> emptyResult = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                eq("SELECT * FROM tags ORDER BY name"),
                eq(new ArrayList<>()),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(emptyResult);

        List<Tag> result = tagDao.findAll();

        assertThat(result)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void update_ShouldExecuteUpdateStatement_WhenTagIsValid() {
        Tag tag = new Tag(1, "Updated Java");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "name = ?",
                List.of("Updated Java")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildUpdateSetClause(
                eq(tag),
                eq(Set.of("id"))
        )).thenReturn(mockFragment);

        tagDao.update(tag);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                eq("UPDATE tags SET name = ? WHERE id = ?"),
                eq(List.of("Updated Java", 1))
        ));
    }

    @Test
    void update_ShouldThrowDatabaseException_WhenUpdateFails() {
        Tag tag = new Tag(1, "Updated Java");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "name = ?",
                List.of("Updated Java")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildUpdateSetClause(
                eq(tag),
                eq(Set.of("id"))
        )).thenReturn(mockFragment);

        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenThrow(new DatabaseException("Update failed", null));

        assertThatThrownBy(() -> tagDao.update(tag))
                .isInstanceOf(DatabaseException.class)
                .hasMessage("Update failed");
    }

    @Test
    void delete_ShouldExecuteDeleteStatement_WhenIdIsValid() {
        int tagId = 1;

        tagDao.delete(tagId);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                eq("DELETE FROM tags WHERE id = ?"),
                eq(List.of(tagId))
        ));
    }

    @Test
    void delete_ShouldThrowDatabaseException_WhenDeleteFails() {
        int tagId = 1;

        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenThrow(new DatabaseException("Delete failed", null));

        assertThatThrownBy(() -> tagDao.delete(tagId))
                .isInstanceOf(DatabaseException.class)
                .hasMessage("Delete failed");
    }

    @Test
    void save_ShouldHandleNullName_WhenTagHasNullName() {
        Tag tag = new Tag(null);
        List<Object> paramsWithNull = new ArrayList<>();
        paramsWithNull.add(null);
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(name) VALUES (?)",
                paramsWithNull
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(tag), eq(Set.of("id"))))
                .thenReturn(mockFragment);
        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenReturn(1);

        tagDao.save(tag);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                eq("INSERT INTO tags (name) VALUES (?) RETURNING id"),
                argThat(params -> params.size() == 1 && params.get(0) == null)
        ));
    }

    @Test
    void findAll_ShouldCallQueryWithCorrectParameters() {
        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(new ArrayList<>());

        tagDao.findAll();

        dbExecutorMock.verify(() -> DBExecutor.query(
                eq("SELECT * FROM tags ORDER BY name"),
                argThat(List::isEmpty),
                any(DBExecutor.RowMapper.class)
        ), times(1));
    }

    @Test
    void update_ShouldIncludeTagIdInParameters() {
        Tag tag = new Tag(42, "Spring Boot");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "name = ?",
                List.of("Spring Boot")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildUpdateSetClause(
                eq(tag),
                eq(Set.of("id"))
        )).thenReturn(mockFragment);

        tagDao.update(tag);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                anyString(),
                argThat(params -> params.size() == 2 &&
                        params.get(0).equals("Spring Boot") &&
                        params.get(1).equals(42))
        ));
    }
}
