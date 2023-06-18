package fifiore.logmonitoring.core;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
class TrafficAlert {
    @Builder.Default
    private long date = 0;
    @Builder.Default
    private boolean isAlert = false;
    @Builder.Default
    private int hitCountAverage = 0;
    @Builder.Default
    private String msg = ";";
}

