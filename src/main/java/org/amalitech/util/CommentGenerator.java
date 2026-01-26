package org.amalitech.util;

import org.amalitech.config.MongoConnectionProvider;
import org.amalitech.models.Comment;
import org.amalitech.dao.MongoCommentDao;

import java.time.LocalDateTime;
import java.util.Random;

public class CommentGenerator {

    private static final int MIN_COMMENTS_PER_POST = 0;
    private static final int MAX_COMMENTS_PER_POST = 10;  // Average ~5 per post → ~1M comments total
    private static final int TOTAL_POSTS = 200000;

    private static final String[] USER_NAMES = {
            "user1", "user2", "alice", "bob", "charlie", "david", "eve", "frank",
            "grace", "henry", "ivy", "jack", "kate", "leo", "mia", "noah"
    };

    private static final String[] COMMENT_TEMPLATES = {
            "Great post! Thanks for sharing.",
            "Interesting perspective. I agree.",
            "This helped me a lot. Well done!",
            "Could you explain more about %s?",
            "I have a similar experience. %s",
            "Awesome content! Keep it up.",
            "Not sure I understand the part about %s.",
            "Thanks, this is very informative.",
            "Disagree with the conclusion, but good read.",
            "Loved the examples. More please!"
    };

    private static final String[] RANDOM_WORDS = {
            "technology", "innovation", "future", "AI", "development", "coding",
            "learning", "experience", "ideas", "solutions"
    };

    public static void main(String[] args) {
        MongoCommentDao commentDao = new MongoCommentDao(MongoConnectionProvider.getCommentsCollection());
        Random random = new Random();

        for (int postId = 1; postId <= TOTAL_POSTS; postId++) {
            int numComments = random.nextInt(MAX_COMMENTS_PER_POST - MIN_COMMENTS_PER_POST + 1) + MIN_COMMENTS_PER_POST;

            for (int i = 0; i < numComments; i++) {
                Comment comment = new Comment();
                comment.setPostId(postId);
                comment.setUserName(USER_NAMES[random.nextInt(USER_NAMES.length)]);
                comment.setBody(generateRandomComment(random));
                comment.setParentCommentId(null);
                comment.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));  // Random past date

                commentDao.save(comment);
            }

            if (postId % 1000 == 0) {
                System.out.println("Generated comments for post " + postId + "/" + TOTAL_POSTS);
            }
        }

        System.out.println("Comment generation complete!");
    }

    private static String generateRandomComment(Random random) {
        String template = COMMENT_TEMPLATES[random.nextInt(COMMENT_TEMPLATES.length)];
        if (template.contains("%s")) {
            String word = RANDOM_WORDS[random.nextInt(RANDOM_WORDS.length)];
            template = String.format(template, word);
        }
        return template;
    }
}