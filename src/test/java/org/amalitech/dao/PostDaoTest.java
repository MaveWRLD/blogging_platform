package org.amalitech.dao;

import org.amalitech.models.Post;
import org.amalitech.util.RowMappers.PostRowMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostDaoTest {

    @Mock
    JdbcOperations jdbcTemplate;

    @InjectMocks
    private PostDao postDao;

    @Test
    void findAll_returnsEmptyList_whenNoPosts() {
        when(jdbcTemplate.query(any(String.class), any(PostRowMapper.class), anyInt(), anyInt()))
                .thenReturn(new ArrayList<>());

        List<Post> posts = postDao.findAll(0, 10);
        assertThat(posts).isNotNull().isEmpty();
    }

    @Test
    void save_savesPostAndReturnsId() {
        Post post = new Post();
        post.setTitle("title");
        post.setBody("body");

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(Object[].class)
        )).thenReturn(123);

        int id = postDao.save(post);

        assertThat(id).isEqualTo(123);

        verify(jdbcTemplate).queryForObject(
                anyString(),
                eq(Integer.class),
                any(Object[].class)
        );
    }

    @Test
    void findById_returnsPost() {
        Post post = new Post();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1))).thenReturn(List.of(post));
        Optional<Post> found = postDao.findById(1);
        assertThat(found).contains(post);
    }

    @Test
    void update_updatesPost() {
        Post post = new Post();
        post.setId(1);
        post.setTitle("new title");

        doReturn(1).when(jdbcTemplate).update(anyString(), any(Object[].class));

        postDao.update(post);

        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void delete_deletesPost() {
        doReturn(1).when(jdbcTemplate).update(anyString(), eq(1));
        postDao.delete(1);
        verify(jdbcTemplate).update(anyString(), eq(1));
    }
}