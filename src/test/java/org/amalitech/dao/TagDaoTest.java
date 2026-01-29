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

}
