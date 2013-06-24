package com.seeedstudio.smartmeter;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;

public class LCDScreen {

    // update listener
    public interface onLCDUpdateListener {
        void lcdWidthChanged(int newWidth);

        void lcdDataUpdated();

        void lcdHoldMode(boolean isHold);

        void lcdChangeUnitMode(String unit);

        void lcdSwitch(boolean isTurn);
    }

    private List<onLCDUpdateListener> listeners = new ArrayList<onLCDUpdateListener>();

    // detection mode
    public enum Mode {
        mV, V, mA, A, Ω, kΩ, mΩ, mDefault
    }

    // unit and hold mode.
    private Mode mode = Mode.mDefault;
    private boolean isHold = false;
    private boolean isTurn = false;

    private double data = 000.0f;
    private Handler mHandler;

    // private Timer timer = new Timer();
    // private TimerTask updateTask = new TimerTask() {
    //
    // @Override
    // public void run() {
    // mHandler.post(new Runnable() {
    //
    // @Override
    // public void run() {
    // updaterByTime();
    // }
    // });
    // }
    // };

    public LCDScreen() {
        mHandler = new Handler();
        // timer.scheduleAtFixedRate(updateTask, 0, 100);

    }

    //
    // class UpdateTask extends TimerTask {
    //
    // @Override
    // public void run() {
    // updaterByTime();
    // }
    //
    // }

    public double getData() {
        return data;
    }

    public void setData(double data) {
        this.data = data;
        iterate();
    }

    public void onUpdateWidth(int width) {
        for (onLCDUpdateListener listener : listeners) {
            listener.lcdWidthChanged(width);
        }
        iterate();
    }

    // public void updaterByTime() {
    // iterate();
    // }

    public void addListener(onLCDUpdateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(onLCDUpdateListener listener) {
        listeners.remove(listener);
    }

    public void setMode(Mode mode) {

        this.mode = mode;
        iterate();
    }

    public boolean isHold() {
        return isHold;
    }

    public void setHold(boolean isHold) {
        this.isHold = isHold;
        iterate();
    }

    public boolean isTurn() {
        return isTurn;
    }

    public void setTurn(boolean isTurn) {
        this.isTurn = isTurn;
        iterate();
    }

    private void iterate() {

        // Log.d("LCDScreen", "LCDScreen iterate()");

        // data = data + 1;

        // if (data == 9999) {
        // data = 0;
        // }

        // 监听器变化
        for (onLCDUpdateListener listener : listeners) {
            listener.lcdDataUpdated();
            listener.lcdChangeUnitMode(mode.name());
            listener.lcdHoldMode(isHold());
            listener.lcdSwitch(isTurn());
        }
    }
}
