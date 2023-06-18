package fifiore.logmonitoring.core;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class WindowsMetrics {
    // first date included
    private long startDate = 0;
    // last date included
    private long endDate = 0;
    private int hitNB = 0;
    private List<String> mostHitSections = new ArrayList<>();
    private int mostHitSectionCount = 0;
    private int[] operationCount = new int[5];
    private double successRate;
    private String lowestSuccessRateHost = "";
    private double lowestSuccessRate = 0;
}
