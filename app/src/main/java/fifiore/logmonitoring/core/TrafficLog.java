package fifiore.logmonitoring.core;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class TrafficLog {
    private long date = 0;
    private int status = 0;
    private String request = "";
    private String remoteHost = ";";
}

