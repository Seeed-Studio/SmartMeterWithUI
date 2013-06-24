package com.seeedstudio.smartmeter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.seeedstudio.bluetooth.BluetoothChatService;
import com.seeedstudio.bluetooth.DeviceListActivity;

@SuppressLint("HandlerLeak")
public class SmartMeterActivity extends Activity {

    // debugging
    private static final boolean D = true;
    private static final String TAG = "SmartMeterActivity";

    // **************************************************//
    // static
    // **************************************************//
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // bluetooth
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    // UI widget
    private Button turnSwitch, bluetoothSwitch, hold, stop, voltage, ampere,
            milliampere, ohm;
    private LCDView lcdView;
    private ControllerWheel mControllerWheel;
    // smart meter controller
    private SmartMeter smartMeter;
    // flag bit
    private boolean isTurn = false, isHold = false, isStop = false;
    private int wheelState = ControllerWheel.STATE_NONE;

    // **************************************************//
    // activity life cycle and UI setup
    // **************************************************//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (D)
            Utility.logging(TAG, "onCreate");

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // get lcd view and smart meter controller
        lcdView = (LCDView) findViewById(R.id.lcd_view);
        mControllerWheel = (ControllerWheel) findViewById(R.id.controllerWheel1);
        smartMeter = new SmartMeter(lcdView.getModel(),
                mControllerWheel.getModel(), mHandler);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (D)
            Utility.logging(TAG, "onStart");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null)
                setupUI();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (D)
            Utility.logging(TAG, "onResume");

        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't
            // started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null)
            mChatService.stop();
        if (D)
            Utility.logging(TAG, "onDestroy");
    }

    private void setupUI() {
        turnSwitch = (Button) findViewById(R.id.switch_turn);
        bluetoothSwitch = (Button) findViewById(R.id.bluetooth);
        hold = (Button) findViewById(R.id.hold);
        stop = (Button) findViewById(R.id.stop);
        turnSwitch.setOnClickListener(new ClickEvent());
        bluetoothSwitch.setOnClickListener(new ClickEvent());
        hold.setOnClickListener(new ClickEvent());
        stop.setOnClickListener(new ClickEvent());

        // // set mode
        // voltage = (Button) findViewById(R.id.voltage);
        // ampere = (Button) findViewById(R.id.ampere);
        // milliampere = (Button) findViewById(R.id.milliampere);
        // ohm = (Button) findViewById(R.id.ohm);
        // voltage.setOnClickListener(new ClickEvent());
        // ampere.setOnClickListener(new ClickEvent());
        // milliampere.setOnClickListener(new ClickEvent());
        // ohm.setOnClickListener(new ClickEvent());

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        // View vb = findViewById(R.id.relativeLayout2);
        mControllerWheel.setScreenHeight(dm.heightPixels);
        mControllerWheel.setScreenWidth(dm.widthPixels);

        // set up bluetooth
        mChatService = new BluetoothChatService(getApplicationContext(),
                mHandler);

    }

    class ClickEvent implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            int key = v.getId();
            if (!isTurn && (key != R.id.switch_turn)) {
                return;
            }
            switch (key) {
            case R.id.switch_turn:
                v.playSoundEffect(AudioManager.FX_KEY_CLICK);
                if (!isTurn) {
                    isTurn = true;
                    turnSwitch.setBackgroundResource(R.drawable.turn_on);
                } else {
                    isTurn = false;
                    turnSwitch.setBackgroundResource(R.drawable.turn_off);
                }
                smartMeter.setSwitch(isTurn);
                smartMeter.prepare();
                smartMeter.startWheel();
                break;
            case R.id.bluetooth:
                // reading to connect
                Intent serverIntent = new Intent(SmartMeterActivity.this,
                        DeviceListActivity.class);
                v.playSoundEffect(AudioManager.FX_KEY_CLICK);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                break;
            case R.id.hold:
                if (!isHold) {
                    isHold = true;
                    hold.setBackgroundResource(R.drawable.hold_on);
                } else {
                    isHold = false;
                    hold.setBackgroundResource(R.drawable.hold_off);
                }
                v.playSoundEffect(AudioManager.FX_KEY_CLICK);
                smartMeter.setHold(isHold);
                break;
            case R.id.stop:
                // if (!isStop) {
                // isStop = true;
                // stop.setBackgroundResource(R.drawable.stop_on);
                // } else {
                // isStop = false;
                // stop.setBackgroundResource(R.drawable.stop_off);
                // }
                // v.playSoundEffect(AudioManager.FX_KEY_CLICK);
                // smartMeter.setStop(isStop);
                // if (degree >= 360.0f) {
                // degree = 0.0f;
                // }
                // degree += 15.0f;
                // pointer.setDegree(degree);
                // pointer.invalidate();
                break;
            // case R.id.voltage:
            // smartMeter.setV();
            // flipMode(1);
            // v.playSoundEffect(AudioManager.FX_KEY_CLICK);
            // start();
            // break;
            // case R.id.ampere:
            // smartMeter.setA();
            // flipMode(2);
            // v.playSoundEffect(AudioManager.FX_KEY_CLICK);
            // start();
            // break;
            // case R.id.milliampere:
            // smartMeter.setMa();
            // flipMode(3);
            // v.playSoundEffect(AudioManager.FX_KEY_CLICK);
            // start();
            // break;
            // case R.id.ohm:
            // smartMeter.setO();
            // flipMode(4);
            // v.playSoundEffect(AudioManager.FX_KEY_CLICK);
            // start();
            // break;

            // case R.id.controllerWheel1:
            // if (D)
            // Log.d(TAG, "mControllerWheel onClick");
            // setState(mControllerWheel.getState());
            // break;
            default:
                break;
            }
        }

    }

    public void setState(int state) {
        if (wheelState != ControllerWheel.STATE_NONE && wheelState == state) {
            return;
        }

        switch (state) {
        case ControllerWheel.STATE_V:
            smartMeter.setV();
            mControllerWheel.playSoundEffect(AudioManager.FX_KEY_CLICK);
            start();
            break;
        case ControllerWheel.STATE_O:
            smartMeter.setO();
            mControllerWheel.playSoundEffect(AudioManager.FX_KEY_CLICK);
            start();
            break;
        case ControllerWheel.STATE_mA:
            smartMeter.setMa();
            mControllerWheel.playSoundEffect(AudioManager.FX_KEY_CLICK);
            start();
            break;
        case ControllerWheel.STATE_A:
            smartMeter.setA();
            mControllerWheel.playSoundEffect(AudioManager.FX_KEY_CLICK);
            start();
            break;
        default:
            break;
        }
    }

    private void start() {
        smartMeter.release();
        // smartMeter.prepare();
        smartMeter.start();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D)
            Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras().getString(
                        DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter
                        .getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupUI();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving,
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_smart_meter, menu);
        return true;
    }

    // **************************************************//
    // handler read and send
    // **************************************************//

    private void write(byte[] message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            // Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
            // .show();
            return;
        }

        // Check that there's actually something to send
        if (message.length > 0) {
            mChatService.write(message);
        }
    }

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if (D)
                    Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    // bluetooth is connected
                    bluetoothSwitch
                            .setBackgroundResource(R.drawable.bluetooth_on);
                    // if (isTurn) {
                    // smartMeter.prepare();
                    // }
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    Utility.toastShort(getApplicationContext(),
                            getString(R.string.connecting));
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    bluetoothSwitch
                            .setBackgroundResource(R.drawable.bluetooth_off);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                if (D)
                    Utility.logging(TAG, "send data: " + writeBuf.toString());
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // int tempInt2 = 0;
                smartMeter.add(readBuf);
                smartMeter.display(readBuf);
                break;
            case MESSAGE_DEVICE_NAME:
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(),
                        msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                        .show();
                break;
            case SmartMeter.SMART_METER:
                switch (msg.arg1) {
                case SmartMeter.SM_GET_DATA:
                    // TODO get state
                    if (D) {
                        Log.d(TAG, "Smartmeter get state");
                    }
                    setState(mControllerWheel.getmState());
                    byte[] cmd = (byte[]) msg.obj;
                    SmartMeterActivity.this.write(cmd);
                    // int tempInt = 0;
                    // for (int i = 0; i < cmd.length; i++) {
                    // tempInt = cmd[i] & 0xff;
                    // Utility.logging(TAG, "SM_GET_DATA Data[" + i + "]: "
                    // + tempInt);
                    // }
                    break;

                default:
                    break;
                }
            }
        }
    };

}
