package org.amalitech.util.report;

import org.amalitech.config.MongoConnectionProvider;
import org.amalitech.dao.MongoCommentDao;
import org.amalitech.dao.PostDao;
import org.amalitech.models.SortOrder;
import org.amalitech.util.exception.NotFoundException;
import org.amalitech.util.exception.ValidationException;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PerformanceReporter {
    
    private static final int WARMUP_RUNS = 3;
    private static final int MEASUREMENT_RUNS = 10;
    
    private final PostDao postDao;
    private final MongoCommentDao commentDao;

    private String searchQuery = "java";
    private Set<Integer> tagIds = Set.of(1, 2);
    private Set<String> statuses = Set.of("published");
    private Integer authorId = 1;
    private int postId = 1;

    public PerformanceReporter() {
        this.postDao = new PostDao();
        this.commentDao = new MongoCommentDao(MongoConnectionProvider.getCommentsCollection());
    }
    
    public void configureTestData(String searchQuery, Set<Integer> tagIds, Set<String> statuses, Integer authorId, int postId) {
        this.searchQuery = searchQuery;
        this.tagIds = tagIds;
        this.statuses = statuses;
        this.authorId = authorId;
        this.postId = postId;
    }
    
    public Map<String, PerformanceMetrics> measurePerformance(boolean isWarmup) {
        Map<String, PerformanceMetrics> metrics = new LinkedHashMap<>();
        
        if (isWarmup) {
            for (int i = 0; i < WARMUP_RUNS; i++) {
                runAllQueries();
            }
        }

        List<Long> q1Times = new ArrayList<>();
        for (int i = 0; i < MEASUREMENT_RUNS; i++) {
            long start = System.nanoTime();
            postDao.search(null, Set.of(), Set.of(), null, SortOrder.NEWEST, 0, 20);
            long end = System.nanoTime();
            q1Times.add(end - start);
        }
        metrics.put("Q1: Latest Posts (no filters)", new PerformanceMetrics("Q1: Latest Posts (no filters)", q1Times));

        List<Long> q2Times = new ArrayList<>();
        for (int i = 0; i < MEASUREMENT_RUNS; i++) {
            long start = System.nanoTime();
            postDao.search(null, tagIds, statuses, authorId, SortOrder.NEWEST, 0, 20);
            long end = System.nanoTime();
            q2Times.add(end - start);
        }
        metrics.put("Q2: Posts by Tags + Filters", new PerformanceMetrics("Q2: Posts by Tags + Filters", q2Times));

        List<Long> q3Times = new ArrayList<>();
        for (int i = 0; i < MEASUREMENT_RUNS; i++) {
            long start = System.nanoTime();
            commentDao.findByPostId(postId);
            long end = System.nanoTime();
            q3Times.add(end - start);
        }
        metrics.put("Q3: Get Post Comments (MongoDB)", new PerformanceMetrics("Q3: Get Post Comments (MongoDB)", q3Times));

        return metrics;
    }
    
    private void runAllQueries() {
        try {
            postDao.findAll();
            commentDao.findByPostId(postId);
        } catch (Exception e) {
        }
    }
    
    public void generateComparisonReport(Map<String, PerformanceMetrics> preMetrics, 
                                        Map<String, PerformanceMetrics> postMetrics) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "performance_report_" + timestamp + ".txt";
        
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("=".repeat(80) + "\n");
            writer.write("PERFORMANCE COMPARISON REPORT\n");
            writer.write("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
            writer.write("=".repeat(80) + "\n\n");
            
            writer.write("TEST CONFIGURATION\n");
            writer.write("-".repeat(80) + "\n");
            writer.write("Search Query: " + searchQuery + "\n");
            writer.write("Tag IDs: " + tagIds + "\n");
            writer.write("Author ID: " + authorId + "\n");
            writer.write("Post ID: " + postId + "\n");
            writer.write("Warmup Runs: " + WARMUP_RUNS + "\n");
            writer.write("Measurement Runs: " + MEASUREMENT_RUNS + "\n\n");
            
            writer.write("DETAILED RESULTS\n");
            writer.write("=".repeat(80) + "\n\n");
            
            double totalImprovement = 0;
            int count = 0;
            
            for (Map.Entry<String, PerformanceMetrics> entry : preMetrics.entrySet()) {
                String queryName = entry.getKey();
                PerformanceMetrics pre = entry.getValue();
                PerformanceMetrics post = postMetrics.get(queryName);
                
                writer.write(queryName + "\n");
                writer.write("-".repeat(80) + "\n");
                
                writer.write(String.format("Pre-Optimization:\n"));
                writer.write(String.format("  Median: %.2f ms\n", pre.getMedianMs()));
                writer.write(String.format("  Average: %.2f ms\n", pre.getAvgMs()));
                writer.write(String.format("  Min: %.2f ms\n", pre.getMinMs()));
                writer.write(String.format("  Max: %.2f ms\n", pre.getMaxMs()));
                writer.write(String.format("  Std Dev: %.2f ms\n\n", pre.getStdDevMs()));
                
                if (post != null) {
                    writer.write(String.format("Post-Optimization:\n"));
                    writer.write(String.format("  Median: %.2f ms\n", post.getMedianMs()));
                    writer.write(String.format("  Average: %.2f ms\n", post.getAvgMs()));
                    writer.write(String.format("  Min: %.2f ms\n", post.getMinMs()));
                    writer.write(String.format("  Max: %.2f ms\n", post.getMaxMs()));
                    writer.write(String.format("  Std Dev: %.2f ms\n\n", post.getStdDevMs()));
                    
                    double improvement = post.calculateImprovement(pre);
                    totalImprovement += improvement;
                    count++;
                    
                    String status = improvement >= 30 ? "EXCELLENT" : improvement >= 10 ? "GOOD" : "MARGINAL";
                    writer.write(String.format("Improvement: %+.2f%% (%s)\n\n", improvement, status));
                }
                
                writer.write("\n");
            }
            
            writer.write("=".repeat(80) + "\n");
            writer.write("SUMMARY\n");
            writer.write("=".repeat(80) + "\n");
            
            if (count > 0) {
                double avgImprovement = totalImprovement / count;
                String overallStatus = avgImprovement >= 30 ? "EXCELLENT" : avgImprovement >= 10 ? "GOOD" : "MARGINAL";
                writer.write(String.format("Average Improvement: %.2f%% (%s)\n", avgImprovement, overallStatus));
                writer.write(String.format("Queries Tested: %d\n", count));
            }
            
            writer.write("\n" + "=".repeat(80) + "\n");

        } catch (IOException e) {
            throw new ValidationException("Failed to generate report");
        }
    }
}
