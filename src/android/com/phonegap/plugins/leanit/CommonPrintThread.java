package com.leanit.qrcode_trace_android.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.bixolon.printer.BixolonPrinter;
import com.bumptech.glide.Glide;
import com.leanit.qrcode_trace_android.MainActivity;
import com.leanit.qrcode_trace_android.R;
import com.leanit.qrcode_trace_android.common.util.AndroidUtil;
import com.leanit.qrcode_trace_android.common.util.LeanItStringUtil;
import com.leanit.qrcode_trace_android.common.util.PropertieUtil;

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

    public void print() {
        if (!CommonConstant.IS_LINK) {
            String checkStrLink = AndroidUtil.getBlueToothAdapter();
            if (LeanItStringUtil.isNotEmpty(checkStrLink)) {
                showMessage = checkStrLink;
                showHandler.sendEmptyMessage(0);
            } else {
                showMessage = context.getResources().getString(R.string.show_waring_no_bluetooth);
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
                                .load(PropertieUtil.getProperties(context, CommonConstant.IMAGE_URL) + url)
                                .asBitmap() //必须
                                .centerCrop()
                                .into(1000,1000)
                                .get();
                        if (null == bitmap) {
                            showMessage = context.getResources().getString(R.string.show_waring_no_qrcode);
                            showHandler.sendEmptyMessage(0);
                        } else {
                            MainActivity.mBixolonPrinter.printBitmap(bitmap, BixolonPrinter.ALIGNMENT_CENTER, 450, 50, false, false, true);
                            MainActivity.mBixolonPrinter.printText(context.getResources().getString(R.string.company),1,16,16,false);
                            MainActivity.mBixolonPrinter.formFeed(true);
                        }
                    } catch (Exception e) {
                        showMessage = context.getResources().getString(R.string.show_waring_qrcode);
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
                    final SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
                    pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                    pDialog.setTitleText("正在打印...");
                    pDialog.setCancelable(false);
                    pDialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(3000l);
                            } catch (InterruptedException e) {
                                Log.d("Thread", e.getMessage());
                            }
                            pDialog.dismiss();

                        }
                    }).start();
            }
            return true;
        }

    });
}
