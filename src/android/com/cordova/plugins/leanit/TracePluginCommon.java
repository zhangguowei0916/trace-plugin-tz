/**
 * cordova is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 * <p/>
 * Copyright (c) Matt Kane 2010
 * Copyright (c) 2011, IBM Corporation
 * Copyright (c) 2013, Maciej Nux Jaros
 */
package com.cordova.plugins.leanit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.bixolon.printer.BixolonPrinter;
import com.cordova.plugins.leanit.CommonPrintThread;
import com.cordova.plugins.leanit.qrcode.CaptureActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;


/**
 * Created by admin on 2017/7/11.
 */
public class TracePluginCommon extends CordovaPlugin {
    // Name of the connected device
    private String mConnectedDeviceName = null;
    private CallbackContext callbackContext;
    public static final int REQUEST_CODE = 0x0ba7c0de;
    public static final String ACTION_COMPLETE_PROCESS_BITMAP = "com.bixolon.anction.COMPLETE_PROCESS_BITMAP";
    public static final String EXTRA_NAME_BITMAP_WIDTH = "BitmapWidth";
    public static final String EXTRA_NAME_BITMAP_HEIGHT = "BitmapHeight";
    public static final String EXTRA_NAME_BITMAP_PIXELS = "BitmapPixels";

    public static final String ACTION_GET_DEFINEED_NV_IMAGE_KEY_CODES = "com.bixolon.anction.GET_DEFINED_NV_IMAGE_KEY_CODES";
    public static final String ACTION_GET_MSR_TRACK_DATA = "com.bixolon.anction.GET_MSR_TRACK_DATA";
    public static final String EXTRA_NAME_NV_KEY_CODES = "NvKeyCodes";
    public static final String EXTRA_NAME_MSR_TRACK_DATA = "MsrTrackData";

    public static final int MESSAGE_START_WORK = Integer.MAX_VALUE - 2;
    public static final int MESSAGE_END_WORK = Integer.MAX_VALUE - 3;

    public static boolean mIsConnected = false;
    public static BixolonPrinter mBixolonPrinter;
    ProgressDialog pBar = null;

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        //在这里为应用设置异常处理程序，然后我们的程序才能捕获未处理的异常
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(cordova.getActivity());

