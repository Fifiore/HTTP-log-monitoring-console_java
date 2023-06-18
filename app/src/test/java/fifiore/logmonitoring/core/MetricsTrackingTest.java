package fifiore.logmonitoring.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class MetricsTrackingTest {

    private final String[][] inputLogs = {{"2", "200", "PUT /api/user HTTP/1.0", "10.0.0.3"},
            {"1", "200", "DELETE /api/user HTTP/1.0", "10.0.0.1"},
            {"1", "404", "GET /api/user HTTP/1.0", "10.0.0.2"},
            {"1", "200", "POST /report HTTP/1.0", "10.0.0.1"},
            {"2", "500", "PUT /api/user HTTP/1.0", "10.0.0.1"},
            {"3", "200", "PATCH /report HTTP/1.0", "10.0.0.1"},
            {"3", "200", "GET /api/user HTTP/1.0", "10.0.0.1"},
            {"4", "200", "GET /api/user HTTP/1.0", "10.0.0.2"},
            {"4", "200", "PUT /api/user HTTP/1.0", "10.0.0.3"},
            {"5", "200", "POST /report HTTP/1.0", "10.0.0.1"},
            {"5", "404", "GET /report HTTP/1.0", "10.0.0.4"},
            {"7", "500", "PUT /api/user HTTP/1.0", "10.0.0.1"},
            {"7", "200", "PUT /report HTTP/1.0", "10.0.0.1"},
            {"9", "200", "GET /report HTTP/1.0", "10.0.0.1"},
            {"10", "404", "GET /api/user HTTP/1.0", "10.0.0.2"},
            {"10", "200", "PUT /api/user HTTP/1.0", "10.0.0.3"},
            {"11", "200", "POST /report HTTP/1.0", "10.0.0.1"},
            {"11", "500", "PUT /api/user HTTP/1.0", "10.0.0.1"},
            {"12", "200", "PUT /report HTTP/1.0", "10.0.0.1"}};

    private void pushLog(MetricsTracking metricsTracking, String[] inputLog) {
        TrafficLog log = new TrafficLog();
        if (4 == inputLog.length) {
            log.setDate(Integer.parseInt(inputLog[0]));
            log.setStatus(Integer.parseInt(inputLog[1]));
            log.setRequest(inputLog[2]);
            log.setRemoteHost(inputLog[3]);
            metricsTracking.pushLog(log);
        }
    }

    @Test
    void execution() {
        MessageChannel<WindowsMetrics> outputPipe = new MessageChannel<>();
        MetricsTracking metricsTracking = new MetricsTracking(outputPipe, 3, 1);

        Arrays.stream(inputLogs).forEach(log -> pushLog(metricsTracking, log));
        outputPipe.close();

        List<WindowsMetrics> result = new ArrayList<>();
        Optional<WindowsMetrics> metrics = outputPipe.read();
        while (metrics.isPresent()) {
            result.add(metrics.get());
            metrics = outputPipe.read();
        }

        checkWindowsDates(result);
        checkHits(result);
        checkSucessRates(result);
    }

    private void checkWindowsDates(List<WindowsMetrics> metrics) {

        // The window [10,12] is ignored due to 1 second delay needed to accept the date 12

        assertEquals(3, metrics.size());

        assertEquals(1, metrics.get(0).getStartDate());
        assertEquals(3, metrics.get(0).getEndDate());

        assertEquals(4, metrics.get(1).getStartDate());
        assertEquals(6, metrics.get(1).getEndDate());

        assertEquals(7, metrics.get(2).getStartDate());
        assertEquals(9, metrics.get(2).getEndDate());
    }

    private void checkHits(List<WindowsMetrics> metrics) {

        assertEquals(7, metrics.get(0).getHitNB());
        assertEquals(4, metrics.get(1).getHitNB());
        assertEquals(3, metrics.get(2).getHitNB());

        assertEquals(1, metrics.get(0).getMostHitSections().size());
        assertEquals("/api", metrics.get(0).getMostHitSections().get(0));
        assertEquals(5, metrics.get(0).getMostHitSectionCount());

        assertEquals(2, metrics.get(1).getMostHitSections().size());
        assertEquals("/api", metrics.get(1).getMostHitSections().get(0));
        assertEquals("/report", metrics.get(1).getMostHitSections().get(1));
        assertEquals(2, metrics.get(1).getMostHitSectionCount());

        assertEquals(1, metrics.get(2).getMostHitSections().size());
        assertEquals("/report", metrics.get(2).getMostHitSections().get(0));
        assertEquals(2, metrics.get(2).getMostHitSectionCount());

        assertEquals(1, metrics.get(0).getOperationCount()[HttpVerb.Values.DELETE.ordinal()]);
        assertEquals(2, metrics.get(0).getOperationCount()[HttpVerb.Values.GET.ordinal()]);
        assertEquals(1, metrics.get(0).getOperationCount()[HttpVerb.Values.PATCH.ordinal()]);
        assertEquals(1, metrics.get(0).getOperationCount()[HttpVerb.Values.POST.ordinal()]);
        assertEquals(2, metrics.get(0).getOperationCount()[HttpVerb.Values.PUT.ordinal()]);
    }

    private void checkSucessRates(List<WindowsMetrics> metrics) {

        assertTrue(Math.abs(71.41 - metrics.get(0).getSuccessRate()) < 0.1);
        assertTrue(Math.abs(75 - metrics.get(1).getSuccessRate()) < 0.1);
        assertTrue(Math.abs(66.66 - metrics.get(2).getSuccessRate()) < 0.1);

        assertTrue(Math.abs(0 - metrics.get(0).getLowestSuccessRate()) < 0.1);
        assertTrue(Math.abs(0 - metrics.get(1).getLowestSuccessRate()) < 0.1);
        assertTrue(Math.abs(66.66 - metrics.get(2).getLowestSuccessRate()) < 0.1);

        assertEquals("10.0.0.2", metrics.get(0).getLowestSuccessRateHost());
        assertEquals("10.0.0.4", metrics.get(1).getLowestSuccessRateHost());
        assertEquals("10.0.0.1", metrics.get(2).getLowestSuccessRateHost());
    }
}
