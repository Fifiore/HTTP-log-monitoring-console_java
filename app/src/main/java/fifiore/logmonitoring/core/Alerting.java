package fifiore.logmonitoring.core;

import java.util.TreeMap;

class Alerting {

    private final MessageChannel<TrafficAlert> outputPipe;
    private int windowSize = 0;
    private int waitDelay = 0;
    private int threshold = 10;
    private long windowStartTime = 0;
    private boolean isOnAlert = false;
    private int cumulatedHitCount = 0;
    private boolean firstWindowComputed = false;

    // TreeMap + most recent date for the moving time window
    private long mostRecentDate = 0;
    private final TreeMap<Long, Integer> hitCount = new TreeMap<>();

    Alerting(MessageChannel<TrafficAlert> outputPipe, int windowSize, int waitDelay) {
        this.outputPipe = outputPipe;
        this.windowSize = windowSize;
        this.waitDelay = waitDelay;
    }

    void setTrafficAlertThreshold(int threshold) {
        this.threshold = threshold;
    }

    void pushLog(TrafficLog log) {
        if (log.getDate() == 0) {
            // invalid logs are ignored
            return;
        }
        if (mostRecentDate < log.getDate()) {
            mostRecentDate = log.getDate();
        }
        incrementCount(log.getDate());
        compute();
    }

    private void incrementCount(long date) {
        if (hitCount.containsKey(date)) {
            hitCount.merge(date, 1, Integer::sum);
        } else {
            hitCount.put(date, 1);
        }
    }

    private boolean isCompletePeriodStored() {
        // Wait a delay before considering the first date
        if (0 == windowStartTime && mostRecentDate - hitCount.firstKey() >= waitDelay) {
            windowStartTime = hitCount.firstKey();
        }
        // We want the complete period plus a delay to compute
        long lastWindowEndDate = windowStartTime + windowSize - 1;
        return windowStartTime != 0 && mostRecentDate - lastWindowEndDate >= waitDelay;
    }

    private void computeTrafficAlert() {

        while (isCompletePeriodStored()) {
            // First and last date in the window
            long startDate = windowStartTime;
            long endDate = windowStartTime + windowSize - 1;

            if (!firstWindowComputed) {
                computeFirstWindow(startDate, endDate);
                firstWindowComputed = true;
            } else {
                adaptCountToDateShift(startDate, endDate);
            }
            int hitCountAverage = cumulatedHitCount / windowSize;
            if (!isOnAlert && hitCountAverage >= threshold
                    || isOnAlert && hitCountAverage < threshold) {
                trafficAlertStatusChanged(hitCountAverage, endDate);
            }
            // Slide the window of 1 sc
            windowStartTime++;
        }
    }

    private void computeFirstWindow(long startDate, long endDate) {
        // First window: cumulate hits of every date received in the window
        hitCount.entrySet().stream()
                .filter(entry -> entry.getKey() >= startDate && entry.getKey() <= endDate)
                .forEach(entry -> cumulatedHitCount += entry.getValue());
    }

    private void adaptCountToDateShift(long startDate, long endDate) {
        // Remove hit counts for the date exiting the window (1 before the window)
        long exitingDate = startDate - 1;
        if (hitCount.containsKey(exitingDate)) {
            cumulatedHitCount -= hitCount.get(exitingDate);
        }
        // Add hit counts for date entering the window
        if (hitCount.containsKey(endDate)) {
            cumulatedHitCount += hitCount.get(endDate);
        }
    }

    private void trafficAlertStatusChanged(int hitCountAverage, long windowEndDate) {
        isOnAlert = !isOnAlert;
        TrafficAlert alert = TrafficAlert.builder().isAlert(isOnAlert)
                .hitCountAverage(hitCountAverage).date(windowEndDate).build();
        emitAlert(alert);
    }

    private void removeOldLogs() {
        if (hitCount.isEmpty()) {
            return;
        }
        hitCount.entrySet().removeIf(entry -> entry.getKey() < windowStartTime - 1);
    }

    private void emitAlert(TrafficAlert alert) {
        outputPipe.push(alert);
    }

    private void compute() {
        if (hitCount.isEmpty()) {
            return;
        }
        computeTrafficAlert();
        removeOldLogs();
    }

}
