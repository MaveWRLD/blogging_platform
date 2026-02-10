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

```graphql
# Get paginated posts with filter
query {
  posts(filter: { page: 0, size: 10, tag: "java" }) {
    posts { id title status commentCount }
    total
    totalPages
    hasNext
  }
}

# Get single post with author & comments
query {
  post(id: 1) {
    post { id title body status publishedAt }
    author { id username email }
    comments { id username body createdAt }
  }
}

# Create post (mutation)
mutation {
  createPost(input: { title: "My First Post", body: "...", status: "DRAFT" }) {
    id title status
  }
}
```

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

```markdown
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

AOP is enabled via:

```java
@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)  // if needed for CGLIB
public class Application { ... }
```

## Monitoring & Debugging

- **Recent logs endpoint** (admin only): `GET /api/admin/logs/recent?limit=100&level=ERROR`
- **Performance metrics** (if exposed): `GET /api/admin/performance/snapshot`
- **Logs** appear in console + captured in-memory for API access



## Testing

- Unit tests (Mockito + AssertJ)
- Integration tests (@SpringBootTest)
- Aspect tests (logging, performance, caching)


### How to Use It

1. Create a file called `README.md` in your project root
2. Replace `https://github.com/MaveWRLD/blogging_platform/tree/feature/module-5` with your actual repo URL
3. Commit & push — GitHub will render it beautifully
