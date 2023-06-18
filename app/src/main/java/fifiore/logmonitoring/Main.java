package fifiore.logmonitoring;

import fifiore.logmonitoring.core.LogStream;
import fifiore.logmonitoring.core.Monitoring;

class Main {

    private static final String THRESHOLD_PARAM = "-alert_th=";

    public static void main(String[] args) {

        if (args.length > 2) {
            LogStream.err("Too many arguments");
            return;
        }

        execute(args);
    }

    private static void execute(String[] args) {
        int threshold = 0;
        String inputFile = "-";

        if (args.length > 0) {
            if (args[0].startsWith(THRESHOLD_PARAM)) {
                threshold = getThreshold(args[0]);
            } else {
                inputFile = args[0];
            }

            if (args.length == 2) {
                if (threshold > 0) {
                    inputFile = args[1];
                } else {
                    if (args[1].startsWith(THRESHOLD_PARAM)) {
                        threshold = getThreshold(args[1]);
                    } else {
                        LogStream.err("Invalid argument");
                    }

                }
            }
        }

        Monitoring monitoring = new Monitoring(inputFile, threshold);
        monitoring.execute();
    }

    private static int getThreshold(String arg) {
        try {
            String stringTh = arg.substring(THRESHOLD_PARAM.length());
            return Integer.parseInt(stringTh);

        } catch (Exception exception) {
            LogStream.err("Invalid alerting threshold argument");
        }
        return 0;
    }
}
