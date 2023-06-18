package fifiore.logmonitoring.core;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

class Display {

    private Display() {}

    static void alert(TrafficAlert alert) {
        StringBuilder stringBuilder = new StringBuilder();
        String endPeriod = dateFormatted(alert.getDate());
        if (alert.isAlert()) {
            stringBuilder.append("*** High traffic generated an alert - hits = ")
                    .append(alert.getHitCountAverage()).append(", triggered at ").append(endPeriod)
                    .append(" ***");
        } else {
            stringBuilder.append("*** Traffic back to normal at " + endPeriod + " ***");
        }
        LogStream.out(stringBuilder.toString());
    }

    static void metrics(WindowsMetrics metrics) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-----\n").append("Time window: ")
                .append(dateFormatted(metrics.getStartDate())).append(", ")
                .append(dateFormatted(metrics.getEndDate())).append("\n").append("Number of hits: ")
                .append(metrics.getHitNB()).append("\n");

        if (metrics.getHitNB() > 0) {
            mostHitSections(stringBuilder, metrics);
            countPerOperation(stringBuilder, metrics);
            successRate(stringBuilder, metrics);
        }
        LogStream.out(stringBuilder.toString());
    }

    private static String dateFormatted(long date) {
        String pattern = "yyyy/MM/dd HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);
        java.util.Date startDate = new java.util.Date(date * 1000);
        return df.format(startDate);
    }

    private static void mostHitSections(StringBuilder stringBuilder, WindowsMetrics metrics) {
        List<String> mostHitSection = metrics.getMostHitSections();
        if (mostHitSection.size() == 1) {
            stringBuilder.append("Most hit section: ");
        } else {
            stringBuilder.append("Most hit sections: ");
        }
        for (String section : mostHitSection) {
            stringBuilder.append(section);
            stringBuilder.append(" ");
        }

        stringBuilder.append(" (" + metrics.getMostHitSectionCount());
        if (metrics.getMostHitSectionCount() > 1) {
            stringBuilder.append(" times)\n");
        } else {
            stringBuilder.append(" time\n");
        }
    }

    private static void countPerOperation(StringBuilder stringBuilder, WindowsMetrics metrics) {
        stringBuilder.append("Operations: ");
        Arrays.stream(HttpVerb.Values.values()).filter(op -> op != HttpVerb.Values.NONE)
                .forEach(op -> {
                    int count = metrics.getOperationCount()[op.ordinal()];
                    if (count > 0) {
                        stringBuilder.append(op.name()).append("(").append(count).append(") ");
                    }
                });
        stringBuilder.append("\n");
    }

    private static void successRate(StringBuilder stringBuilder, WindowsMetrics metrics) {
        stringBuilder.append("Success rate: " + Math.round(metrics.getSuccessRate()) + "%\n")
                .append("Remote host with lowest success rate: ")
                .append(metrics.getLowestSuccessRateHost()).append(" (")
                .append(Math.round(metrics.getLowestSuccessRate())).append("%)");
    }
}


