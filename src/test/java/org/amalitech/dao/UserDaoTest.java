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

    @Test
    void findByUsername_ShouldReturnUser_WhenUserExists() {
        String username = "john_doe";
        User expectedUser = new User(1, "john_doe", "john@example.com", "hashedPassword123", "USER", "ACTIVE");
        List<User> queryResult = List.of(expectedUser);

        dbExecutorMock.when(() -> DBExecutor.query(
                eq("SELECT id, username, email, password, role, status, created_at FROM users WHERE username = ?"),
                eq(List.of(username)),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(queryResult);

        User result = userDao.findByUsername(username);

        assertThat(result)
                .isNotNull()
                .extracting(User::getId, User::getUsername, User::getEmail, User::getRole, User::getStatus)
                .containsExactly(1, "john_doe", "john@example.com", "USER", "ACTIVE");
    }

    @Test
    void findByUsername_ShouldIncludePassword_WhenUserExists() {
        String username = "john_doe";
        User expectedUser = new User(1, "john_doe", "john@example.com", "hashedPassword123", "USER", "ACTIVE");
        List<User> queryResult = List.of(expectedUser);

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                eq(List.of(username)),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(queryResult);

        User result = userDao.findByUsername(username);

        assertThat(result.getPassword()).isEqualTo("hashedPassword123");
    }

    @Test
    void findByUsername_ShouldThrowNotFoundException_WhenUserDoesNotExist() {
        String username = "nonexistent_user";
        List<User> emptyResult = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                eq("SELECT id, username, email, password, role, status, created_at FROM users WHERE username = ?"),
                eq(List.of(username)),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(emptyResult);

        assertThatThrownBy(() -> userDao.findByUsername(username))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Post with username nonexistent_user not found");
    }

    @Test
    void findByUsername_ShouldUseCorrectSQL_WithAllRequiredColumns() {
        String username = "john_doe";
        User expectedUser = new User(1, "john_doe", "john@example.com", "hashedPassword123", "USER", "ACTIVE");
        List<User> queryResult = List.of(expectedUser);

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(queryResult);

        userDao.findByUsername(username);

        dbExecutorMock.verify(() -> DBExecutor.query(
                argThat(sql -> sql.contains("SELECT id, username, email, password, role, status, created_at")
                        && sql.contains("FROM users")
                        && sql.contains("WHERE username = ?")),
                eq(List.of(username)),
                any(DBExecutor.RowMapper.class)
        ));
    }

    @Test
    void findByUsername_ShouldHandleNullUsername() {
        String username = null;
        List<User> emptyResult = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(emptyResult);

        assertThatThrownBy(() -> userDao.findByUsername(username))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void findByUsername_ShouldHandleEmptyUsername() {
        String username = "";
        List<User> emptyResult = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                eq(List.of("")),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(emptyResult);

        assertThatThrownBy(() -> userDao.findByUsername(username))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Post with username  not found");
    }

    @Test
    void findByUsername_ShouldReturnFirstUser_WhenMultipleUsersMatch() {
        String username = "john_doe";
        User firstUser = new User(1, "john_doe", "john1@example.com", "password1", "USER", "ACTIVE");
        User secondUser = new User(2, "john_doe", "john2@example.com", "password2", "USER", "ACTIVE");
        List<User> queryResult = List.of(firstUser, secondUser);

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                eq(List.of(username)),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(queryResult);

        User result = userDao.findByUsername(username);

        assertThat(result)
                .isNotNull()
                .extracting(User::getId, User::getEmail)
                .containsExactly(1, "john1@example.com");
    }

    @Test
    void save_ShouldHandleUserWithAllFields() {
        User user = new User("admin", "admin@example.com", "secureHash", "ADMIN", "ACTIVE");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(username, email, password, role, status) VALUES (?, ?, ?, ?, ?)",
                List.of("admin", "admin@example.com", "secureHash", "ADMIN", "ACTIVE")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(user), eq(Set.of("id", "createdAt"))))
                .thenReturn(mockFragment);

        userDao.save(user);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                anyString(),
                argThat(params -> params.size() == 5
                        && params.get(0).equals("admin")
                        && params.get(3).equals("ADMIN"))
        ));
    }

    @Test
    void save_ShouldHandleUserWithDifferentRoles() {
        User moderator = new User("mod", "mod@example.com", "hash", "MODERATOR", "ACTIVE");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(username, email, password, role, status) VALUES (?, ?, ?, ?, ?)",
                List.of("mod", "mod@example.com", "hash", "MODERATOR", "ACTIVE")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(moderator), eq(Set.of("id", "createdAt"))))
                .thenReturn(mockFragment);

        userDao.save(moderator);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                contains("INSERT INTO users"),
                argThat(params -> params.contains("MODERATOR"))
        ));
    }

    @Test
    void save_ShouldHandleUserWithDifferentStatuses() {
        User inactiveUser = new User("inactive", "inactive@example.com", "hash", "USER", "INACTIVE");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(username, email, password, role, status) VALUES (?, ?, ?, ?, ?)",
                List.of("inactive", "inactive@example.com", "hash", "USER", "INACTIVE")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(eq(inactiveUser), eq(Set.of("id", "createdAt"))))
                .thenReturn(mockFragment);

        userDao.save(inactiveUser);

        dbExecutorMock.verify(() -> DBExecutor.execute(
                anyString(),
                argThat(params -> params.contains("INACTIVE"))
        ));
    }

    @Test
    void findByUsername_ShouldHandleUsernameWithSpecialCharacters() {
        String username = "user@123";
        User expectedUser = new User(1, "user@123", "test@example.com", "hash", "USER", "ACTIVE");
        List<User> queryResult = List.of(expectedUser);

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                eq(List.of(username)),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(queryResult);

        User result = userDao.findByUsername(username);

        assertThat(result)
                .isNotNull()
                .extracting(User::getUsername)
                .isEqualTo("user@123");
    }

    @Test
    void findByUsername_ShouldPassUsernameAsParameter() {
        String username = "test_user";
        List<User> emptyResult = new ArrayList<>();

        dbExecutorMock.when(() -> DBExecutor.query(
                anyString(),
                anyList(),
                any(DBExecutor.RowMapper.class)
        )).thenReturn(emptyResult);

        assertThatThrownBy(() -> userDao.findByUsername(username));

        dbExecutorMock.verify(() -> DBExecutor.query(
                anyString(),
                argThat(params -> params.size() == 1 && params.get(0).equals("test_user")),
                any(DBExecutor.RowMapper.class)
        ));
    }

    @Test
    void save_ShouldCallExecuteExactlyOnce() {
        User user = new User("single", "single@example.com", "hash", "USER", "ACTIVE");
        SqlBuilder.SqlFragment mockFragment = new SqlBuilder.SqlFragment(
                "(username) VALUES (?)",
                List.of("single")
        );

        sqlBuilderMock.when(() -> SqlBuilder.buildInsertClause(any(User.class), anySet()))
                .thenReturn(mockFragment);

        userDao.save(user);

        dbExecutorMock.verify(() -> DBExecutor.execute(anyString(), anyList()), times(1));
    }

    @Test
    void findByUsername_ShouldCallQueryExactlyOnce() {
        String username = "query_once";
        List<User> queryResult = List.of(new User(1, username, "email", "pass", "USER", "ACTIVE"));

        dbExecutorMock.when(() -> DBExecutor.query(anyString(), anyList(), any(DBExecutor.RowMapper.class)))
                .thenReturn(queryResult);

        userDao.findByUsername(username);

        dbExecutorMock.verify(() -> DBExecutor.query(anyString(), anyList(), any(DBExecutor.RowMapper.class)), times(1));
    }
}
