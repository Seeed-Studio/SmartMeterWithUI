package com.seeedstudio.smartmeter;

import java.util.ArrayList;
import java.util.List;

public class ControllerWheelModel {
    // update listener
    public interface onControllerWheelListener {
        int getState();

        void wheelChangeVmode(boolean isV);

        void wheelChangeOmmode(boolean isom);

        void wheelChangemAmode(boolean ismA);

        void wheelChangeAmode(boolean isA);

        void onItemTouch();
    }

    private List<onControllerWheelListener> listeners = new ArrayList<onControllerWheelListener>();

    private boolean isV = false;
    private boolean isOm = false;
    private boolean ismA = false;
    private boolean isA = false;
    private int state = -1;

    public int getState() {
        iterate();
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public boolean isV() {
        return isV;
    }

    public void setV(boolean isV) {
        this.isV = isV;
        iterate();
    }

    public boolean isOm() {
        return isOm;
    }

    public void setOm(boolean isOm) {
        this.isOm = isOm;
        iterate();
    }

    public boolean isIsmA() {
        return ismA;
    }

    public void setIsmA(boolean ismA) {
        this.ismA = ismA;
        iterate();
    }

    public boolean isA() {
        return isA;
    }

    public void setA(boolean isA) {
        this.isA = isA;
        iterate();
    }

    public void addListener(onControllerWheelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(onControllerWheelListener listener) {
        listeners.remove(listener);
    }

    private void iterate() {

        // 监听器变化
        for (onControllerWheelListener listener : listeners) {

            // listener.wheelDataUpdated();
            // listener.wheelChangeVmode(isV());
            // listener.wheelChangeOmmode(isOm());
            // listener.wheelChangemAmode(isIsmA());
            // listener.wheelChangeAmode(isA());
            setState(listener.getState());
            listener.onItemTouch();
        }
    }
}
