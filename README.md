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
- Spring GraphQL**
- Spring Data JDBC** / JPA (PostgreSQL)
- Spring Data MongoDB
- Hibernate Validator
- PostgreSQL (main storage)
- MongoDB** (comments)
- HikariCP** connection pool
- MapStruct** (DTO ↔ Entity mapping)
- Lombok**
- jBCrypt** (password hashing)
- AspectJ AOP** — logging, performance monitoring, caching
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

- **REST + Swagger UI**: http://localhost:8080/swagger-ui.html
- **GraphQL Playground**: http://localhost:8080/playground
- **GraphiQL**: http://localhost:8080/graphiql
- **Altair GraphQL Client** (alternative): http://localhost:8080/altair

## 📚 API Overview

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

## Testing

- Unit tests (Mockito + AssertJ)
- Integration tests (@SpringBootTest)
- Aspect tests (logging, performance, caching)


### How to Use It

1. Create a file called `README.md` in your project root
2. Replace `https://github.com/MaveWRLD/blogging_platform/tree/feature/module-5` with your actual repo URL
3. Commit & push — GitHub will render it beautifully
