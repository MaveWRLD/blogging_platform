# Performance Optimization Report
**Blogging Platform**

## Executive Summary

The blogging platform exhibited severe performance degradation under moderate concurrent load in its baseline state: mean response times in seconds, p99 latencies reaching 30+ seconds, frequent timeouts, and error rates up to 100% on write operations.

After implementing targeted optimizations — strategic database indexing, Caffeine in-memory caching, HikariCP connection pooling, thread-safe concurrent counters for views/likes/trending, and asynchronous processing — the system achieved dramatic improvements:

- Overall mean response time reduced from **1,871 ms → 6.3 ms** (~297× faster)
- p99 tail latency dropped from **30,012 ms → 68 ms** (~442× better)
- Throughput increased from **64.9 req/s → 112.8 req/s** (1.74× higher)
- Error rate eliminated entirely (**4.0% → 0.0%**)

These results confirm that the implemented changes successfully addressed database bottlenecks, connection exhaustion, synchronous blocking, and write contention — delivering a responsive, scalable foundation suitable for real user traffic.

## 1. Introduction

### Purpose
This report validates the performance improvements made to the blogging platform backend following a series of targeted optimizations. The goal was to eliminate long-tail latencies, reduce error rates under load, and increase overall throughput for core user flows (feed loading, trending discovery, post creation/updates, user profiles).

### Scope
Optimizations focused on:
- PostgreSQL indexing for posts, tags, and time-based queries
- MongoDB compound index on comments
- Caffeine caching for hot post entities
- HikariCP JDBC connection pool
- Concurrent data structures for view/like/trending counters
- Asynchronous offloading of non-critical tasks

### Test Environment
- Local development machine
- Java + Spring Boot application
- PostgreSQL (posts/tags) + MongoDB (comments)
- Identical load applied before and after optimizations

## 2. Optimizations Implemented

- **Database Indexing**  
  Compound indexes on posts (status + created_at DESC, status + like_count DESC + view_count DESC, user_id + created_at DESC), post_tags, and MongoDB comments (postId + createdAt DESC).  
  → Enables fast feed, trending, tag, and profile queries.

- **Caffeine In-Memory Caching**  
  Applied to post reads and updates.  
  → Reduces repeated database hits for popular content.

- **HikariCP Connection Pool**  
  High-performance JDBC pooling.  
  → Prevents connection starvation and reduces acquisition latency.

- **Concurrent Data Structures**  
  Atomic counters and thread-safe structures for view/like/trending score increments.  
  → Eliminates contention during bursty social interactions.

- **Asynchronous Processing**  
  Offloaded non-critical work from request threads.  
  → Keeps API responses fast even during background tasks.

## 3. Testing Methodology

### Tools
- **Load testing**: Apache JMeter
- **JVM profiling**: VisualVM (heap, threads, GC, CPU sampling)

### Test Plan
- Realistic blogging traffic simulation:
    - Create Post, Update Post, Get All Posts, Get Trending Posts,
      Get Post With Filtering, Get Post By Id, Get Post By UserId,
      Get User, User Login
- Total samples per run: **6,440 requests**
- Same JMeter test plan used for baseline and optimized versions
- Metrics captured: latency (mean, median, percentiles), throughput, error rate

### Profiling Workflow (added to development documentation)
1. Start application in dev mode
2. Attach VisualVM to the JVM process
3. Load JMeter test plan (.jmx)
4. Execute test (preferably non-GUI mode: `jmeter -n -t plan.jmx -l results.jtl`)
5. Monitor VisualVM during ramp-up and steady state
6. After completion: generate HTML report  
   `jmeter -g results.jtl -o report-folder`
7. Capture key graphs and VisualVM snapshots

## 4. Results

### 4.1 Overall Comparison

| Metric                | Before (Baseline) | After (Optimized) | Improvement                  |
|-----------------------|-------------------|-------------------|------------------------------|
| Mean Response Time    | 1,871 ms          | 6.3 ms            | ~297× faster                 |
| Median Response Time  | 5 ms              | 3 ms              | 40% faster                   |
| p95 Latency           | 5,579 ms          | 8 ms              | ~697× better                 |
| p99 Latency           | 30,012 ms         | 68 ms             | ~442× better                 |
| Max Latency           | 39,247 ms         | 1,095 ms          | ~97% reduction               |
| Throughput            | 64.9 req/s        | 112.8 req/s       | 1.74× higher                 |
| Error Rate            | 4.0%              | 0.0%              | Completely eliminated        |

