package fifiore.logmonitoring.core;

public class LogStream {
    private LogStream() {}

    public static void out(String message) {
        System.out.println(message);
    }

    public static void out(int value) {
        System.out.println(value);
    }

    public static void err(String message) {
        System.err.println(message);
    }

    public static void err(Exception exception) {
        System.err.println(exception.getMessage());
    }
}
