```markdown
# Blogging Platform API

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green?style=for-the-badge&logo=spring&logoColor=white)
![GraphQL](https://img.shields.io/badge/GraphQL-E10098?style=for-the-badge&logo=graphql&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)

Modern Blogging Platform backend built with Spring Boot 3.4+.  
Exposes both RESTful and GraphQL APIs for managing users, blog posts, comments, tags, and reviews.  
Combines layered architecture, AOP (logging + performance monitoring), input validation, exception handling, caching, and efficient data retrieval patterns.

## Features

- Dual API support: REST endpoints + GraphQL interface
- User management — registration, authentication, roles
- Blog posts — create, update, publish, delete, pagination, search, trending
- Comments — threaded replies (MongoDB), CRUD operations
- Tags** — categorization and filtering
- AOP-powered** logging, performance monitoring & caching
- Efficient algorithms — pagination, full-text search, trending sorting
- OpenAPI 3 documentation (Swagger UI)
- PostgreSQL for relational data + MongoDB for comments
- Input validation, custom exceptions, secure password hashing

## 🛠 Tech Stack

- Java 21
- Spring Boot 3.4+
- Spring Web (REST)
- Spring GraphQL
- Spring Data JDBC** / JPA (PostgreSQL)
- Spring Data MongoDB
- Hibernate Validator
- PostgreSQL (main storage)
- MongoDB (comments)
- HikariCP connection pool
- MapStruct (DTO ↔ Entity mapping)
- Lombok
- jBCrypt (password hashing)
- AspectJ AOP — logging, performance monitoring, caching
- Mockito**, AssertJ, JUnit 5 (testing)
- Springdoc OpenAPI (Swagger UI)

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ & MongoDB 6+ (or Docker)
- Git

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/blogging-platform.git
cd blogging-platform
```

### 2. Configure environment

Create `src/main/resources/application.yml` (or use `application-dev.yml`):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/blogdb
    username: postgres
    password: yourpassword
  data:
    mongodb:
      uri: mongodb://localhost:27017/blog_comments
  graphql:
    graphiql:
      enabled: true
    playground:
      enabled: true
server:
  port: 8080
```

### 3. Run the application

```bash
# Development mode
mvn spring-boot:run

