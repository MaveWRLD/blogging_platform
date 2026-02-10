package org.amalitech.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.amalitech.aspect.LoggingAspect;
import org.amalitech.dao.PostDao;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.models.Post;
import org.amalitech.service.PostService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SpringBootTest
class LoggingAspectTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostDao postDao;

    @MockitoBean
    private org.amalitech.interfaces.PostRepository postRepository;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setupLogCapture() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void cleanup() {
        listAppender.stop();
        listAppender.list.clear();
    }

    @Test
    void shouldLogEntryAndExit_forServiceMethod() {
        PostFilter filter = new PostFilter();
        when(postRepository.findPosts(anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(new Post()));

        postService.findPosts(filter);

        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).anyMatch(e ->
                e.getLevel() == Level.INFO &&
                        e.getFormattedMessage().contains("ENTRY ->") &&
                        e.getFormattedMessage().contains("PostService.findPosts") &&
                        e.getFormattedMessage().contains("arguments") // Checks arg serialization
        );
        assertThat(logs).anyMatch(e ->
                e.getLevel() == Level.INFO &&
                        e.getFormattedMessage().contains("EXIT ->") &&
                        e.getFormattedMessage().contains("PostService.findPosts")
        );
    }

    @Test
    void shouldLogException_whenServiceThrowsValidationException() {
        PostFilter invalidFilter = new PostFilter();
        invalidFilter.setPage(-1);

        assertThatThrownBy(() -> postService.findPosts(invalidFilter))
                .isInstanceOf(org.amalitech.util.exception.ValidationException.class);

        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).anyMatch(e ->
                e.getLevel() == Level.ERROR &&
                        e.getFormattedMessage().contains("EXCEPTION in") &&
                        e.getFormattedMessage().contains("PostService.findPosts") &&
                        e.getFormattedMessage().contains("ValidationException")
        );
    }

    @Test
    void shouldLogSlowDatabaseQuery_whenDaoOperationTakesOver1000ms() throws Throwable {
        when(postRepository.findById(anyInt())).thenAnswer(invocation -> {
            Thread.sleep(1200); // >1000ms
            return Optional.of(new Post());
        });

        postService.findPostById(42);

        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).anyMatch(e ->
                e.getLevel() == Level.WARN &&
                        e.getFormattedMessage().contains("SLOW QUERY DETECTED") &&
                        e.getFormattedMessage().contains("took") &&
                        e.getFormattedMessage().contains("PostDao.findById")
        );
        assertThat(logs).anyMatch(e ->
                e.getLevel() == Level.DEBUG &&
                        e.getFormattedMessage().contains("DB QUERY START ->") &&
                        e.getFormattedMessage().contains("PostDao.findById")
        );
        assertThat(logs).anyMatch(e ->
                e.getLevel() == Level.DEBUG &&
                        e.getFormattedMessage().contains("DB QUERY END ->") &&
                        e.getFormattedMessage().contains("completed in")
        );
    }

    @Test
    void shouldLogDatabaseFailure_whenDaoThrowsException() {
        when(postRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> postService.findPostById(100))
                .isInstanceOf(RuntimeException.class);

        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).anyMatch(e ->
                e.getLevel() == Level.ERROR &&
                        e.getFormattedMessage().contains("DB QUERY FAILED ->") &&
                        e.getFormattedMessage().contains("PostDao.findById") &&
                        e.getFormattedMessage().contains("after") &&
                        e.getFormattedMessage().contains("DB error")
        );
    }
}