#### Jmeter Response Time Graph Before and After Respectively

![Throughput – After](pre%20optimization/pre_jmeter_response_time_graph.png)

[INSERT SCREENSHOT: JMeter Statistics Table – After (or side-by-side if combined)]

### 4.2 Key Endpoint Improvements

| Endpoint                  | Mean Latency       | p95 Latency       | p99 Latency        | Throughput Gain    | Error Rate Change |
|---------------------------|--------------------|-------------------|--------------------|--------------------|-------------------|
| Get Trending Posts        | 280 ms → 3.2 ms    | 10 ms → 4 ms      | 13,222 ms → 29 ms  | 12.6 → 41.4 (+3.3×)| 0% → 0%           |
| Get All Posts             | 2,455 ms → 4.9 ms  | 4,340 ms → 4 ms   | 33,764 ms → 46 ms  | 12.1 → 40.2 (+3.3×)| 1.8% → 0%         |
| Get Post With Filtering   | 680 ms → 4.1 ms    | 11 ms → 4 ms      | 21,476 ms → 47 ms  | 12.6 → 41.4 (+3.3×)| ~0% → 0%          |
| Get Post By Id            | 28 ms → 4.2 ms     | 8 ms → 4 ms       | 48 ms → 43 ms      | 12.4 → 41.0 (+3.3×)| 0% → 0%           |
| Get Post By UserId        | 53 ms → 8.5 ms     | 15 ms → 4 ms      | 51 ms → 40 ms      | 6.2 → 20.7 (+3.3×) | 0% → 0%           |
| Get User                  | 8,094 ms → 7.4 ms  | 22,370 ms → 7 ms  | 30,023 ms → 64 ms  | 6.2 → 20.3 (+3.3×) | 3.3% → 0%         |
| Update Post               | 12,305 ms → 13.5 ms| 26,240 ms → 15 ms | 30,066 ms → 125 ms | 2.1 → 5.2 (+2.5×)  | 5% → 0%           |
| Create Post               | 12 ms (100% error) → 16 ms | 18 ms → 19 ms | 85 ms → 144 ms | 2.2 → 5.1 (+2.3×)  | 100% → 0%         |

### 4.3 Visual Evidence – Before vs After Comparison (Respectively)

#### Statistics Tables

![Statistics Table – Before Optimization](pre%20optimization/pre%20statistics.png)

![Statistics Table – After Optimization](post%20optimization/post%20optimization%20staticstics.png)

#### Response Time Percentiles Over Time

![Response Time Percentiles – Before (large spikes)](pre%20optimization/pre%20response%20time%20over%20time.png)

![Response Time Percentiles – After (flat & fast)](post%20optimization/post%20response%20time%20over%20time.png)

#### Throughput (Transactions per Second)

![Throughput – Before](pre%20optimization/pre%20transactions%20per%20second.png)

![Throughput – After](post%20optimization/post%20transactions%20per%20seconf%20throughput.png)

#### Errors

![Errors – Before (non-zero rate)](pre%20optimization/pre%20errors.png)
![Errors – After (zero)](post%20optimization/post%20errors.png)

#### VisualVM Monitor Tab
![Throughput – After](pre%20optimization/pre_visualvm.png)

![Throughput – After](post%20optimization/post_visualvm.png)

## 5. Key Observations

- Read-heavy operations (trending, home feed, tag filtering, user profiles) improved by 2–3 orders of magnitude — primarily due to indexing and caching.
- Tail latency (p95/p99) collapsed from tens of seconds to sub-100 ms, eliminating user-noticeable delays.
- Write operations (create/update post) became reliable and fast — concurrent structures + HikariCP removed contention and blocking.
- No errors remained under the tested load — timeouts and resource exhaustion were resolved.
- Throughput scaled consistently ~1.7–3.3× across most endpoints.