# Or build & run JAR
mvn clean package
java -jar target/blogging-platform-0.0.1-SNAPSHOT.jar
```

### 4. Access the APIs

- REST + Swagger UI**: http://localhost:8080/swagger-ui.html
- GraphQL Playground**: http://localhost:8080/playground
- GraphiQL**: http://localhost:8080/graphiql
- Altair GraphQL Client** (alternative): http://localhost:8080/altair

## API Overview

### REST Endpoints (examples)

- `POST /api/users` — Create user
- `POST /api/posts` — Create blog post
- `GET /api/posts?page=0&size=12` — Paginated posts
- `GET /api/posts/{id}` — Post with comments
- `GET /api/posts/trending?limit=10` — Trending posts
- `POST /api/comments` — Add comment

### GraphQL Examples

GraphQL API is available alongside REST; see the GraphQL schema and generated types under `src/main/resources/graphql` for the exact fields and operations. Example usage: use the GraphQL playground or a client (Altair, GraphiQL) to run queries and mutations such as fetching paginated posts, retrieving a single post with its author and comments, and creating posts via mutations. For up-to-date example queries see the `graphql/` resources or the `schema.graphqls` in the project.

## Project Structure

```
src/main/java/org/amalitech
├── aspect               # AOP: logging, performance, caching
├── controllers          # REST Controllers
├── graphQLResolver      # GraphQL Query & Mutation resolvers
├── service              # Business logic layer
├── dao                  # Data access (JDBC / Mongo)
├── dto                  # Data Transfer Objects
├── models               # Domain entities
├── util                 # Validators, exceptions, helpers
└── algorithm            # Trending sort, cache manager
```

# AOP (Aspect-Oriented Programming) in Blogging Platform

This project uses **Spring AOP** to implement cross-cutting concerns in a clean, modular way.  
The aspects handle **logging**, **performance monitoring**, and **caching** across the service, controller, and DAO layers.

## Aspects Overview

| Aspect                     | Purpose                              | Pointcuts Applied To                          | Key Features                                      |
|----------------------------|--------------------------------------|-----------------------------------------------|---------------------------------------------------|
| `LoggingAspect`            | Method entry/exit + exception logging | Controllers, Services, DAOs                   | Entry/exit logs with arguments, exception stack trace, slow query detection |
| `PerformanceMonitoringAspect` | Track execution time & statistics   | Services, Algorithm layer                     | Records call count, avg/min/max time, logs slow executions (>500ms) |
| `CachingAspect`            | Method result caching                | Methods annotated with `@Cacheable`           | Cache hit/miss logging, TTL support, key generation based on args |

All aspects are enabled automatically via component scanning and `@EnableAspectJAutoProxy`.

## 1. LoggingAspect

**Location**: `org.amalitech.aspect.LoggingAspect`

**Responsibilities**:
- Log method entry with arguments (serialized via Jackson)
- Log method exit
- Log exceptions with full stack trace
- Detect and warn about slow database operations (>1000ms)

**Pointcuts**:
- Controllers: `execution(* org.amalitech.controllers..*(..))`
- Services: `execution(* org.amalitech.service..*(..))`
- DAOs: `execution(* org.amalitech.dao..*(..))`

**Example log output**:
```
ENTRY -> PostService.findPosts(..) with arguments: [{"page":0,"size":10,"tag":"java"}]
EXIT -> PostService.findPosts(..)
DB QUERY START -> findById
DB QUERY END -> findById completed in 12ms
SLOW QUERY DETECTED -> findPosts took 1234ms
EXCEPTION in PostService.createPost(): ValidationException - Title cannot be empty
```

## 2. PerformanceMonitoringAspect

**Location**: `org.amalitech.aspect.PerformanceMonitoringAspect`

**Responsibilities**:
- Measure execution time of service & algorithm methods
- Maintain in-memory statistics (call count, average, min, max time)
- Log slow executions (>500ms) with current average
- Expose metrics snapshot via `snapshot()` method

**Pointcuts**:
- Services: `execution(* org.amalitech.service..*(..))`
- Algorithms: `execution(* org.amalitech.algorithm..*(..))`

**Features**:
- Thread-safe using `ConcurrentHashMap` + `AtomicLong`
- Warning logs for slow methods with avg time
- Public `snapshot()` method returns `PerformanceStatsDto` (call count, avg, min, max per method)

**Example log**:
```
PERFORMANCE -> PostService.findPosts(..) executed in 45ms
SLOW EXECUTION -> PostService.createPost(..) took 720ms (avg: 312.50ms)
```

## 3. CachingAspect

**Location**: `org.amalitech.aspect.CachingAspect`

**Responsibilities**:
- Intercept methods annotated with `@Cacheable`
- Check cache hit/miss
- Store results with TTL if cache miss
- Log cache operations (hit, miss, stored)

**Pointcut**:
- `@annotation(org.amalitech.annotation.Cacheable)`

**Cache key generation**:
- Prefix (if provided) + method name + hash of arguments
- Uses `Arrays.hashCode(args)` for simplicity

**Example log**:
```
CACHE HIT -> posts:findById:42
CACHE MISS -> posts:getTrendingPosts:10
CACHE STORED -> posts:getTrendingPosts:10 (TTL: 300s)
```

## Configuration

All aspects are Spring-managed beans (`@Component` + `@Aspect`).

AOP is enabled via the `@EnableAspectJAutoProxy` annotation on the application configuration (add it to the main `@SpringBootApplication` class or a `@Configuration` class).

## Monitoring & Debugging

- Recent logs endpoint (admin only): GET /api/admin/logs/recent (use query params: limit=100, level=ERROR)
- Performance metrics (if exposed): GET /api/admin/performance/snapshot
- Logs appear in console and are captured in-memory for API access



## Testing

- Unit tests (Mockito + AssertJ)
- Integration tests (@SpringBootTest)
- Aspect tests (logging, performance, caching)


### How to Use It

1. Create a file called `README.md` in your project root
2. Replace `https://github.com/MaveWRLD/blogging_platform/tree/feature/module-5` with your actual repo URL
3. Commit & push — GitHub will render it beautifully

## Repository Interfaces & Query Patterns

This project uses Spring Data (JPA / JDBC) for relational data (PostgreSQL) and Spring Data MongoDB for comment/thread storage. Repository interfaces live under `org.amalitech.dao` (or `org.amalitech.repository` depending on the module). Below are recommended patterns and short examples you can use/extend.

When to use which approach:
- Spring Data JPA repositories (or Spring Data JDBC) for relational entities: prefer method-name queries, `@Query` (JPQL) for non-trivial joins, and Criteria / QueryDSL for dynamic/filter queries.
- Spring Data MongoDB repositories for comments/thread structures that benefit from a document model and flexible schema.
- Use native queries only when performance characteristics require database-specific SQL.

Example: Post repository (JPA)

```java
// src/main/java/org/amalitech/dao/PostRepository.java
public interface PostRepository extends JpaRepository<Post, Long> {
    // Simple derived query - pageable & filter by status
    Page<Post> findByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    // Custom JPQL (complex joins / projections)
    @Query("SELECT p FROM Post p JOIN p.author a WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :term, '%')) ORDER BY p.publishedAt DESC")
    Page<Post> searchByTitleOrBody(@Param("term") String term, Pageable pageable);

    // Top trending by simple view-count heuristic
    List<Post> findTop10ByOrderByViewsDesc();
}
```

Example: Comment repository (MongoDB)

```java
// src/main/java/org/amalitech/dao/CommentRepository.java
public interface CommentRepository extends MongoRepository<Comment, String> {
    // Retrieve comments for a post in chronological order
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    // Find threaded replies by parent comment id
    List<Comment> findByParentCommentId(String parentCommentId);
}
```

