package org.amalitech.ui.controller;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.amalitech.util.report.PerformanceMetrics;
import org.amalitech.util.report.PerformanceReporter;
import org.amalitech.service.MetricsService;
import org.amalitech.ui.util.AppExecutors;
import org.amalitech.ui.util.DbTask;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

public class PerformanceTestController {

    @FXML private TextField searchQueryField;
    @FXML private TextField tagIdsField;
    @FXML private TextField authorIdField;
    @FXML private TextField postIdField;
    @FXML private ComboBox<String> testPhaseCombo;

    @FXML private Button startTestButton;
    @FXML private Button applyIndexesButton;
    @FXML private Button generateReportButton;

    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private TextArea logArea;

    @FXML private BarChart<String, Number> performanceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    @FXML private VBox resultsContainer;
    @FXML private Label summaryLabel;
    @FXML private TextArea findingsArea;

    private Map<String, PerformanceMetrics> preOptimizationMetrics;
    private Map<String, PerformanceMetrics> postOptimizationMetrics;
    private PerformanceReporter reporter;

    @FXML
    public void initialize() {
        reporter = new PerformanceReporter();

        testPhaseCombo.setItems(FXCollections.observableArrayList(
            "Pre-Optimization Baseline",
            "Post-Optimization Measurement",
            "Full Comparison (Pre + Post)"
        ));
        testPhaseCombo.getSelectionModel().select(0);

        searchQueryField.setText("java");
        tagIdsField.setText("1,2");
        authorIdField.setText("1");
        postIdField.setText("1");

        yAxis.setLabel("Execution Time (ms)");
        xAxis.setLabel("Query");
        performanceChart.setTitle("Query Performance Comparison");
        performanceChart.setAnimated(true);

        applyIndexesButton.setDisable(true);
        generateReportButton.setDisable(true);

        resultsContainer.setVisible(false);
        progressBar.setProgress(0);

        log("Performance Testing Framework initialized");
        log("Configure test parameters and click 'Start Test'");
    }

    @FXML
    private void handleStartTest() {
        String phase = testPhaseCombo.getValue();

        if (phase == null) {
            showAlert("Please select a test phase", Alert.AlertType.WARNING);
            return;
        }

        if (!validateInputs()) {
            return;
        }

        try {
            String searchQuery = searchQueryField.getText().trim();
            Set<Integer> tagIds = parseIntegerSet(tagIdsField.getText());
            Integer authorId = Integer.parseInt(authorIdField.getText().trim());
            int postId = Integer.parseInt(postIdField.getText().trim());

            reporter.configureTestData(searchQuery, tagIds, Set.of("published"), authorId, postId);

            disableInputs(true);
            clearResults();
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

            if (phase.startsWith("Pre-Optimization")) {
                runPreOptimizationTest();
            } else if (phase.startsWith("Post-Optimization")) {
                runPostOptimizationTest();
            } else {
                runFullComparisonTest();
            }

        } catch (NumberFormatException e) {
            showAlert("Invalid input: " + e.getMessage(), Alert.AlertType.ERROR);
            disableInputs(false);
        }
    }

    private void runPreOptimizationTest() {
        log("Starting PRE-OPTIMIZATION baseline measurement...");
        statusLabel.setText("Status: Running baseline tests...");

        DbTask<Map<String, PerformanceMetrics>> task = new DbTask<>(() -> {
            updateProgress(0.2, "Warming up JVM...");
            Map<String, PerformanceMetrics> metrics = reporter.measurePerformance(true);
            updateProgress(1.0, "Baseline measurement complete!");
            return metrics;
        });

        task.setOnSucceeded(event -> {
            preOptimizationMetrics = task.getValue();
            displayResults(preOptimizationMetrics, null);
            log("✓ Pre-optimization baseline captured");
            log("Next: Apply database indexes, then run post-optimization test");
            statusLabel.setText("Status: Baseline complete - Apply indexes next");
            applyIndexesButton.setDisable(false);
            disableInputs(false);
            progressBar.setProgress(1.0);
        });

        task.setOnFailed(event -> handleTaskFailure(task.getException()));

        AppExecutors.getDbExecutor().execute(task);
    }

    private void runPostOptimizationTest() {
        log("Starting POST-OPTIMIZATION measurement...");
        statusLabel.setText("Status: Running optimized tests...");

        DbTask<Map<String, PerformanceMetrics>> task = new DbTask<>(() -> {
            updateProgress(0.2, "Warming up JVM...");
            Map<String, PerformanceMetrics> metrics = reporter.measurePerformance(false);
            updateProgress(1.0, "Post-optimization measurement complete!");
            return metrics;
        });

        task.setOnSucceeded(event -> {
            postOptimizationMetrics = task.getValue();

            if (preOptimizationMetrics != null) {
                displayResults(preOptimizationMetrics, postOptimizationMetrics);
                generateReportButton.setDisable(false);
                log("✓ Post-optimization measurement complete");
                log("Ready to generate comparison report");
                statusLabel.setText("Status: Tests complete - Generate report");
            } else {
                displayResults(null, postOptimizationMetrics);
                log("⚠ Warning: No pre-optimization baseline found");
                log("Run pre-optimization test first for comparison");
                statusLabel.setText("Status: Post-optimization complete (no baseline)");
            }

            disableInputs(false);
            progressBar.setProgress(1.0);
        });

        task.setOnFailed(event -> handleTaskFailure(task.getException()));

        AppExecutors.getDbExecutor().execute(task);
    }

