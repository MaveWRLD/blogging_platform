
CREATE EXTENSION IF NOT EXISTS pg_trgm;

DROP INDEX IF EXISTS idx_posts_title_trgm;
CREATE INDEX idx_posts_title_trgm ON posts USING gin(title gin_trgm_ops);


DROP INDEX IF EXISTS idx_posts_body_trgm;
CREATE INDEX idx_posts_body_trgm ON posts USING gin(body gin_trgm_ops);

DROP INDEX IF EXISTS idx_posts_user_id;
CREATE INDEX idx_posts_user_id ON posts(user_id);

DROP INDEX IF EXISTS idx_comments_post_id;
CREATE INDEX idx_comments_post_id ON comments(post_id);

DROP INDEX IF EXISTS idx_posts_status;
CREATE INDEX idx_posts_status ON posts(status);

DROP INDEX IF EXISTS idx_posts_created_at;
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);

DROP INDEX IF EXISTS idx_posts_user_created;
CREATE INDEX idx_posts_user_created ON posts(user_id, created_at DESC);

DROP INDEX IF EXISTS idx_post_tags_tag_id;
CREATE INDEX idx_post_tags_tag_id ON post_tags(tag_id);

DROP INDEX IF EXISTS idx_post_tags_post_id;
CREATE INDEX idx_post_tags_post_id ON post_tags(post_id);

DROP INDEX IF EXISTS idx_users_email;
CREATE INDEX idx_users_email ON users(email);

ANALYZE posts;
ANALYZE comments;
ANALYZE post_tags;
ANALYZE users;

-- Rebuild indexes if needed (e.g., after bulk data changes):
-- REINDEX TABLE posts;

-- Monitor index bloat over time:
-- SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
-- FROM pg_tables WHERE tablename IN ('posts', 'comments', 'post_tags');

-- ================================================================================
-- ROLLBACK (if needed)
-- ================================================================================
-- To remove all optimization indexes:
/*
DROP INDEX IF EXISTS idx_posts_title_trgm;
DROP INDEX IF EXISTS idx_posts_body_trgm;
DROP INDEX IF EXISTS idx_posts_user_id;
DROP INDEX IF EXISTS idx_posts_status;
DROP INDEX IF EXISTS idx_posts_created_at;
DROP INDEX IF EXISTS idx_posts_user_created;
DROP INDEX IF EXISTS idx_post_tags_tag_id;
DROP INDEX IF EXISTS idx_post_tags_post_id;
DROP INDEX IF EXISTS idx_users_email;
*/

-- ================================================================================
-- COMPLETED
-- ================================================================================
SELECT 'Optimization indexes created successfully!' AS status;
