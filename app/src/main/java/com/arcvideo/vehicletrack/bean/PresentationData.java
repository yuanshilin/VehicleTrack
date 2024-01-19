package com.arcvideo.vehicletrack.bean;

import java.io.Serializable;

public class PresentationData implements Serializable {
    private boolean plugged;
    private boolean IsHdmi;

    public PresentationData(boolean plugged, boolean isHdmi) {
        this.plugged = plugged;
        IsHdmi = isHdmi;
    }

    public boolean isPlugged() {
        return plugged;
    }

    public boolean isHdmi() {
        return IsHdmi;
    }
}
