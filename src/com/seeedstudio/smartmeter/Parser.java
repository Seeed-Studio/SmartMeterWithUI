package com.seeedstudio.smartmeter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.util.ByteArrayBuffer;

/**
 * @author Seeed Studio
 * 
 */
public class Parser {
    // debugging
    private final static boolean D = false;
    private final static String TAG = "Parser";

    // **************************************************** //
    // static
    // **************************************************** //

    // // ////////////////////////////////////
    // start bit and stop bit, default is 'S0' and '/E'
    // packaging the data with header and trail
    public static byte[] START_BIT = new byte[] { 0x53, 0x00 };
    public static byte[] STOP_BIT = new byte[] { 0x2f, 0x45 };

    // ////////////////////////////////////
    // Smart Meter data flow
    // start bit + data_length + data_type + data + odd_check + stop bit
    // odevity
    public static byte[] SM_ODD = new byte[] { 0x00 };
    public static byte[] SM_EVEN = new byte[] { 0x01 };

    // units
    // 0x01:mV,0x02:V, 0x03:mA, 0x04:A, 0x05:Ω, 0x06: kΩ, 0x07: MΩ,
    public static byte[] mV = { 0x01 };
    public static byte[] V = { 0x02 };
    public static byte[] mA = { 0x03 };
    public static byte[] A = { 0x04 };
    public static byte[] Om = { 0x05 };
    public static byte[] kOm = { 0x06 };
    public static byte[] mOm = { 0x07 };
    // negative : 0x81:mV, 0x82:V, 0x83:mA, 0x84:A
    public static byte[] _mV = { (byte) 0x81 };
    public static byte[] _V = { (byte) 0x82 };
    public static byte[] _mA = { (byte) 0x83 };
    public static byte[] _A = { (byte) 0x84 };

    // smart meter value type: V,mv,A,mA,O
    // phone ask terminal for data
    public static byte[] voltage = { 0x01 };
    public static byte[] current = { 0x02 };
    public static byte[] mCurrent = { 0x03 };
    public static byte[] resistor = { 0x04 };

    // it notice what data will be back from terminal
    public static byte[] v_get = { (byte) 0x81 };
    public static byte[] c_get = { (byte) 0x82 };
    public static byte[] mC_get = { (byte) 0x83 };
    public static byte[] r_get = { (byte) 0x84 };

    // **************************************************** //
    // field number
    // **************************************************** //

    // temp to save the be parsed byte array
    private static byte[] saveData = null;
    // alt for static
    public static byte[] tempData = null;

    // for deal with data which be added
    private ArrayList<byte[]> byteArrayList = new ArrayList<byte[]>();
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // **************************************************** //
    // Constructor and setter, getter method
    // **************************************************** //

    /**
     * 
     * Constructor for IR Parser
     * 
     */
    public Parser() {
    }

    public static byte[] getSTART_BIT() {
        return START_BIT;
    }

    public static void setSTART_BIT(byte[] sTART_BIT) {
        START_BIT = sTART_BIT;
    }

    public static byte[] getSTOP_BIT() {
        return STOP_BIT;
    }

    public static void setSTOP_BIT(byte[] sTOP_BIT) {
        STOP_BIT = sTOP_BIT;
    }

    public byte[] getSaveData() {
        return saveData;
    }

    public void setSaveData(byte[] data) {
        saveData = data;
        // tempData = saveData;
        if (D)
            Utility.logging(TAG,
                    "setSaveData(byte[] saveData), save data length: "
                            + data.length);
    }

    // **************************************************** //
    // add data to parser and ready to encoding
    // **************************************************** //

    /**
     * Add the data to parser
     * 
     * @param data
     *            Data will be parser, byte[]
     */
    public void add(byte[] data) {
        if (byteArrayList != null) {
            if (D)
                Utility.logging(TAG, "add(byte[] data), Parser add byte[] data");
            byteArrayList.add(data);
        }
        // tempData = data;

        // int tempInt = 0;
        //
        // for (int i = 0; i < data.length; i++) {
        // tempInt = data[i] & 0xff;
        // Utility.logging(TAG, "parser static tempData Data[" + i + "]: "
        // + tempInt + ", data length: " + data.length);
        // }
    }

    /**
     * Add the data to parser
     * 
     * @param data
     *            Data will be parser, ArrayList<byte[]>
     */
    public void add(ArrayList<byte[]> data) {
        if (byteArrayList != null) {
            if (D)
                Utility.logging(TAG,
                        "add(byte[] data), Parser add Arrarylist<T> data");
            byteArrayList = data;
        }
    }

