package fifiore.logmonitoring.core;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Monitoring {

    private static final char CSV_DELIMITER = ',';

    // Logs could arrive with a delay.
    // Wait {WAIT_DELAY} seconds before considering a date for analysis
    private static final int WAIT_DELAY = 4;

    private static final int METRIC_WINDOW = 10; // seconds
    private static final int TRAFFIC_ALERT_WINDOW = 120; // seconds

    private final MessageChannel<TrafficLog> logPipe = new MessageChannel<>();
    private final MessageChannel<WindowsMetrics> outputMetricsPipe = new MessageChannel<>();
    private final MessageChannel<TrafficAlert> outputAlertPipe = new MessageChannel<>();
    private final MetricsTracking metricsTracking;
    private final Alerting alerting;
    private final CsvLogReader reader;

    public Monitoring(String inputSource, int threshold) {
        metricsTracking = new MetricsTracking(outputMetricsPipe, METRIC_WINDOW, WAIT_DELAY);
        alerting = new Alerting(outputAlertPipe, TRAFFIC_ALERT_WINDOW, WAIT_DELAY);
        if (threshold > 0) {
            alerting.setTrafficAlertThreshold(threshold);
        }
        reader = new CsvLogReader(inputSource, CSV_DELIMITER);
    }

    public void execute() {
        ExecutorService executors = Executors.newFixedThreadPool(3);
        CompletableFuture<Void> displayMetrics =
                CompletableFuture.runAsync(this::displayMetricsWorker, executors);
        CompletableFuture<Void> displayAlerts =
                CompletableFuture.runAsync(this::displayAlertWorker, executors);
        CompletableFuture<Void> treatLogs = CompletableFuture.runAsync(this::logWorker, executors);

        // Read logs from stream (file or standard input)
        Optional<TrafficLog> log = reader.getLine();
        while (log.isPresent()) {
            logPipe.push(log.get());
            log = reader.getLine();
        }
        logPipe.close();

        CompletableFuture.allOf(displayMetrics, displayAlerts, treatLogs).join();
        executors.shutdown();
    }

    private void logWorker() {
        Optional<TrafficLog> log = logPipe.read();
        while (log.isPresent()) {
            metricsTracking.pushLog(log.get());
            alerting.pushLog(log.get());
            log = logPipe.read();
        }
        outputAlertPipe.close();
        outputMetricsPipe.close();
    }

    private void displayMetricsWorker() {
        Optional<WindowsMetrics> metrics = outputMetricsPipe.read();
        while (metrics.isPresent()) {
            Display.metrics(metrics.get());
            metrics = outputMetricsPipe.read();
        }
    }

    private void displayAlertWorker() {
        Optional<TrafficAlert> alert = outputAlertPipe.read();
        while (alert.isPresent()) {
            Display.alert(alert.get());
            alert = outputAlertPipe.read();
        }
    }
}

