CREATE TABLE users (
                       id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       username    VARCHAR NOT NULL UNIQUE,
                       email       VARCHAR NOT NULL UNIQUE,
                       password    VARCHAR NOT NULL,
                       role        VARCHAR,
                       status      VARCHAR,
                       created_at  TIMESTAMP DEFAULT NOW()
);

COMMENT ON COLUMN users.role IS 'ADMIN, AUTHOR, READER';
COMMENT ON COLUMN users.status IS 'ACTIVE, SUSPENDED';


CREATE TABLE posts (
                       id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       title       VARCHAR NOT NULL,
                       body        TEXT,
                       user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       status      VARCHAR,
                       created_at  TIMESTAMP DEFAULT NOW(),
                       updated_at  TIMESTAMP DEFAULT NOW()
);

COMMENT ON COLUMN posts.body IS 'Content of the post';
COMMENT ON COLUMN posts.status IS 'DRAFT, PUBLISHED';


CREATE TABLE comments (
                          id                  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          post_id             INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                          user_id             INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          parent_comment_id   INTEGER REFERENCES comments(id) ON DELETE SET NULL,
                          body                TEXT NOT NULL,
                          status              VARCHAR,
                          created_at          TIMESTAMP DEFAULT NOW()
);

COMMENT ON COLUMN comments.status IS 'VISIBLE, HIDDEN';

CREATE TABLE tags (
                      id      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      name    VARCHAR NOT NULL UNIQUE
);


CREATE TABLE post_tags (
                           post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                           tag_id  INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                           PRIMARY KEY (post_id, tag_id)
);


CREATE TABLE reviews (
                         id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         post_id     INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                         user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         rating      INTEGER CHECK (rating BETWEEN 1 AND 5),
                         review_text TEXT,
                         created_at  TIMESTAMP DEFAULT NOW(),
                         UNIQUE (post_id, user_id)
);

COMMENT ON COLUMN reviews.rating IS '1 to 5';
