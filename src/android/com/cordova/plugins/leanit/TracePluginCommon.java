/**
 * cordova is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 * <p>
 * Copyright (c) Matt Kane 2010
 * Copyright (c) 2011, IBM Corporation
 * Copyright (c) 2013, Maciej Nux Jaros
 */
package com.cordova.plugins.leanit;

import com.cordova.plugins.leanit.CommonPrintThread;

import android.app.AlertDialog;
import android.content.DialogInterface;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bixolon.printer.BixolonPrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


/**
 * Created by admin on 2017/7/11.
 */
public class TracePluginCommon extends CordovaPlugin {
    // Name of the connected device
    private String mConnectedDeviceName = null;

    public static final int REQUEST_CODE = 0x0ba7c0de;
    public static final String ACTION_COMPLETE_PROCESS_BITMAP = "com.bixolon.anction.COMPLETE_PROCESS_BITMAP";
    public static final String EXTRA_NAME_BITMAP_WIDTH = "BitmapWidth";
    public static final String EXTRA_NAME_BITMAP_HEIGHT = "BitmapHeight";
    public static final String EXTRA_NAME_BITMAP_PIXELS = "BitmapPixels";

    public static final String ACTION_GET_DEFINEED_NV_IMAGE_KEY_CODES = "com.bixolon.anction.GET_DEFINED_NV_IMAGE_KEY_CODES";
    public static final String ACTION_GET_MSR_TRACK_DATA = "com.bixolon.anction.GET_MSR_TRACK_DATA";
    public static final String EXTRA_NAME_NV_KEY_CODES = "NvKeyCodes";
    public static final String EXTRA_NAME_MSR_TRACK_DATA = "MsrTrackData";

    private static final String SCAN_INTENT = "com.cordova.plugins.leanit.qrcode.CaptureActivity";
    public static final int MESSAGE_START_WORK = Integer.MAX_VALUE - 2;
    public static final int MESSAGE_END_WORK = Integer.MAX_VALUE - 3;