    private void runFullComparisonTest() {
        log("Starting FULL COMPARISON test...");
        statusLabel.setText("Status: Running full comparison...");

        DbTask<Void> task = new DbTask<>(() -> {
            updateProgress(0.1, "Phase 1/3: Pre-optimization baseline...");
            preOptimizationMetrics = reporter.measurePerformance(true);

            updateProgress(0.4, "Phase 2/3: Apply indexes manually, then continue...");
            Platform.runLater(() -> {
                var alert = getAlert();
                alert.showAndWait();

                DbTask<Map<String, PerformanceMetrics>> postTask = new DbTask<>(() -> {
                    updateProgress(0.7, "Phase 3/3: Post-optimization measurement...");
                    Map<String, PerformanceMetrics> metrics = reporter.measurePerformance(false);
                    updateProgress(1.0, "Comparison complete!");
                    return metrics;
                });

                postTask.setOnSucceeded(e -> {
                    postOptimizationMetrics = postTask.getValue();
                    displayResults(preOptimizationMetrics, postOptimizationMetrics);
                    generateReportButton.setDisable(false);
                    log("✓ Full comparison test complete");
                    statusLabel.setText("Status: Complete - Generate report");
                    disableInputs(false);
                    progressBar.setProgress(1.0);
                });

                postTask.setOnFailed(e -> handleTaskFailure(postTask.getException()));

                AppExecutors.getDbExecutor().execute(postTask);
            });

            return null;
        });

        task.setOnSucceeded(event -> {
        });

        task.setOnFailed(event -> handleTaskFailure(task.getException()));

        AppExecutors.getDbExecutor().execute(task);
    }