        if (null == mBixolonPrinter) {
            mBixolonPrinter = new BixolonPrinter(cordova.getActivity(), mHandler, null);
        }
        //连接打印机
        if ("link".equals(action)) {
            mBixolonPrinter.findBluetoothPrinters();
            return true;
        } else if ("print".equals(action)) {    //打印机打印二维码
            new CommonPrintThread(cordova.getActivity(), args.getString(0)).print();
            return true;
        } else if ("templatePrint".equals(action)) {    //模板打印
            templatePrint(new JSONObject(args.getString(0)));
        } else if ("scan".equals(action)) {//扫描二维码
            scan();
        } else if ("update".equals(action)) {//版本更新
            JSONObject myJsonObject = new JSONObject(args.getString(0));
            //获取对应的值
            update(myJsonObject.getString("versionCode"), myJsonObject.getString("description"), myJsonObject.getString("url"));
        } else if ("setting".equals(action)) {
            //获取对应的值
            setParamAndroid(new JSONObject(args.getString(0)));
        }
        return true;
    }

    /**
     * 设置
     */
    public void setParamAndroid(JSONObject jsonObject) {
        SharedPreferences sharedPreferences = cordova.getActivity().getSharedPreferences("setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();//获取编辑器
        try {
            editor.putString("crashUrl", jsonObject.getString("crashUrl"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        editor.commit();//提交修改
    }

    /**
     * 模板打印
     */
    public void templatePrint(JSONObject jsonObject) {
        try {
            String templateUrl = jsonObject.getString("templateUrl");
            Bitmap bitmap = CommonPrintThread.getBitMBitmap(templateUrl);
            JSONArray attrs = jsonObject.getJSONArray("attrs");
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            for (int i = 0; i < attrs.length(); i++) {
                JSONObject object = attrs.get(i);
                int x = object.getInteger("x");
                int y = object.getInteger("y");
                int width = object.getInteger("width");
                int height = object.getInteger("height");
                String color = object.getString("color").toUpperCase();
                if (color.startsWith("0X")) {
                    color = color.substring(2);
                }
                String type = object.getString("type");
                String value = object.getString("value");
                paint.setColor(Integer.parseInt(color, 16));
                switch (type) {
                    case "pic":
                        Bitmap qrcodeBit = CommonPrintThread.compressBitMap(CommonPrintThread.getBitMBitmap(value), width, height);
                        canvas.drawBitmap(qrcodeBit, x, y, paint);
                        break;
                    case "word":
                        paint.setTextSize(width);//设置文字大小
                        canvas.drawText(value, x, y, paint);
                        break;
                    default:
                        break;
                }
            }
            new CommonPrintThread(cordova.getActivity(), "").print(bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示升级信息的对话框
     *
     * @param versionName
     * @param desc
     * @param url
     */
    private void update(String versionName, String desc, final String url) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getActivity());
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle("请升级APP版本至" + versionName);
        builder.setMessage(desc.replaceAll("#", "\n"));
        builder.setCancelable(false);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (Environment.getExternalStorageState().equals(
                        Environment.MEDIA_MOUNTED)) {
                    downFile(url);//点击确定将apk下载
                } else {
                    Toast.makeText(cordova.getActivity(), "SD卡不可用，请插入SD卡", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //用户点击了取消
            }
        });
        builder.create().show();
    }


    /**
     * 下载最新版本的apk
     *
     * @param path apk下载地址
     */
    private void downFile(final String path) {
        pBar = new ProgressDialog(cordova.getActivity());
        pBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pBar.setCancelable(false);
        pBar.setTitle("正在下载...");
        pBar.setMessage("请稍候...");
        pBar.setProgress(0);
        pBar.show();
        new Thread() {
            public void run() {
                try {
                    URL url = new URL(path);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setReadTimeout(5000);
                    con.setConnectTimeout(5000);
                    con.setRequestProperty("Charset", "UTF-8");
                    con.setRequestMethod("GET");
                    if (con.getResponseCode() == 200) {
                        int length = con.getContentLength();// 获取文件大小
                        InputStream is = con.getInputStream();
                        pBar.setMax(Math.round(length / 1024 / 1024)); // 设置进度条的总长度
                        pBar.setProgressNumberFormat("%1d M/%2d M");
                        FileOutputStream fileOutputStream = null;
                        if (is != null) {
                            //对apk进行保存
                            chmod(cordova.getActivity().getFilesDir().getAbsolutePath());
                            File file;
                            //如果相等的话表示当前的sdcard挂载在手机上并且是可用的
                            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                                file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "android-trace.apk");
                            } else {
                                file = new File(cordova.getActivity().getFilesDir().getAbsolutePath(), "android-trace.apk");
                            }

                            fileOutputStream = new FileOutputStream(file);
                            byte[] buf = new byte[1024];
                            int ch;
                            int process = 0;
                            while ((ch = is.read(buf)) != -1) {
                                fileOutputStream.write(buf, 0, ch);
                                process += ch;
                                pBar.setProgress(Math.round(process / 1024 / 1024)); // 实时更新进度了
                            }
                        }
                        if (fileOutputStream != null) {
                            fileOutputStream.flush();
                            fileOutputStream.close();
                        }
                        //apk下载完成，使用Handler()通知安装apk
                        mHandler.sendEmptyMessage(99);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }.start();
    }

    /**
     * 二维码扫描插件
     */
    public void scan() {
        Intent intent = new Intent(this.cordova.getActivity(), CaptureActivity.class);
        this.cordova.startActivityForResult((CordovaPlugin) this, intent, REQUEST_CODE);
    }

    /**
     * 返回值适用
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("scanUrl", intent.getStringExtra("scanUrl"));
                } catch (JSONException e) {
                    Log.d("trace scan plugin", "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                callbackContext.success(obj);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("scanUrl", "");
                } catch (JSONException e) {
                    Log.d("trace scan plugin", "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                callbackContext.success(obj);
            } else {
                //this.error(new PluginResult(PluginResult.Status.ERROR), this.callback);
                callbackContext.error("Unexpected error");
            }
        }
    }

    //暂时不适用
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
        }
    }

    public static void chmod(String pathc) {
        String chmodCmd = "chmod -R 777 " + pathc;
        try {
            Runtime.getRuntime().exec(chmodCmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Handler mHandler = new Handler(new Handler.Callback() {

        @SuppressWarnings("unchecked")
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case 99:
                    //将下载进度对话框取消
                    pBar.cancel();
                    //安装apk，也可以进行静默安装
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    //对apk进行保存
                    File file;
                    //如果相等的话表示当前的sdcard挂载在手机上并且是可用的
                    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "android-trace.apk");
                    } else {
                        file = new File(cordova.getActivity().getFilesDir().getAbsolutePath(), "android-trace.apk");
                    }
                    chmod(cordova.getActivity().getFilesDir().getAbsolutePath());
                    intent.setDataAndType(Uri.fromFile(file),
                            "application/vnd.android.package-archive");
                    cordova.getActivity().startActivity(intent);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    return true;
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
                            Toast.makeText(cordova.getActivity(), "连接失败....", Toast.LENGTH_SHORT).show();
                            break;
                    }
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
