package com.seeedstudio.smartmeter;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.util.Log;

import com.seeedstudio.smartmeter.LCDScreen.Mode;

public class SmartMeter {

    // debugging
    private static final boolean D = true;
    private static final String TAG = "SmartMeter";

    // static state
    public static final int SMART_METER = 10;
    public static final int SM_GET_DATA = 1;

    // LCD screen for display the data.
    private LCDScreen screen;
    // controller wheel model
    private ControllerWheelModel wheel;
    // handler for IPC with Main UI activity.
    private Handler mHandler;
    // parser for protocol.
    private Parser mParser;
    // timer for get data per 200ms
    private Timer timer;
    TimerTask task = null;
    // mode flag
    private Mode mode = null;
    // command will be send.
    private byte[] cmd = new byte[] {};

    // /////// temp for test ///////
    private static byte[] temp = null;

    public SmartMeter(LCDScreen screen, ControllerWheelModel wheel,
            Handler handler) {
        this.screen = screen;
        this.mHandler = handler;
        this.wheel = wheel;
        mParser = new Parser();

    }

    public void startWheel() {
        setState(wheel.getState());
    }

    // set up unit
    public void setV() {
        this.mode = Mode.V;
        setupMode();
    }

    public void setO() {
        this.mode = Mode.Ω;
        setupMode();
    }

    public void setMa() {
        this.mode = Mode.mA;
        setupMode();
    }

    public void setA() {
        this.mode = Mode.A;
        setupMode();
    }

    public void setState(int state) {
        switch (state) {
        case ControllerWheel.STATE_V:
            setV();
            break;
        case ControllerWheel.STATE_O:
            setO();
            break;
        case ControllerWheel.STATE_mA:
            setMa();
            break;
        case ControllerWheel.STATE_A:
            setA();
            break;
        case ControllerWheel.STATE_NONE:
            start();
        default:
            break;
        }
    }

    // set up smart meter status
    public void setSwitch(boolean isTurn) {
        screen.setTurn(isTurn);
    }

    public void setHold(boolean isHold) {
        screen.setHold(isHold);
    }

    public void setStop(boolean isStop) {
        // TODO for stop button
    }

    // **************************************************//
    // handler data and display it
    // **************************************************//
    public void add(byte[] data) {
        mParser.add(data);
    }

    public void display(byte[] data) {
        if (D)
            Log.d(TAG, "display()");

        if (mParser.isAvailable()) {
            byte[] temp = mParser.getSaveData();
            displayData(temp);
        } else {
            Utility.logging(TAG, "data is inavailable");
        }
    }

    private void displayData(byte[] data) {

        if (data.length < 6) {
            if (D)
                Log.d(TAG, "data length < 6");
            return;
        }
        int key = data[1] & 0xff;
        int units = data[5] & 0xff;
        int HighReal = (data[2] << 8) & 0xff00;
        int lowReal = data[3] & 0xff;
        int less = data[4] & 0xff;

        if (D) {
            Log.d(TAG, "key = " + key + "\n" + "units = " + units + "\n"
                    + "HighReal = " + HighReal + "\n" + "lowReal = " + lowReal
                    + "\n" + "less = " + less + "\n");

        }

        String sign = "";
        String unit = "";
        switch (units) {
        // V
        // case 129:
        // switch (units) {
        case 1:
            sign = "+";
            unit = "mV";
            screen.setMode(Mode.mV);
            break;
        case 2:
            sign = "+";
            unit = "V";
            screen.setMode(Mode.V);
            break;
        case 129:
            sign = "-";
            unit = "mV";
            screen.setMode(Mode.mV);
            break;
        case 130:
            sign = "-";
            unit = "V";
            screen.setMode(Mode.V);
            break;
        // }
        // break;
        // A
        // case 130:
        // switch (units) {
        case 4:
            sign = "+";
            unit = "A";
            screen.setMode(Mode.A);
            break;
        case 132:
            sign = "-";
            unit = "A";
            screen.setMode(Mode.A);
            break;
        // }
        // break;
        // mA
        // case 131:
        // switch (units) {
        case 3:
            sign = "+";
            unit = "mA";
            screen.setMode(Mode.mA);
            break;
        case 131:
            sign = "-";
            unit = "mA";
            screen.setMode(Mode.mA);
            break;
        // }
        // break;
        // R
        // case 132:
        // sign = "";
        // switch (units) {
        case 5:
            sign = "";
            unit = "Ω";
            screen.setMode(Mode.Ω);
            break;
        case 6:
            sign = "";
            unit = "kΩ";
            screen.setMode(Mode.kΩ);
            break;
        case 7:
            sign = "";
            unit = "mΩ";
            screen.setMode(Mode.mΩ);
            break;
        // }
        }

        int combineData = HighReal + lowReal;
        String all = sign + combineData + "." + getDecade(less);
        Double display = Double.valueOf(all);
        screen.setData(display);
        // updateLCD();
    }

    private String getDecade(int original) {
        String back = String.valueOf(original);

        if (0 < original && original < 10) {
            back = "0" + back;
        }
        if (100 <= original) {
            back = String.valueOf(original / 10);
        }
        // if (10 < original && original < 100) {
        // back = original / 100;
        // } else if (original >= 100) {
        // // 去商的个位数（用取摸）
        // back = original / 100;
        // }

        return back;
    }

    private void updateLCD() {
        // screen.updaterByTime();
    }

    // timetask for send command per 200ms
    private class CommandTask extends TimerTask {

        Handler mHandler;

        public CommandTask(Handler mHandler) {
            this.mHandler = mHandler;
        }

        @Override
        public void run() {
            mHandler.obtainMessage(SMART_METER, SM_GET_DATA, -1,
                    SmartMeter.this.cmd).sendToTarget();
            if (D)
                Log.d(TAG, "commandTask is on per 300ms");
        }
    }

    // controller interface
    private void setupMode() {

        switch (mode) {
        case V:
            cmd = mParser.encoder("".getBytes(), mParser.START_BIT,
                    mParser.STOP_BIT, mParser.voltage);
            break;
        case Ω:
            cmd = mParser.encoder("".getBytes(), mParser.START_BIT,
                    mParser.STOP_BIT, mParser.resistor);
            break;
        case mA:
            cmd = mParser.encoder("".getBytes(), mParser.START_BIT,
                    mParser.STOP_BIT, mParser.mCurrent);
            break;
        case A:
            cmd = mParser.encoder("".getBytes(), mParser.START_BIT,
                    mParser.STOP_BIT, mParser.current);
            break;
        default:
            break;
        }
        Log.d("SmartMeter Unit mode: >>>>>>>>>>>>>>>>>>>", mode.toString());
        screen.setMode(mode);
    }

    public void prepare() {
        screen.setData(000.0f);
    }

    public void start() {

        if (timer == null) {
            timer = new Timer();
        }

        task = new CommandTask(mHandler);
        timer.scheduleAtFixedRate(task, 0, 300);

    }

    public void release() {
        if (task != null) {
            task.cancel();
        }
    }
}
