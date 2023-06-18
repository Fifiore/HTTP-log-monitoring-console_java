package fifiore.logmonitoring.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class CsvLogReader {

    private char delimiter = 0;
    private boolean firstRow = true;
    private BufferedReader buffer;
    private Map<Integer, Columns> columnIndexes = new HashMap<>();

    CsvLogReader(String inputSource, char delimiter) {
        this.delimiter = delimiter;

        if ("-".equals(inputSource)) {
            buffer = new BufferedReader(new InputStreamReader(System.in));
        } else {
            try {
                buffer = new BufferedReader(new FileReader(inputSource));
            } catch (Exception exception) {
                LogStream.err(exception);
            }
        }
    }

    Optional<TrafficLog> getLine() {
        if (firstRow) {
            String line = getStreamLine();
            if (line == null) {
                return Optional.empty();
            }
            readHeader(line);
            firstRow = false;
        }

        String line = getStreamLine();
        if (line == null) {
            return Optional.empty();
        }
        return Optional.of(readTrafficLog(line));
    }

    private void saveColumnIndex(String value, int index) {
        columnIndexes.put(index, Columns.fromText(value));
    }

    private void fillLog(TrafficLog log, String value, int valueIndex) {
        if (value == null) {
            return;
        }
        if (columnIndexes.containsKey(valueIndex)) {
            switch (columnIndexes.get(valueIndex)) {
                case DATE:
                    log.setDate(Long.parseLong(value));
                    break;
                case REQUEST:
                    log.setRequest(value);
                    break;
                case STATUS:
                    log.setStatus(Integer.parseInt(value));
                    break;
                case REMOTE_HOST:
                    log.setRemoteHost(value);
                    break;
                default:
                    break;
            }
        }

    }

    private String removeQuotes(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() >= 2 && value.charAt(0) == '\"'
                && value.charAt(value.length() - 1) == '\"') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private void readHeader(String line) {
        String[] values = line.split(String.valueOf(delimiter));
        for (int i = 0; i < values.length; i++) {
            saveColumnIndex(removeQuotes(values[i]), i);
        }
    }

    private TrafficLog readTrafficLog(String line) {
        TrafficLog log = new TrafficLog();
        String[] values = line.split(String.valueOf(delimiter));
        for (int i = 0; i < values.length; i++) {
            fillLog(log, removeQuotes(values[i]), i);
        }
        return log;
    }

    private String getStreamLine() {
        if (buffer != null) {
            try {
                String result = buffer.readLine();
                if (result == null) {
                    buffer.close();
                }
                return result;
            } catch (Exception exception) {
                LogStream.err(exception);
            }
        }
        return null;
    }

    private enum Columns {
        DATE, REQUEST, STATUS, REMOTE_HOST, NONE;

        static Columns fromText(String value) {
            if ("date".equals(value)) {
                return DATE;
            } else if ("request".equals(value)) {
                return REQUEST;
            } else if ("status".equals(value)) {
                return STATUS;
            } else if ("remotehost".equals(value)) {
                return REMOTE_HOST;
            }
            return NONE;
        }
    }
}
