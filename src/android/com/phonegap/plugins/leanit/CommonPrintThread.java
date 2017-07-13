package com.phonegap.plugins.leanit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.bixolon.printer.BixolonPrinter;
import com.bumptech.glide.Glide;
import com.phonegap.plugins.leanit.TracePluginCommon;

import cn.pedant.SweetAlert.SweetAlertDialog;

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
        if (!CommonConstant.IS_LINK) {
            String checkStrLink = getBlueToothAdapter();
            if (null==checkStrLink||''==checkStrLink) {
                showMessage = checkStrLink;
                showHandler.sendEmptyMessage(0);
            } else {
                showHandler.sendEmptyMessage(1);
            }
        } else {
            showHandler.sendEmptyMessage(2);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = null;
                    try {
                        bitmap = Glide.with(context)
                                .load(url)
                                .asBitmap() //必须
                                .centerCrop()
                                .into(1000,1000)
                                .get();
                        if (null == bitmap) {
                            showHandler.sendEmptyMessage(0);
                        } else {
                            TracePluginCommon.mBixolonPrinter.printBitmap(bitmap, BixolonPrinter.ALIGNMENT_CENTER, 450, 50, false, false, true);
                            TracePluginCommon.mBixolonPrinter.printText(context.getResources().getString(R.string.company),1,16,16,false);
                            TracePluginCommon.mBixolonPrinter.formFeed(true);
                        }
                    } catch (Exception e) {
                        showHandler.sendEmptyMessage(0);
                    }
                }
            }).start();
        }

    }

    public final Handler showHandler = new Handler(new Handler.Callback() {

        @SuppressWarnings("unchecked")
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Toast.makeText(context, showMessage, Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Toast.makeText(context, showMessage, Toast.LENGTH_LONG).show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(3000l);
                            } catch (InterruptedException e) {
                                Log.d("Thread", e.getMessage());
                            }
                            AppManager.getAppManager().finishAllActivityExceptMain();

                        }
                    }).start();
                    break;
                case 2:
                    AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getActivity());
                    builder.setTitle("提示");
                    builder.setMessage("正在打印..");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(3000l);
                            } catch (InterruptedException e) {
                                Log.d("Thread", e.getMessage());
                            }
                            dialog.dismiss();

                        }
                    }).start();
            }
            return true;
        }

    });
}
