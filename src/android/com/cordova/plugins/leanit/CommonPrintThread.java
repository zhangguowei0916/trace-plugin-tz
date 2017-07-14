package com.cordova.plugins.leanit;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.bixolon.printer.BixolonPrinter;
import com.cordova.plugins.leanit.TracePluginCommon;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;


/**
 * Created by admin on 2017/3/17.
 */
public class CommonPrintThread {
    private Context context;
    private String url;
    private String showMessage;

    public CommonPrintThread(Context context, String url) {
        this.context = context;
        this.url = url;
    }

    public static String getBlueToothAdapter() throws IllegalArgumentException {
        BluetoothAdapter blueadapter = BluetoothAdapter.getDefaultAdapter();  //获取已经保存过的设备信息
        if (null == blueadapter) {
            return "您的手机不支持蓝牙设备";
        } else if (!blueadapter.isEnabled()) {
            return "蓝牙设备未开启";
        } else {
            Set<BluetoothDevice> device = blueadapter.getBondedDevices();
            if (null == device || device.size() < 1) {
                return "蓝牙设备未配对打印机";
            }
        }

        return "";
    }

    public void print() {
        if (!TracePluginCommon.mIsConnected) {
            String checkStrLink = getBlueToothAdapter();
            if (null != checkStrLink && !"".equals( checkStrLink)) {
                showMessage = checkStrLink;
                showHandler.sendEmptyMessage(0);
            } else {
                showMessage = "请先连接蓝牙设备";
                showHandler.sendEmptyMessage(0);
            }
        } else {
            showHandler.sendEmptyMessage(2);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = null;
                    try {
                        bitmap = getBitMBitmap(url);
                        if (null == bitmap) {
                            showMessage = "二维码不存在";
                            showHandler.sendEmptyMessage(0);
                        } else {
                            TracePluginCommon.mBixolonPrinter.printBitmap(bitmap, BixolonPrinter.ALIGNMENT_CENTER, 450, 50, false, false, true);
                            TracePluginCommon.mBixolonPrinter.printText("中国中铁一局", 1, 16, 16, false);
                            TracePluginCommon.mBixolonPrinter.formFeed(true);
                        }
                    } catch (Exception e) {
                        showHandler.sendEmptyMessage(0);
                    }
                }
            }).start();
        }

    }

    public static Bitmap getBitMBitmap(String urlpath) {
        Bitmap map = null;
        try {
            URL url = new URL(urlpath);
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream in;
            in = conn.getInputStream();
            map = BitmapFactory.decodeStream(in);
            // TODO Auto-generated catch block
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public final Handler showHandler = new Handler(new Handler.Callback() {

        @SuppressWarnings("unchecked")
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Toast.makeText(context, showMessage, Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    Toast.makeText(context, "正在打印..", Toast.LENGTH_LONG).show();
            }
            return true;
        }

    });
}