    private static Alert getAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Apply Database Indexes");
        alert.setHeaderText("Step 2/3: Apply Optimization Indexes");
        alert.setContentText(
            "Please run the following in your PostgreSQL client:\n\n" +
            "psql -d your_database -f src/main/resources/db/optimization_indexes.sql\n\n" +
            "Or manually execute the SQL script.\n\n" +
            "Click OK when indexes are applied."
        );
        return alert;
    }

    @FXML
    private void handleApplyIndexes() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Apply Database Indexes");
        alert.setHeaderText("Optimization Indexes");
        alert.setContentText(
            "Run the following command in your terminal:\n\n" +
            "psql -U your_user -d your_database -f src/main/resources/db/optimization_indexes.sql\n\n" +
            "Or copy the SQL from:\n" +
            "src/main/resources/db/optimization_indexes.sql\n\n" +
            "After applying, run the Post-Optimization test."
        );
        alert.showAndWait();

        log("Waiting for user to apply database indexes...");
    }

    @FXML
    private void handleGenerateReport() {
        if (preOptimizationMetrics == null || postOptimizationMetrics == null) {
            showAlert("Both pre and post optimization metrics required", Alert.AlertType.WARNING);
            return;
        }

        log("Generating comprehensive comparison report...");
        log("Collecting Micrometer metrics...");
        statusLabel.setText("Status: Generating report...");
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        DbTask<Void> task = new DbTask<>(() -> {
            reporter.generateComparisonReport(preOptimizationMetrics, postOptimizationMetrics);

            generateMicrometerMetricsLog();

            return null;
        });

        task.setOnSucceeded(event -> {
            log("✓ Report generated: performance_report.txt");
            log("✓ Micrometer metrics logged to console");
            statusLabel.setText("Status: Report saved to performance_report.txt");
            progressBar.setProgress(1.0);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Report Generated");
            alert.setHeaderText("Performance Report Complete");
            alert.setContentText("Report saved to: performance_report.txt\n\nMicrometer metrics logged to console.\nCheck project root directory.");
            alert.showAndWait();
        });

        task.setOnFailed(event -> handleTaskFailure(task.getException()));

        AppExecutors.getDbExecutor().execute(task);
    }

    private void generateMicrometerMetricsLog() {
        MeterRegistry registry = MetricsService.getRegistry();

        Platform.runLater(() -> {
            log("\n=== MICROMETER METRICS ===");

            registry.getMeters().stream()
                .filter(meter -> meter.getId().getName().startsWith("hikaricp"))
                .forEach(meter -> {
                    log(String.format("  %s: %s",
                        meter.getId().getName(),
                        formatMeterValue(meter)));
                });

            registry.getMeters().stream()
                .filter(meter -> meter.getId().getName().startsWith("jdbc.query"))
                .forEach(meter -> {
                    log(String.format("  %s: %s",
                        meter.getId().getName(),
                        formatMeterValue(meter)));
                });

            log("=========================\n");
        });
    }

    private String formatMeterValue(Meter meter) {
        return meter.measure().toString();
    }

    private void displayResults(Map<String, PerformanceMetrics> preMetrics,
                                Map<String, PerformanceMetrics> postMetrics) {
        resultsContainer.setVisible(true);
        performanceChart.getData().clear();

        XYChart.Series<String, Number> preSeries = new XYChart.Series<>();
        preSeries.setName("Pre-Optimization");

        XYChart.Series<String, Number> postSeries = new XYChart.Series<>();
        postSeries.setName("Post-Optimization");

        StringBuilder findings = new StringBuilder();
        double totalImprovement = 0;
        int count = 0;

        if (preMetrics != null) {
            for (Map.Entry<String, PerformanceMetrics> entry : preMetrics.entrySet()) {
                String queryName = entry.getKey().replace("Q1: ", "").replace("Q2: ", "").replace("Q3: ", "");
                PerformanceMetrics pre = entry.getValue();

                preSeries.getData().add(new XYChart.Data<>(queryName, pre.getMedianMs()));

                findings.append(String.format("%s\n  Pre:  %.2fms (±%.2fms)\n",
                    entry.getKey(), pre.getMedianMs(), pre.getStdDevMs()));

                if (postMetrics != null && postMetrics.containsKey(entry.getKey())) {
                    PerformanceMetrics post = postMetrics.get(entry.getKey());
                    postSeries.getData().add(new XYChart.Data<>(queryName, post.getMedianMs()));

                    double improvement = post.calculateImprovement(pre);
                    totalImprovement += improvement;
                    count++;

                    findings.append(String.format("  Post: %.2fms (±%.2fms)\n  Improvement: %+.2f%% (%s)\n\n",
                        post.getMedianMs(), post.getStdDevMs(), improvement,
                        improvement >= 30 ? "EXCELLENT" : improvement >= 10 ? "GOOD" : "MARGINAL"));
                } else {
                    findings.append("\n");
                }
            }
        }

        if (preMetrics != null) {
            performanceChart.getData().add(preSeries);
        }
        if (postMetrics != null) {
            performanceChart.getData().add(postSeries);
        }

        if (count > 0) {
            double avgImprovement = totalImprovement / count;
            String status = avgImprovement >= 30 ? "EXCELLENT" : avgImprovement >= 10 ? "GOOD" : "MARGINAL";
            summaryLabel.setText(String.format(
                "Average Improvement: %.2f%% (%s) | Queries Tested: %d",
                avgImprovement, status, count
            ));
            summaryLabel.setStyle(avgImprovement >= 30 ? "-fx-text-fill: #2ecc71;" : "-fx-text-fill: #f39c12;");
        } else {
            summaryLabel.setText("No comparison data available");
            summaryLabel.setStyle("-fx-text-fill: #95a5a6;");
        }

        findingsArea.setText(findings.toString());
    }

    private void updateProgress(double progress, String message) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            statusLabel.setText("Status: " + message);
            log(message);
        });
    }

    private void log(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.appendText("[" + timestamp + "] " + message + "\n");
        });
    }

    private void clearResults() {
        logArea.clear();
        performanceChart.getData().clear();
        findingsArea.clear();
        summaryLabel.setText("");
        resultsContainer.setVisible(false);
    }

    private boolean validateInputs() {
        if (searchQueryField.getText().trim().isEmpty()) {
            showAlert("Search query cannot be empty", Alert.AlertType.WARNING);
            return false;
        }

        try {
            parseIntegerSet(tagIdsField.getText());
            Integer.parseInt(authorIdField.getText().trim());
            Integer.parseInt(postIdField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Invalid number format: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }

        return true;
    }

    private Set<Integer> parseIntegerSet(String input) {
        Set<Integer> result = new java.util.HashSet<>();
        for (String s : input.split(",")) {
            result.add(Integer.parseInt(s.trim()));
        }
        return result;
    }

    private void disableInputs(boolean disable) {
        searchQueryField.setDisable(disable);
        tagIdsField.setDisable(disable);
        authorIdField.setDisable(disable);
        postIdField.setDisable(disable);
        testPhaseCombo.setDisable(disable);
        startTestButton.setDisable(disable);
    }

    private void handleTaskFailure(Throwable exception) {
        Platform.runLater(() -> {
            log("❌ ERROR: " + exception.getMessage());
            statusLabel.setText("Status: Error - Check logs");
            progressBar.setProgress(0);
            disableInputs(false);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Test Failed");
            alert.setHeaderText("Performance Test Error");
            alert.setContentText(
                "Error: " + exception.getMessage() + "\n\n" +
                "Troubleshooting:\n" +
                "1. Verify database connection in db.properties\n" +
                "2. Ensure test data exists (posts, comments, tags)\n" +
                "3. Check that IDs in test parameters are valid"
            );
            alert.showAndWait();
        });
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type.toString());
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
