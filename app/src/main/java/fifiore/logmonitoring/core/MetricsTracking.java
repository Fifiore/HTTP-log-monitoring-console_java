package fifiore.logmonitoring.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
import lombok.Setter;

class MetricsTracking {

    private final MessageChannel<WindowsMetrics> outputPipe;
    private int windowSize = 0;
    private int waitDelay = 0;
    private long windowStartTime = 0;

    // TreeMap + most recent date for the moving time window
    private long mostRecentDate = 0;
    private final TreeMap<Long, List<TrafficLog>> logs = new TreeMap<>();

    MetricsTracking(MessageChannel<WindowsMetrics> outputPipe, int windowSize, int waitDelay) {
        this.outputPipe = outputPipe;
        this.windowSize = windowSize;
        this.waitDelay = waitDelay;
    }

    void pushLog(TrafficLog log) {
        if (log.getDate() == 0) {
            // invalid logs are ignored
            return;
        }
        if (mostRecentDate < log.getDate()) {
            mostRecentDate = log.getDate();
        }
        storeLog(log);
        compute();
    }

    private void storeLog(TrafficLog log) {
        if (logs.containsKey(log.getDate())) {
            logs.get(log.getDate()).add(log);
        } else {
            logs.put(log.getDate(), new ArrayList<>(List.of(log)));
        }
    }

    private boolean isCompletePeriodStored() {
        // Wait a delay before considering the first date
        if (0 == windowStartTime && mostRecentDate - logs.firstKey() >= waitDelay) {
            windowStartTime = logs.firstKey();
        }
        // We want the complete period plus a delay to compute
        long lastWindowEndDate = windowStartTime + windowSize - 1;
        return windowStartTime != 0 && mostRecentDate - lastWindowEndDate >= waitDelay;
    }

    private String extractSection(String request) {
        String[] elements = request.split(" ");
        if (elements.length != 3) {
            return "";
        }

        String[] sectionElements = elements[1].split("/");
        if (sectionElements.length < 2) {
            return "";
        }
        return "/" + sectionElements[1];
    }

    private void incrementHitPerSectionCount(Map<String, Integer> hitPerSection, String section) {
        if (hitPerSection.containsKey(section)) {
            hitPerSection.merge(section, 1, Integer::sum);
        } else {
            hitPerSection.put(section, 1);
        }
    }

    private void incrementHttpOperationsCount(WindowsMetrics metric, String request) {
        HttpVerb.Values httpVerb = HttpVerb.fromRequest(request);
        if (httpVerb != HttpVerb.Values.NONE) {
            metric.getOperationCount()[httpVerb.ordinal()]++;
        }
    }

    private void incrementStatusCount(Map<String, StatusCount> stats, String host,
            boolean success) {
        StatusCount status;
        if (stats.containsKey(host)) {
            status = stats.get(host);
        } else {
            status = new StatusCount();
            stats.put(host, status);
        }
        if (success) {
            status.setSuccessCount(status.getSuccessCount() + 1);
        } else {
            status.setFailureCount(status.getFailureCount() + 1);
        }
    }

    private void computeMostHitSections(Map<String, Integer> hitPerSection,
            WindowsMetrics metrics) {
        int highestCount = 0;

        for (Map.Entry<String, Integer> entry : hitPerSection.entrySet()) {
            metrics.setHitNB(metrics.getHitNB() + entry.getValue());
            if (highestCount < entry.getValue()) {
                highestCount = entry.getValue();
                metrics.getMostHitSections().clear();
                metrics.getMostHitSections().add(entry.getKey());
            } else if (highestCount == entry.getValue()) {
                metrics.getMostHitSections().add(entry.getKey());
            }
        }
        metrics.setMostHitSectionCount(highestCount);
    }

    private void computeSuccessRate(Map<String, StatusCount> statsPerRemoteHost,
            WindowsMetrics metrics) {
        int successCount = 0;
        int requestCount = 0;
        String lowestSuccessRateHost = "";
        double lowestSuccessRate = 100;
        for (Map.Entry<String, StatusCount> entry : statsPerRemoteHost.entrySet()) {
            int hostRequestCount =
                    entry.getValue().getSuccessCount() + entry.getValue().getFailureCount();
            double hostSuccessRate =
                    (double) (entry.getValue().getSuccessCount()) * 100 / hostRequestCount;
            if (hostSuccessRate < lowestSuccessRate) {
                lowestSuccessRate = hostSuccessRate;
                lowestSuccessRateHost = entry.getKey();
            }
            requestCount += hostRequestCount;
            successCount += entry.getValue().getSuccessCount();
        }
        if (requestCount > 0) {
            double successRate = (double) (successCount) * 100 / requestCount;
            metrics.setSuccessRate(successRate);
            metrics.setLowestSuccessRateHost(lowestSuccessRateHost);
            metrics.setLowestSuccessRate(lowestSuccessRate);
        }
    }

    private void processTrafficLog(TrafficLog trafficLog, WindowsMetrics metrics,
            Map<String, Integer> hitPerSection, Map<String, StatusCount> statsPerRemoteHost) {
        incrementHitPerSectionCount(hitPerSection, extractSection(trafficLog.getRequest()));
        // Aggregate hits per HTTP verb (PUT,GET...)
        incrementHttpOperationsCount(metrics, trafficLog.getRequest());
        if (trafficLog.getStatus() > 0) {
            incrementStatusCount(statsPerRemoteHost, trafficLog.getRemoteHost(),
                    trafficLog.getStatus() < 300);
        }
    }

    private void computeMetrics() {

        while (isCompletePeriodStored()) {
            WindowsMetrics metrics = new WindowsMetrics();
            // First and last date in the window
            metrics.setStartDate(windowStartTime);
            metrics.setEndDate(windowStartTime + windowSize - 1);

            // To aggregate hits per section
            Map<String, Integer> hitPerSection = new TreeMap<>();

            // To aggregate successes/failures by remote host
            Map<String, StatusCount> statsPerRemoteHost = new HashMap<>();

            logs.entrySet().stream()
                    .filter(entry -> entry.getKey() >= metrics.getStartDate()
                            && entry.getKey() <= metrics.getEndDate())
                    .flatMap(entry -> entry.getValue().stream())
                    .forEach(trafficLog -> processTrafficLog(trafficLog, metrics, hitPerSection,
                            statsPerRemoteHost));

            // Fill metrics
            computeMostHitSections(hitPerSection, metrics);
            computeSuccessRate(statsPerRemoteHost, metrics);

            emitMetrics(metrics);

            windowStartTime += windowSize;
        }
    }

    private void removeOldLogs() {
        if (logs.isEmpty()) {
            return;
        }
        logs.entrySet().removeIf(entry -> entry.getKey() < windowStartTime);
    }

    private void emitMetrics(WindowsMetrics metrics) {
        outputPipe.push(metrics);
    }

    private void compute() {
        if (logs.isEmpty()) {
            return;
        }
        computeMetrics();
        removeOldLogs();
    }

    @Getter
    @Setter
    private class StatusCount {
        int successCount = 0;
        int failureCount = 0;
    }
}