    public static boolean mIsConnected = false;
    public static BixolonPrinter mBixolonPrinter;

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if (null == mBixolonPrinter) {
            mBixolonPrinter = new BixolonPrinter(cordova.getActivity(), mHandler, null);
        }
        if ("link".equals(action)) {
            mBixolonPrinter.findBluetoothPrinters();
            return true;
        } else if ("print".equals(action)) {
            new CommonPrintThread(cordova.getActivity(), args.getString(0)).print();
            return true;
        }else if ("scan".equals(action)) {
            scan(args);
        }
        return super.execute(action, args, callbackContext);
    }


    /**
     * Starts an intent to scan and decode a barcode.
     */
    public void scan(JSONArray args) {
        Intent intentScan = new Intent(SCAN_INTENT);
        intentScan.addCategory(Intent.CATEGORY_DEFAULT);

        // add config as intent extras
        if (args.length() > 0) {

            JSONObject obj;
            JSONArray names;
            String key;
            Object value;

            for (int i = 0; i < args.length(); i++) {

                try {
                    obj = args.getJSONObject(i);
                } catch (JSONException e) {
                    Log.i("CordovaLog", e.getLocalizedMessage());
                    continue;
                }

                names = obj.names();
                for (int j = 0; j < names.length(); j++) {
                    try {
                        key = names.getString(j);
                        value = obj.get(key);

                        if (value instanceof Integer) {
                            intentScan.putExtra(key, (Integer) value);
                        } else if (value instanceof String) {
                            intentScan.putExtra(key, (String) value);
                        }

                    } catch (JSONException e) {
                        Log.i("CordovaLog", e.getLocalizedMessage());
                        continue;
                    }
                }
            }

        }
        // avoid calling other phonegap apps
        intentScan.setPackage(this.cordova.getActivity().getApplicationContext().getPackageName());
        this.cordova.startActivityForResult((CordovaPlugin) this, intentScan, REQUEST_CODE);
    }

    private void dispatchMessage(Message msg) {
        switch (msg.arg1) {
            case BixolonPrinter.PROCESS_GET_STATUS:
                if (msg.arg2 == BixolonPrinter.STATUS_NORMAL) {
                    Toast.makeText(cordova.getActivity(), "No error", Toast.LENGTH_SHORT).show();
                } else {
                    StringBuffer buffer = new StringBuffer();
                    if ((msg.arg2 & BixolonPrinter.STATUS_COVER_OPEN) == BixolonPrinter.STATUS_COVER_OPEN) {
                        buffer.append("Cover is open.\n");
                    }
                    if ((msg.arg2 & BixolonPrinter.STATUS_PAPER_NOT_PRESENT) == BixolonPrinter.STATUS_PAPER_NOT_PRESENT) {
                        buffer.append("Paper end sensor: paper not present.\n");
                    }

                    Toast.makeText(cordova.getActivity(), buffer.toString(), Toast.LENGTH_SHORT).show();
                }
                break;

            case BixolonPrinter.PROCESS_GET_PRINTER_ID:
                Bundle data = msg.getData();
                Toast.makeText(cordova.getActivity(), data.getString(BixolonPrinter.KEY_STRING_PRINTER_ID), Toast.LENGTH_SHORT).show();
                break;

            case BixolonPrinter.PROCESS_GET_BS_CODE_PAGE:
                data = msg.getData();
                Toast.makeText(cordova.getActivity(), data.getString(BixolonPrinter.KEY_STRING_CODE_PAGE), Toast.LENGTH_SHORT).show();
                break;

            case BixolonPrinter.PROCESS_GET_PRINT_SPEED:
                switch (msg.arg2) {
                    case BixolonPrinter.PRINT_SPEED_LOW:
                        Toast.makeText(cordova.getActivity(), "Print speed: low", Toast.LENGTH_SHORT).show();
                        break;
                    case BixolonPrinter.PRINT_SPEED_MEDIUM:
                        Toast.makeText(cordova.getActivity(), "Print speed: medium", Toast.LENGTH_SHORT).show();
                        break;
                    case BixolonPrinter.PRINT_SPEED_HIGH:
                        Toast.makeText(cordova.getActivity(), "Print speed: high", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;

            case BixolonPrinter.PROCESS_GET_PRINT_DENSITY:
                switch (msg.arg2) {
                    case BixolonPrinter.PRINT_DENSITY_LIGHT:
                        Toast.makeText(cordova.getActivity(), "Print density: light", Toast.LENGTH_SHORT).show();
                        break;
                    case BixolonPrinter.PRINT_DENSITY_DEFAULT:
                        Toast.makeText(cordova.getActivity(), "Print density: default", Toast.LENGTH_SHORT).show();
                        break;
                    case BixolonPrinter.PRINT_DENSITY_DARK:
                        Toast.makeText(cordova.getActivity(), "Print density: dark", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;

            case BixolonPrinter.PROCESS_GET_POWER_SAVING_MODE:
                String text = "Power saving mode: ";
                if (msg.arg2 == 0) {
                    text += false;
                } else {
                    text += true + "\n(Power saving time: " + msg.arg2 + ")";
                }
                Toast.makeText(cordova.getActivity(), text, Toast.LENGTH_SHORT).show();
                break;

            case BixolonPrinter.PROCESS_AUTO_STATUS_BACK:
                StringBuffer buffer = new StringBuffer(0);
                if ((msg.arg2 & BixolonPrinter.AUTO_STATUS_COVER_OPEN) == BixolonPrinter.AUTO_STATUS_COVER_OPEN) {
                    buffer.append("Cover is open.\n");
                }
                if ((msg.arg2 & BixolonPrinter.AUTO_STATUS_NO_PAPER) == BixolonPrinter.AUTO_STATUS_NO_PAPER) {
                    buffer.append("Paper end sensor: no paper present.\n");
                }

                if (buffer.capacity() > 0) {
                    Toast.makeText(cordova.getActivity(), buffer.toString(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(cordova.getActivity(), "No error.", Toast.LENGTH_SHORT).show();
                }
                break;
            case BixolonPrinter.PROCESS_EXECUTE_DIRECT_IO:
                buffer = new StringBuffer();
                data = msg.getData();
                byte[] response = data.getByteArray(BixolonPrinter.KEY_STRING_DIRECT_IO);
                for (int i = 0; i < response.length && response[i] != 0; i++) {
                    buffer.append(Integer.toHexString(response[i]) + " ");
                }

                Toast.makeText(cordova.getActivity(), buffer.toString(), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private final Handler mHandler = new Handler(new Handler.Callback() {

        @SuppressWarnings("unchecked")
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case BixolonPrinter.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BixolonPrinter.STATE_CONNECTED:
                            Toast.makeText(cordova.getActivity(), "连接成功", Toast.LENGTH_SHORT).show();
                            mIsConnected = true;
                            break;

                        case BixolonPrinter.STATE_CONNECTING:
                            Toast.makeText(cordova.getActivity(), "正在连接...", Toast.LENGTH_SHORT).show();
                            break;

                        case BixolonPrinter.STATE_NONE:
                            mIsConnected = false;
//                           Toast.makeText(cordova.getActivity(), "连接失败....", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    return true;

                case BixolonPrinter.MESSAGE_WRITE:
                    switch (msg.arg1) {
                        case BixolonPrinter.PROCESS_SET_DOUBLE_BYTE_FONT:
                            mHandler.obtainMessage(MESSAGE_END_WORK).sendToTarget();

                            Toast.makeText(cordova.getActivity(), "Complete to set double byte font.", Toast.LENGTH_SHORT).show();
                            break;

                        case BixolonPrinter.PROCESS_DEFINE_NV_IMAGE:
                            mBixolonPrinter.getDefinedNvImageKeyCodes();
                            Toast.makeText(cordova.getActivity(), "Complete to define NV image", Toast.LENGTH_LONG).show();
                            break;

                        case BixolonPrinter.PROCESS_REMOVE_NV_IMAGE:
                            mBixolonPrinter.getDefinedNvImageKeyCodes();
                            Toast.makeText(cordova.getActivity(), "Complete to remove NV image", Toast.LENGTH_LONG).show();
                            break;

                        case BixolonPrinter.PROCESS_UPDATE_FIRMWARE:
                            mBixolonPrinter.disconnect();
                            Toast.makeText(cordova.getActivity(), "Complete to download firmware.\nPlease reboot the printer.", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    return true;

                case BixolonPrinter.MESSAGE_READ:
                    return true;

                case BixolonPrinter.MESSAGE_DEVICE_NAME:
                    // 提示打印机型号　暂时注掉
                    mConnectedDeviceName = msg.getData().getString(BixolonPrinter.KEY_STRING_DEVICE_NAME);
                    Toast.makeText(cordova.getActivity(), mConnectedDeviceName, Toast.LENGTH_LONG).show();
                    return true;

                case BixolonPrinter.MESSAGE_TOAST:
                    // unable to connect the device　暂时注掉
                    Toast.makeText(cordova.getActivity(), msg.getData().getString(BixolonPrinter.KEY_STRING_TOAST), Toast.LENGTH_SHORT).show();
                    return true;

                case BixolonPrinter.MESSAGE_BLUETOOTH_DEVICE_SET:
                    if (msg.obj == null) {
                        Toast.makeText(cordova.getActivity(), "没有配对的蓝牙打印机", Toast.LENGTH_SHORT).show();
                    } else {
//                        DialogManager.showBluetoothDialog(MainActivity.this, (Set<BluetoothDevice>) msg.obj);
                        Set<BluetoothDevice> pairedDevices = (Set<BluetoothDevice>) msg.obj;
                        final String[] items = new String[pairedDevices.size()];
                        int index = 0;
                        for (BluetoothDevice device : pairedDevices) {
                            items[index++] = device.getAddress();
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getActivity());
                        builder.create();
                        builder.setTitle("配对蓝牙打印机")
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        mBixolonPrinter.connect(items[which]);
                                        dialog.dismiss();
                                    }
                                });
                        builder.show();
                    }
                    return true;

                case BixolonPrinter.MESSAGE_PRINT_COMPLETE:
                    Toast.makeText(cordova.getActivity(), "打印完成", Toast.LENGTH_SHORT).show();
                    return true;

                case BixolonPrinter.MESSAGE_ERROR_INVALID_ARGUMENT:
                    Toast.makeText(cordova.getActivity(), "参数不正确", Toast.LENGTH_SHORT).show();
                    return true;

                case BixolonPrinter.MESSAGE_ERROR_NV_MEMORY_CAPACITY:
                    Toast.makeText(cordova.getActivity(), "NV memory capacity error", Toast.LENGTH_SHORT).show();
                    return true;

                case BixolonPrinter.MESSAGE_ERROR_OUT_OF_MEMORY:
                    Toast.makeText(cordova.getActivity(), "内存溢出", Toast.LENGTH_SHORT).show();
                    return true;

                case BixolonPrinter.MESSAGE_COMPLETE_PROCESS_BITMAP:
                    String text = "Complete to process bitmap.";
                    Bundle data = msg.getData();
                    byte[] value = data.getByteArray(BixolonPrinter.KEY_STRING_MONO_PIXELS);
                    if (value != null) {
                        Intent intent = new Intent();
                        intent.setAction(ACTION_COMPLETE_PROCESS_BITMAP);
                        intent.putExtra(EXTRA_NAME_BITMAP_WIDTH, msg.arg1);
                        intent.putExtra(EXTRA_NAME_BITMAP_HEIGHT, msg.arg2);
                        intent.putExtra(EXTRA_NAME_BITMAP_PIXELS, value);
                    }

                    Toast.makeText(cordova.getActivity(), text, Toast.LENGTH_SHORT).show();
                    return true;

                case MESSAGE_START_WORK:
                    Toast.makeText(cordova.getActivity(), "1", Toast.LENGTH_SHORT).show();
                    return true;

                case MESSAGE_END_WORK:
                    Toast.makeText(cordova.getActivity(), "2", Toast.LENGTH_SHORT).show();
                    return true;


                case BixolonPrinter.MESSAGE_NETWORK_DEVICE_SET:
                    if (msg.obj == null) {
                        Toast.makeText(cordova.getActivity(), "No connectable device", Toast.LENGTH_SHORT).show();
                    }
//					DialogManager.showNetworkDialog(cordova.getActivity(), (Set<String>) msg.obj);
                    return true;
            }
            return false;
        }
    });


    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsConnected = false;
        mBixolonPrinter.disconnect();
    }

}
