package fifiore.logmonitoring.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class AlertingTest {

    private void pushLog(Alerting alerting, long date) {
        TrafficLog log = new TrafficLog();
        log.setDate(date);
        alerting.pushLog(log);
    }

    @Test
    void execution() {
        MessageChannel<TrafficAlert> outputPipe = new MessageChannel<>();
        Alerting alerting = new Alerting(outputPipe, 3, 2);
        alerting.setTrafficAlertThreshold(2);

        long[] dates = {2, 1, 2, 2, 3, 3, 2, 2, 4, 3, 5, 5, 6, 5, 8, 9, 10, 9, 9, 9, 10, 10, 12, 11,
                11, 11, 11, 15, 17, 16, 19};
        Arrays.stream(dates).forEach(date -> pushLog(alerting, date));
        outputPipe.close();

        List<TrafficAlert> result = new ArrayList<>();
        Optional<TrafficAlert> alert = outputPipe.read();
        while (alert.isPresent()) {
            result.add(alert.get());
            alert = outputPipe.read();
        }

        assertEquals(4, result.size());

        assertEquals(3, result.get(0).getDate());
        assertEquals(true, result.get(0).isAlert());

        assertEquals(6, result.get(1).getDate());
        assertEquals(false, result.get(1).isAlert());

        assertEquals(10, result.get(2).getDate());
        assertEquals(true, result.get(2).isAlert());

        assertEquals(13, result.get(3).getDate());
        assertEquals(false, result.get(3).isAlert());
    }
}
