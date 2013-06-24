package com.seeedstudio.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.http.util.ByteArrayBuffer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.seeedstudio.smartmeter.SmartMeterActivity;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "SmartMeterActivity";

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Head and Trail
    private static final byte[] COUNTER = { 0x37, 0x31, 0x20 };// 71
    // private static final byte[] COUNTER = { 7, 1};// 71
    private static final byte[] _40Hz = { 0x34, 0x30, 0x20 };
    private static final byte[] _38Hz = { 0x33, 0x38, 0x20 };
    private static final byte[] _36Hz = { 0x33, 0x36, 0x20 };
    private static final byte[] LEARN_HEAD = { 0x38, 0x33, 0x31, 0x30, 0x20 }; // 8310
                                                                               // +
                                                                               // space
    private static final byte[] LEARN_TRAIL = { 0x20, 0x39, 0x35, 0x36, 0x39 }; // sapce
                                                                                // +
                                                                                // 9569
    public static final String CMD_START = "83114769";
    public static final byte[] SEND_HEAD = { 0x38, 0x33, 0x31, 0x31, 0x20 }; // 8311
    public static final byte[] SEND_TRAIL = { 0x20, 0x34, 0x37, 0x36, 0x39 }; // 4769
    public static final byte[] SPACE = { 0x20 };
    public static final byte[] TEMP = { 0x20, 0x20, 0x20, 0x20, 0x20 };
    public static int SPACE_COUNTER = 0;
    public static final String compare40HZ = "40";
    public static final String compare38HZ = "38";
    public static final String compare37HZ = "37";

    public static byte[] target;
    public static ArrayList<byte[]> listData = new ArrayList<byte[]>();

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private ArrayList<Byte> mList = new ArrayList<Byte>();

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming
                                              // connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing
                                                  // connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote
                                                 // device

    private static final int _36HZ = 4;
    private static final int _38HZ = 5;
    private static final int _40HZ = 6;

    /**
     * Constructor. Prepares a new SmartMeterActivity session.
     * 
     * @param context
     *            The UI Activity Context
     * @param handler
     *            A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * 
     * @param state
     *            An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D)
            Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(SmartMeterActivity.MESSAGE_STATE_CHANGE, state,
                -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        if (D)
            Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * 
     * @param device
     *            The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D)
            Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * 
     * @param socket
     *            The BluetoothSocket on which the connection was made
     * @param device
     *            The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket,
            BluetoothDevice device) {
        if (D)
            Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one
        // device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler
                .obtainMessage(SmartMeterActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(SmartMeterActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D)
            Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * 
     * @param out
     *            The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public static byte[] getTarget() {
        return target;
    }

    public static void setTarget(byte[] target) {
        BluetoothChatService.target = target;
        if (target == null) {
            listData.clear();
        }
        listData.add(target);
    }

    public static ArrayList<byte[]> getListData() {
        return listData;
    }

    public static void setListData(ArrayList<byte[]> data) {
        if (data == null) {
            listData.clear();
        }
    }

    private byte[] cutData(byte[] buffer, int bytes) {
        Log.d(TAG, "cutData()");
        ByteArrayBuffer bab = new ByteArrayBuffer(bytes);

        bab.append(buffer, 0, bytes);
        return bab.toByteArray();
    }

    public void splitToSend(byte[] buffer, String frequency) {

        String tempfrequency = " 38 ";
        int counter = 0;

        // setting split parts
        int parts = 5;
        Log.d(TAG, "buffer length= " + buffer.length);
        if ((buffer.length) > 200 && (buffer.length < 300)) {
            parts = 8;
        } else if ((buffer.length) > 300 && (buffer.length < 400)) {
            parts = 11;
        } else if ((buffer.length) > 400 && (buffer.length < 500)) {
            parts = 14;
        } else if ((buffer.length) > 500 && (buffer.length < 600)) {
            parts = 17;
        } else if ((buffer.length) > 600) {
            parts = 20;
        }
        int array = buffer.length / parts; // 分段的区间长度
        Log.d(TAG, "array: " + array);
        Log.d(TAG, "frequency: " + frequency);

        // setting the frequency
        if (frequency.equals(compare40HZ)) {
            tempfrequency = " 40 ";
        } else if (frequency.equals(compare38HZ)) {
            tempfrequency = " 38 ";
        } else if (frequency.equals(compare37HZ)) {
            tempfrequency = " 37 ";
        }
        Log.d(TAG, "tempfrequency: " + tempfrequency);

        // counter the data number
        if (SPACE_COUNTER != 0) {
            counter = SPACE_COUNTER + 5;
        }

        // send some temp data to active the community
        write(TEMP);
        // start key: 83114769
        write(CMD_START.getBytes());
        // head key: 8311 (head)
        write(SEND_HEAD);
        // counter number(command length)
        // write(Integer.toString(counter).getBytes());
        // change the frequency
        write(tempfrequency.getBytes());
        // data
        subByteArray(buffer, 0, array);
        // trail key: 4769
        write(SEND_TRAIL);
    }

    private byte[] subByteArray(byte[] buffer, int begin, int count) {
        byte[] backByte = new byte[count];
        int splitPart;

        // buffer length too short to lower count,
        // transport all the buffer without split.
        if (buffer.length <= count) {
            count = buffer.length;
            backByte = new byte[count];
        }

        // get the split part which determine by
        // buffer length and the array(count argument).
        if (buffer.length % count == 0) {
            splitPart = buffer.length / count;
        } else {
            splitPart = buffer.length / count + 1;
        }

        for (int split = 0; split < splitPart;) {
            begin = count * split;
            split++;
            Log.d(TAG, "begin: " + begin);

            // the last part length less than "count", phase
            // get the really length.
            if ((buffer.length - begin) < count) {
                backByte = new byte[buffer.length - begin];
            }

            // for count, and then back it.
            for (int i = 0; i < count; i++) {
                // begin + i >= max length of array
                if ((begin + i) >= buffer.length) {
                    break;
                }
                backByte[i] = buffer[begin + i];
            }

            String message = new String(backByte, 0, backByte.length);
            Log.d(TAG, "backByte message: " + message);

            write(backByte);

            // return backByte;
        }
        return backByte;
    }

    // check the buffer whether is right or not.
    public boolean checkByteArray(byte[] list) {
        int sign = 0;
        int dataCounter = 3;
        // check index 0 of the list array if equals 0x10(New Line)
        byte[] temp = new byte[5];
        // jPlus is Offset of the array if list[0] == 0x10
        int jPlus;
        String fa = "Get trouble, try again";
        String ft = "Get command succeed";

        if (list.length < 13) {
            Log.d(TAG, "False");
            return false;
        } else {

            if (list[0] == (byte) (10)) {
                Log.d(TAG, "list[0] == New Line directive");
                jPlus = 1;
            } else {
                jPlus = 0;
            }

            // get the header
            for (int j = 0; j < 5; j++) {
                temp[j] = list[j + jPlus];
            }

            // get data counter number
            for (int i = 0; i < 10; i++) {
                if (SPACE[0] == list[i + jPlus]) {
                    sign++;
                }
                if (2 == sign) {
                    dataCounter = i - jPlus - LEARN_HEAD.length + 1;
                    Log.d(TAG, "dataCounter: " + dataCounter);
                    sign = 0;
                }
            }
            // head + data counter number + data + trail
            // Minus the head length, trial length and the data counter number.
            target = new byte[list.length - jPlus - LEARN_HEAD.length
                    - LEARN_TRAIL.length - dataCounter];
        }

        // condition header and trail
        sign = 0;
        for (int i = 0; i < 5; i++) {

            Log.d(TAG, "list[" + i + "]" + list[i]);
            Log.d(TAG, "LEARN_HEAD[" + i + "]" + LEARN_HEAD[i]);
            Log.d(TAG, "list[" + (list.length - (i + 1)) + "]"
                    + list[list.length - (i + 1)]);
            Log.d(TAG, "LEARN_TRAIL[" + (LEARN_TRAIL.length - (i + 1)) + "]"
                    + LEARN_TRAIL[LEARN_TRAIL.length - (i + 1)]);

            // condition the head and trail.
            if (LEARN_HEAD[i] == temp[i]
                    && LEARN_TRAIL[LEARN_TRAIL.length - (i + 1)] == list[list.length
                            - (i + 1)]) {
                sign++;
                Log.d(TAG, "sign: " + sign);
            }
        }

        if (sign == 5) {
            Log.d(TAG, "True");
            // Send a failure message back to the Activity
            // mHandler.obtainMessage(BluetoothChatService.MESSAGE_TOAST,
            // ft.getBytes().length, -1, ft.getBytes()).sendToTarget();

            // Assignment the data to target byte array
            // SPACE_COUNTER = 0;
            // for (int i = 0; i < target.length; i++) {
            // target[i] = list[LEARN_HEAD.length + dataCounter + jPlus + i];
            // if (target[i] == SPACE[0]) {
            // SPACE_COUNTER++;
            // Log.d(TAG, "space: " + SPACE_COUNTER);
            // }
            // Log.d(TAG, "target counter: " + i);
            // }

            String t = new String(list,
                    LEARN_HEAD.length + dataCounter + jPlus, list.length);
            target = t.getBytes();
            String data = new String(target, 0, target.length);
            Log.d(TAG, "data: " + data);
            mHandler.obtainMessage(SmartMeterActivity.MESSAGE_READ,
                    ft.getBytes().length, -1, ft.getBytes()).sendToTarget();
            return true;
        } else {
            Log.d(TAG, "False");
            mHandler.obtainMessage(SmartMeterActivity.MESSAGE_READ,
                    fa.getBytes().length, -1, fa.getBytes()).sendToTarget();
            return false;
        }
    }

    // public boolean checkByteArray(byte[] list) {
    // int sign = 0;
    // int dataCounter = 3;
    // String fa = "false";
    // String ft = "true";
    //
    // if (list.length < 13) {
    // Log.d(TAG, "False");
    // return false;
    // } else {
    // // get data counter number
    // for (int i = 0; i < 10; i++) {
    // if (SPACE[0] == list[i]) {
    // sign++;
    // }
    // if (2 == sign) {
    // dataCounter = i - LEARN_HEAD.length + 1;
    // Log.d(TAG, "dataCounter: " + dataCounter);
    // sign = 0;
    // }
    // }
    // // head + data counter number + data + trail
    // // Minus the head length, trial length and the data counter number.
    // target = new byte[list.length - LEARN_HEAD.length
    // - LEARN_TRAIL.length - dataCounter];
    // }
    //
    // sign = 0;
    // for (int i = 0; i < 5; i++) {
    //
    // // log
    // Log.d(TAG, "list[" + i + "]" + list[i]);
    // Log.d(TAG, "LEARN_HEAD[" + i + "]" + LEARN_HEAD[i]);
    // Log.d(TAG, "list[" + (list.length - (i + 1)) + "]"
    // + list[list.length - (i + 1)]);
    // Log.d(TAG, "LEARN_TRAIL[" + (LEARN_TRAIL.length - (i + 1)) + "]"
    // + LEARN_TRAIL[LEARN_TRAIL.length - (i + 1)]);
    //
    // // condition the head and trail.
    // if (LEARN_HEAD[i] == list[i]
    // && LEARN_TRAIL[LEARN_TRAIL.length - (i + 1)] == list[list.length
    // - (i + 1)]) {
    // sign++;
    // Log.d(TAG, "sign: " + sign);
    // }
    // }
    //
    // if (sign == 5) {
    // Log.d(TAG, "True");
    // // Assignment the data to target byte array
    // SPACE_COUNTER = 0;
    // for (int i = 0; i < target.length; i++) {
    // target[i] = list[LEARN_HEAD.length + dataCounter + i];
    // if (target[i] == SPACE[0]) {
    // SPACE_COUNTER++;
    // Log.d(TAG, "space: " + SPACE_COUNTER);
    // }
    // Log.d(TAG, "target counter: " + i);
    // }
    // String data = new String(target, 0, target.length);
    // Log.d(TAG, "data: " + data);
    // mHandler.obtainMessage(SmartMeterActivity.MESSAGE_READ,
    // ft.getBytes().length, -1, ft.getBytes()).sendToTarget();
    // return true;
    // } else {
    // Log.d(TAG, "False");
    // mHandler.obtainMessage(SmartMeterActivity.MESSAGE_READ,
    // fa.getBytes().length, -1, fa.getBytes()).sendToTarget();
    // return false;
    // }
    // }

    public byte[] decodeToSend(byte[] source, int Hz) {

        byte[] targetByteArray = new byte[4];
        if (!mList.isEmpty()) {
            mList.clear();
            // insert the package head to mList
            for (int i = 0; i < COUNTER.length; i++) {
                mList.add(COUNTER[i]);
                Log.d(TAG, "mList: " + mList.toString());
            }

            switch (Hz) {
            case _40HZ:
                for (int j = 0; j < _40Hz.length; j++) {
                    mList.add(_40Hz[j]);
                }
                break;
            case _38HZ:
                for (int j = 0; j < _38Hz.length; j++) {
                    mList.add(_38Hz[j]);
                }
            case _36HZ:
                for (int j = 0; j < _36Hz.length; j++) {
                    mList.add(_36Hz[j]);
                }
                break;
            }
        }

        for (int i = 0; i < mList.size(); i++) {
            targetByteArray[i] = mList.remove(i);
        }

        printByteArray(targetByteArray);
        return targetByteArray;
    }

    private void printByteArray(byte[] list) {
        if (list.length != 0) {
            for (int i = 0; i < 4; i++) {
                // System.out.print("list: [" + i + "]" + list[i]);
                Log.d(TAG, "printByteArray list: [" + i + "]" + list[i]);
            }
        }
    }

    /**
     * Get 7-bit ASCII character array from input String. The lower 7 bits of
     * each character in the input string is assumed to be the ASCII character
     * value.
     */
    private byte[] getAsciiBytes(String input) {
        char[] c = input.toCharArray();
        byte[] b = new byte[c.length];
        for (int i = 0; i < c.length; i++)
            b[i] = (byte) (c[i] & 0x007F);

        return b;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(SmartMeterActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(SmartMeterActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(SmartMeterActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(SmartMeterActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted (or
     * until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter
                        .listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D)
                Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate
                            // new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D)
                Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            if (D)
                Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a
     * device. It runs straight through; the connection either succeeds or
     * fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG,
                            "unable to close() socket during connection failure",
                            e2);
                }
                // Start the service over to restart listening mode
                BluetoothChatService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            byte[] temp;
            // ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytes;

//            int count = 22;
//            byte[] b = new byte[count];
            // while (count == 0) {
            // count = mmInStream.available();
            // }
            // byte[] b = new byte[count];
            // mmInStream.read(b);

            // Keep listening to the InputStream while connected
            while (true) {

                try {
                    Thread.sleep(50);
                    // while (count == 0) {
                    // count = mmInStream.available();
                    // }
                    // byte[] b = new byte[count];

//                    int readCount = 0; // 已经成功读取的字节的个数
//                    while (readCount < count) {
//                        readCount += mmInStream.read(b, readCount, count
//                                - readCount);
//                    }

                    // Read from the InputStream
                     bytes = mmInStream.read(buffer);

                    // mChat.setData(buffer);
                    // setTarget(buffer);
                     temp = cutData(buffer, bytes);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(SmartMeterActivity.MESSAGE_READ,
                            temp.length, -1, temp).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                // try {
                //
                // while ((bytes = mmInStream.read(buffer, 0, buffer.length)) >
                // -1) {
                //
                // try {
                // this.sleep(150);
                // } catch (InterruptedException e) {
                // // TODO Auto-generated catch block
                // e.printStackTrace();
                // }
                //
                // // write to baos
                // baos.write(buffer, 0, bytes);
                // Log.d(TAG, "bytes: " + bytes);
                //
                // baos.flush();
                // Log.d(TAG, "baos: " + baos.toString());
                //
                // byte[] message = baos.toByteArray();
                //
                // if (checkByteArray(message)) {
                // buffer = message;
                // baos.reset();
                // break;
                // }
                // }
                //
                // // Send the obtained bytes to the UI Activity
                // mHandler.obtainMessage(SmartMeterActivity.MESSAGE_READ,
                // buffer.length, -1, buffer).sendToTarget();
                // } catch (IOException e) {
                // Log.e(TAG, "disconnected", e);
                // connectionLost();
                // break;
                // }
            }
        }

        /**
         * Write to the connected OutStream.
         * 
         * @param buffer
         *            The bytes to write
         */
        public void write(byte[] buffer) {

            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(SmartMeterActivity.MESSAGE_WRITE, -1,
                        -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }

            // try {
            //
            // try {
            // ConnectedThread.sleep(50);
            // } catch (InterruptedException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
            //
            // mmOutStream.write(buffer, 0, buffer.length);
            // mmOutStream.flush();
            //
            // String message = new String(buffer, 0, buffer.length);
            // Log.d(TAG, "writing bytes: " + message);
            //
            // // Share the sent message back to the UI Activity
            // mHandler.obtainMessage(SmartMeterActivity.MESSAGE_WRITE, -1, -1,
            // buffer).sendToTarget();
            // // mmOutStream.close();
            // } catch (IOException e) {
            // Log.e(TAG, "Exception during write", e);
            // }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
