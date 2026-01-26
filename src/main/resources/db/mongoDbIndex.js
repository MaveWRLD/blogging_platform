db.comments.createIndex({ postId: 1 })
db.comments.createIndex({ postId: 1, createdAt: -1 })
db.comments.createIndex({ postId: 1, createdAt: 1 })

db.comments.dropIndexes()