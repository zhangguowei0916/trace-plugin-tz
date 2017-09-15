package com.cordova.plugins.leanit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.bixolon.printer.BixolonPrinter;

import org.json.JSONException;
import org.json.JSONObject;

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
	private String desc;
	private final float width = 1000;
	private final float height = 600;

	public CommonPrintThread(Context context, String args) {
		this.context = context;
		try {
			if (null != args && args.length() > 0) {
				JSONObject myJsonObject = new JSONObject(args);
				this.url = myJsonObject.getString("url").replace("\\", "/");
				this.desc = myJsonObject.getString("desc");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
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
			if (null != checkStrLink && !"".equals(checkStrLink)) {
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
							TracePluginCommon.mBixolonPrinter.printText(desc, 1, 16, 16, false);
							TracePluginCommon.mBixolonPrinter.formFeed(true);
						}
					} catch (Exception e) {
						showHandler.sendEmptyMessage(0);
					}
				}
			}).start();
		}

	}

	public void print(final Bitmap bitmap) {
		if (!TracePluginCommon.mIsConnected) {
			String checkStrLink = getBlueToothAdapter();
			if (null != checkStrLink && !"".equals(checkStrLink)) {
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
					try {
						if (null == bitmap) {
							showMessage = "二维码不存在";
							showHandler.sendEmptyMessage(0);
						} else {
							Bitmap map = compressBitMap(bitmap, width, height);
							TracePluginCommon.mBixolonPrinter.setLabelMode();
//							TracePluginCommon.mBixolonPrinter.setReceiptMode();
							//back-feeding enable
							TracePluginCommon.mBixolonPrinter.executeDirectIo(new byte[]{0x1d, 0x28, 0x46, 0x04, 0x00, 0x03}, false);
							TracePluginCommon.mBixolonPrinter.executeDirectIo(new byte[]{0x1d, 0x28, 0x46, 0x04, 0x00, 0x00, 0x00, (byte) 0x46, 0x00}, false);
							TracePluginCommon.mBixolonPrinter.printBitmap(map, BixolonPrinter.ALIGNMENT_CENTER, 0, 50, false, true, true);
							TracePluginCommon.mBixolonPrinter.formFeed(true);
							TracePluginCommon.mBixolonPrinter.executeDirectIo(new byte[]{0x1d, 0x28, 0x46, 0x04, 0x00, 0x04}, false);

						}
					} catch (Exception e) {
						showHandler.sendEmptyMessage(0);
					}
				}
			}).start();
		}

	}

	public static Bitmap compressBitMap(Bitmap bitmap, float willWidth, float willHeight) {
		if (null != bitmap) {
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			// 设置想要的大小
			float scale = Math.min(willWidth / width, willHeight / height);
			// 计算缩放比例
			float scaleWidth = scale * width;
			float scaleHeight = scale * height;
			// 取得想要缩放的matrix参数
			Matrix matrix = new Matrix();
			matrix.postScale(scale, scale);
			// 得到新的图片
			Bitmap targetBitMap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix,
				true);
			return targetBitMap;
		}
		return null;
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

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
