package org.amalitech.util.report;

import java.util.Arrays;
import java.util.List;

public class PerformanceMetrics {
    
    private final String queryName;
    private final List<Long> executionTimes;
    private final double medianMs;
    private final double avgMs;
    private final double minMs;
    private final double maxMs;
    private final double stdDevMs;
    
    public PerformanceMetrics(String queryName, List<Long> executionTimesNano) {
        this.queryName = queryName;
        this.executionTimes = executionTimesNano;
        
        double[] timesMs = executionTimesNano.stream()
            .mapToDouble(t -> t / 1_000_000.0)
            .toArray();
        
        Arrays.sort(timesMs);
        
        this.medianMs = calculateMedian(timesMs);
        this.avgMs = calculateAverage(timesMs);
        this.minMs = timesMs[0];
        this.maxMs = timesMs[timesMs.length - 1];
        this.stdDevMs = calculateStdDev(timesMs, avgMs);
    }
    
    private double calculateMedian(double[] sortedArray) {
        int mid = sortedArray.length / 2;
        if (sortedArray.length % 2 == 0) {
            return (sortedArray[mid - 1] + sortedArray[mid]) / 2.0;
        } else {
            return sortedArray[mid];
        }
    }
    
    private double calculateAverage(double[] array) {
        return Arrays.stream(array).average().orElse(0.0);
    }
    
    private double calculateStdDev(double[] array, double mean) {
        double variance = Arrays.stream(array)
            .map(val -> Math.pow(val - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }
    
    public double calculateImprovement(PerformanceMetrics baseline) {
        if (baseline.medianMs == 0) return 0.0;
        return ((baseline.medianMs - this.medianMs) / baseline.medianMs) * 100.0;
    }
    
    public String getQueryName() {
        return queryName;
    }
    
    public double getMedianMs() {
        return medianMs;
    }
    
    public double getAvgMs() {
        return avgMs;
    }
    
    public double getMinMs() {
        return minMs;
    }
    
    public double getMaxMs() {
        return maxMs;
    }
    
    public double getStdDevMs() {
        return stdDevMs;
    }
    
    @Override
    public String toString() {
        return String.format("%s: median=%.2fms, avg=%.2fms, min=%.2fms, max=%.2fms, stdDev=%.2fms",
            queryName, medianMs, avgMs, minMs, maxMs, stdDevMs);
    }
}