Query logic notes:
- Prefer Pagination (Pageable) for list endpoints to avoid large-memory results.
- Keep read vs write queries separated: read-only queries marked at service layer with `@Transactional(readOnly = true)` to optimize performance.
- For complex, dynamic filters (multi-field search, tag intersection, date range), consider Criteria API or QueryDSL for JPA; for MongoDB use `Criteria` / `Query` in a custom repository implementation.

---

## Transaction Handling Strategy

Transactions are handled at the service layer. The project follows these conventions:

- Annotate service methods with `@Transactional` (Spring) to manage commit/rollback.
- Default propagation: `Propagation.REQUIRED` — joins an existing transaction or creates a new one.
- Use `readOnly = true` for methods that only read data (optimizes some JPA providers and avoids accidental writes).
- Use `rollbackFor = Exception.class` when business logic can throw checked exceptions that must trigger a rollback.
- For operations that must always commit independently (e.g., audit logging, remote calls), use `Propagation.REQUIRES_NEW`.

Examples

Read-only query method:

```java
@Service
public class PostService {
    // ...existing code...

    @Transactional(readOnly = true)
    public Page<Post> getPosts(Pageable pageable) {
        return postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, pageable);
    }
}
```

Write method with rollback rules:

```java
@Service
public class PostService {
    // ...existing code...

    @Transactional(rollbackFor = { Exception.class })
    public Post createPost(CreatePostDto dto) {
        // validate DTO -> may throw a checked ValidationException
        // persist Post entity and any dependent entities (tags, attachments)
        // if any exception is thrown, the transaction will be rolled back
    }
}
```

Independent transaction example (audit save runs in its own transaction):

```java
@Transactional
public void publishPost(Long postId) {
    // update post state
    // save audit in a separate transaction so it doesn't roll back with the post update
    saveAuditRecordInNewTransaction(...);
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveAuditRecordInNewTransaction(AuditRecord r) {
    auditRepository.save(r);
}
```

Transaction testing tips:
- Unit tests: mock repositories and assert transactional behavior by verifying interactions.
- Integration tests (`@SpringBootTest`): by default, Spring test framework wraps each test in a transaction and rolls back on completion — use this to test data isolation.
- To assert rollback behavior, write an integration test that triggers a failing operation and assert pre-existing state wasn't persisted.

---

## Caching Configuration & Testing

This project includes an aspect-powered caching mechanism (see `org.amalitech.aspect.CachingAspect`) which intercepts methods annotated with `@Cacheable` (project's custom annotation) and logs cache hits/misses. For production caching you can use Redis; for local/dev use an in-memory cache manager.

Recommended configuration examples are shown below.

Development (in-memory cache - simple, zero-ops for quick dev):

```java
@Bean
public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("posts", "users", "comments");
}
```

Production (Redis-backed cache configuration in `@Configuration`):

```yaml
# application-prod.yml
spring:
  cache:
    type: redis
  redis:
    host: redis-host
    port: 6379
```

And a Java config example for TTL and JSON serialization:

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofSeconds(300)) // default TTL 5 minutes
        .disableCachingNullValues()
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

    return RedisCacheManager.builder(factory)
        .cacheDefaults(config)
        .build();
}
```

If you prefer not to run Redis for tests, use a profile `test` that sets `spring.cache.type=simple` or provide a `@TestConfiguration` that registers a `ConcurrentMapCacheManager`.

Testing caching behavior

Unit tests (fast):
- Mock the repository and call the service method twice.
- Verify repository is invoked only once for the cached call (use Mockito.verify).
- Alternatively, use `@MockBean CacheManager` and assert cache interactions.

Example unit test sketch:

```java
@SpringBootTest
@ActiveProfiles("test")
class PostServiceCacheTest {
    @Autowired PostService postService;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("posts").clear();
    }

    @Test
    void cachedMethod_shouldReturnCachedResultOnSecondCall() {
        Post p1 = postService.getPost(1L);
        Post p2 = postService.getPost(1L);
        assertSame(p1, p2); // same instance from cache in simple cache manager
    }
}
```

Integration tests (real cache):
- Use Testcontainers to run a real Redis instance during integration tests and assert TTL/eviction behavior.
- Alternatively, use an embedded/standalone Redis test-instance for CI.

Notes about the AOP CachingAspect:
- The Aspect will log cache key generation and TTL; ensure your cache manager respects configured TTLs (RedisCacheManager shown above).
- When using custom `@Cacheable` annotation, confirm pointcut and annotation package names match the README examples (`org.amalitech.annotation.Cacheable`).

---

## Acceptance Criteria (Repository docs checklist)

- [x] Repository structure and typical query logic documented for PostgreSQL (JPA) and MongoDB.
- [x] Transaction handling strategies described with examples (`@Transactional`, propagation, readOnly, rollback behavior).
- [x] Caching configuration documented with sample Redis config and a dev in-memory fallback.
- [x] Testing steps for caching and transactions included (unit + integration recommendations).

