package com.seeedstudio.smartmeter;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class Utility {
    // debugging switch
    public static boolean DEBUG = true;

    // log
    public static void logging(String tag, String text) {
        Log.d(tag, "+++>>  " + text );
    }

    // toast for android
    public static void toastShort(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
