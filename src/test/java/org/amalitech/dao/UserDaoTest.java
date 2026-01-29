package org.amalitech.dao;

import org.amalitech.util.exception.DatabaseException;
import org.amalitech.util.exception.NotFoundException;
import org.amalitech.models.User;
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
class UserDaoTest {

    @InjectMocks
    private UserDao userDao;

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
    void save_ShouldExecuteInsertStatement_WhenUserIsValid() {
        User user = new User("john_doe", "john@example.com", "hashedPassword123", "USER", "ACTIVE");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(username, email, password, role, status) VALUES (?, ?, ?, ?, ?)",
                List.of("john_doe", "john@example.com", "hashedPassword123", "USER", "ACTIVE")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(user), eq(Set.of("id", "createdAt"))))
                .thenReturn(mockFragment);
        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenReturn(1);

        userDao.save(user);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                eq("INSERT INTO users (username, email, password, role, status) VALUES (?, ?, ?, ?, ?) RETURNING id"),
                eq(List.of("john_doe", "john@example.com", "hashedPassword123", "USER", "ACTIVE"))
        ));
        assertThat(user.getId()).isEqualTo(1);
    }

    @Test
    void save_ShouldExcludeIdAndCreatedAt_WhenBuildingInsertClause() {
        User user = new User("jane_doe", "jane@example.com", "hashedPassword456", "ADMIN", "ACTIVE");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(username, email, password, role, status) VALUES (?, ?, ?, ?, ?)",
                List.of("jane_doe", "jane@example.com", "hashedPassword456", "ADMIN", "ACTIVE")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(any(User.class), eq(Set.of("id", "createdAt"))))
                .thenReturn(mockFragment);
        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenReturn(1);

        userDao.save(user);

        sqlBuilderMock.verify(() -> SqlBuilder.buildInsertClause(
                eq(user),
                eq(Set.of("id", "createdAt"))
        ));
    }

    @Test
    void save_ShouldThrowDatabaseException_WhenExecuteFails() {
        User user = new User("john_doe", "john@example.com", "hashedPassword123", "USER", "ACTIVE");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(username, email, password, role, status) VALUES (?, ?, ?, ?, ?)",
                List.of("john_doe", "john@example.com", "hashedPassword123", "USER", "ACTIVE")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(user), eq(Set.of("id", "createdAt"))))
                .thenReturn(mockFragment);

        dbExecutorMock.when(() -> DBExecutor.execute(anyString(), anyList()))
                .thenThrow(new DatabaseException("Insert failed", null));

        assertThatThrownBy(() -> userDao.save(user))
                .isInstanceOf(DatabaseException.class)
                .hasMessage("Insert failed");
    }
}