    /**
     * Deal with all data to be ready, whatever byte[] or ArrayList<byte[]>
     * 
     * @return
     */
    public byte[] preDecoder() {
        if (byteArrayList != null) {
            try {
                if (D)
                    Utility.logging(TAG,
                            "preDecoder(), Read data Array List length: "
                                    + byteArrayList.size());

                byte[] back = linkingData(byteArrayList);
                // byteArrayList.clear();

                if (D)
                    Utility.logging(TAG,
                            "preDecoder(), After Linked Data length: "
                                    + back.length);

                return back;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Compose all the data byte array together
     * 
     * @param dataList
     *            byte Array Data List
     * @return
     * @throws IOException
     */
    private byte[] linkingData(ArrayList<byte[]> dataList) throws IOException {

        byte[] data;
        if (dataList == null) {
            return null;
        }

        for (int i = 0; i < dataList.size(); i++) {
            baos.write(dataList.get(i));
        }

        baos.flush();
        data = baos.toByteArray();
        baos.reset();

        if (D)
            Utility.logging(TAG,
                    "linkingData(ArrayList<byte[]> dataList), Linked data: "
                            + data.length);

        // printData(data);
        return data;
    }

    /**
     * Check whether data is right
     * 
     * @return true for available, false otherwise
     */
    public boolean isAvailable() {
        if (byteArrayList == null) {
            return false;
        }

        if (decoder(preDecoder())) {
            return true;
        }

        return false;
    }

    public boolean prepare() {
        // if (byteArrayList != null) {
        // for (int i = 0; i < byteArrayList.size(); i++) {
        // if (decoder(byteArrayList.get(i))) return true;
        // }
        // }
        if (tempData != null) {
            if (decoder(tempData))
                return true;
        }
        return false;
    }

    private void printData(byte[] data) {
        int d;
        for (int i = 0; i < data.length; i++) {
            d = data[i] & 0xff;
            Utility.logging(TAG, "printData(), data[" + i + "]: " + d);
        }
    }

    // **************************************************** //
    // decoder
    // **************************************************** //

    /**
     * To parser the data, split the Header and Trail
     * 
     * @param data
     * @return if ok, back true, else return false
     */
    public boolean decoder(byte[] data) {
        if (data == null) {
            return false;
        }

        int sign = 0;
        byte[] temp = null;

        printData(data);

        // data.length-GET_HEADER.length-1 meanings the length without
        // GET_HEADER length, in case out index exception.
        for (int i = 0; i < data.length - START_BIT.length - 1; i++) {

            if (data[i] == START_BIT[0]) {
                sign++;

                if (D)
                    Utility.logging(TAG,
                            "decoder(byte[] data), get the first header data: "
                                    + data[i]);

                if (data[i + 1] == START_BIT[1]) {
                    sign++;
                    // cut down the header
                    temp = cutHeader(data, data.length - START_BIT.length - i);

                    if (D)
                        Utility.logging(TAG,
                                "decoder(byte[] data), temp array length: "
                                        + temp.length);

                    if (D)
                        Utility.logging(TAG,
                                "decoder(byte[] data), deal with the header: "
                                        + data[i] + data[i + 1] + data[i + 2]);
                }
            }
        }

        // not get the header
        if (temp == null) {
            if (D)
                Utility.logging(TAG, "decoder(byte[] data), not get the header");
            return false;
        }

        // get the trail and cut it down
        for (int i = 1; i < temp.length; i++) {
            if (temp[i - 1] == STOP_BIT[0]) {
                sign++;

                if (temp[i] == STOP_BIT[1]) {
                    sign++;
                    // temp = cutTrail(temp, temp.length - i - 2);
                    temp = cutTrail(temp, i - 1);

                    if (D)
                        Utility.logging(TAG,
                                "decoder(byte[] data), deal with the trail data");
                    break;
                }
            }
        }

        // it complete to parser the package
        if (sign == 4) {
            if (D)
                Utility.logging(TAG,
                        "decoder(byte[] data), it complete to parser the data package");
            byteArrayList.clear();
            setSaveData(temp);
            return true;
        } else {
            if (D)
                Utility.logging(TAG,
                        "decoder(byte[] data), it incomplete to parser the data package");
            byteArrayList.clear();
            return false;
        }
    }

    private byte[] cutHeader(byte[] data, int nBytes) {
        byte[] back;
        ByteArrayBuffer bab;

        bab = new ByteArrayBuffer(nBytes);
        bab.append(data, data.length - nBytes, nBytes);
        back = bab.toByteArray();

        return back;
    }

    private byte[] cutTrail(byte[] temp, int nBytes) {
        byte[] back;
        ByteArrayBuffer bab;

        bab = new ByteArrayBuffer(nBytes);
        bab.append(temp, 0, nBytes);
        back = bab.toByteArray();

        return back;
    }

    // **************************************************** //
    // encoder
    // **************************************************** //

    public byte[] encoder(byte[] data, byte[] header, byte[] trail,
            byte[] dataType) {
        if (data == null) {
            return null;
        }

        byte[] back;
        // start bit + data_length + data_type + data + odd_check + stop bit

        ByteArrayBuffer bab = new ByteArrayBuffer(header.length + data.length
                + 1 + dataType.length + SM_ODD.length + trail.length);

        bab.append(header, 0, header.length); // header
        bab.append(data.length + 1); // data_length
        bab.append(dataType, 0, dataType.length); // data_type
        bab.append(data, 0, data.length); // data
        bab.append(SM_ODD, 0, SM_ODD.length); // check
        bab.append(trail, 0, trail.length); // trail

        back = bab.toByteArray();

        if (D)
            Utility.logging(TAG, "encoder(), encoding data_length: "
                    + (data.length + 1));

        if (D) {
            Utility.logging(TAG, "encoder(), encoded data length: "
                    + back.length);
            for (int i = 0; i < back.length; i++) {
                Utility.logging(TAG, "encoder(), encoded data[" + i + "]: "
                        + back[i]);
            }
        }

        return back;
    }

}
