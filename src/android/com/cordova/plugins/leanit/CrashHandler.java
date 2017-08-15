package com.cordova.plugins.leanit;

/**
 * Created by admin on 2016/10/31.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.xutils.common.util.MD5;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告.
 *
 * @author user
 */
public class CrashHandler implements UncaughtExceptionHandler {

	public static final String TAG = "CrashHandler";

	//系统默认的UncaughtException处理类
	private UncaughtExceptionHandler mDefaultHandler;
	//CrashHandler实例
	private static CrashHandler INSTANCE = new CrashHandler();
	//程序的Context对象
	private Context mContext;
	//用来存储设备信息和异常信息
	private Map<String, String> infos = new LinkedHashMap<String, String>();

	//用于格式化日期,作为日志文件名的一部分
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	/**
	 * 保证只有一个CrashHandler实例
	 */
	private CrashHandler() {
	}

	/**
	 * 获取CrashHandler实例 ,单例模式
	 */
	public static CrashHandler getInstance() {
		return INSTANCE;
	}

	/**
	 * 初始化
	 *
	 * @param context
	 */
	public void init(Context context) {
		mContext = context;
		//获取系统默认的UncaughtException处理器
		mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		//设置该CrashHandler为程序的默认处理器
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	/**
	 * 当UncaughtException发生时会转入该函数来处理
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		if (!handleException(ex) && mDefaultHandler != null) {
			//如果用户没有处理则让系统默认的异常处理器来处理
			mDefaultHandler.uncaughtException(thread, ex);
		} else {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Log.e(TAG, "error : ", e);
			}
			//退出程序
			android.os.Process.killProcess(android.os.Process.myPid());
			System.exit(1);
		}
	}

	/**
	 * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
	 *
	 * @param ex
	 * @return true:如果处理了该异常信息;否则返回false.
	 */
	private boolean handleException(Throwable ex) {
		if (ex == null) {
			return false;
		}

		ex.printStackTrace();

		//使用Toast来显示异常信息
		new Thread() {
			@Override
			public void run() {
				Looper.prepare();
				Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出.", Toast.LENGTH_LONG).show();
				Looper.loop();
			}
		}.start();

		Log.e(TAG, "->", ex);

		//收集设备参数信息
		collectDeviceInfo(mContext);
		//保存日志文件
		saveCrash(ex);
		return true;
	}

	/**
	 * 收集设备参数信息
	 *
	 * @param ctx
	 */
	public void collectDeviceInfo(Context ctx) {
		try {
			PackageManager pm = ctx.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
			if (pi != null) {
				String versionName = pi.versionName == null ? "null" : pi.versionName;
				String versionCode = pi.versionCode + "";
				infos.put("versionName", versionName);
				infos.put("versionCode", versionCode);
			}
		} catch (NameNotFoundException e) {
			Log.e(TAG, "an error occured when collect package info", e);
		}
		Field[] buildField = Build.class.getDeclaredFields();
		for (Field field : buildField) {
			try {
				field.setAccessible(true);
				Object value = field.get(null);

				if (value instanceof String[]) {
					String[] valueArray = (String[]) value;
					String valueStr = "";
					for (String str : valueArray) {
						valueStr += str + " ";
					}
					value = valueStr;
				}

				infos.put(field.getName(), value.toString());
				Log.d(TAG, field.getName() + " : " + value);
			} catch (Exception e) {
				Log.e(TAG, "an error occured when collect crash info", e);
			}
		}

		Field[] versionField = Build.VERSION.class.getDeclaredFields();
		for (Field field : versionField) {
			try {
				field.setAccessible(true);
				Object value = field.get(null);

				if (value instanceof String[]) {
					String[] valueArray = (String[]) value;
					String valueStr = "";
					for (String str : valueArray) {
						valueStr += str + " ";
					}
					value = valueStr;
				}
				infos.put("VERSION." + field.getName(), value.toString());
				Log.d(TAG, "VERSION." + field.getName() + " : " + value.toString());
			} catch (Exception e) {
				Log.e(TAG, "an error occured when collect crash info", e);
			}
		}
	}


	private void saveCrash(Throwable ex) {
		try {
			final StringBuffer sb = formatException(ex);

			String time = formatter.format(new Date());
			String fileName = "crash-" + time + "-" + System.currentTimeMillis() + ".log";

			saveToFile(fileName, sb);

			upload(fileName, sb);

		} catch (Exception e) {
			Log.e(TAG, "an error occured while writing file...", e);
		}
	}

	private void saveToFile(String fileName, StringBuffer sb) throws IOException {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			File dir = mContext.getExternalFilesDir("logs");
			if (!dir.exists()) {
				dir.mkdirs();
			}

			File file = new File(dir, fileName);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(sb.toString().getBytes("UTF-8"));
			fos.close();
		}
	}

	@NonNull
	private StringBuffer formatException(Throwable ex) {
		final StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, String> entry : infos.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			sb.append(key + "=" + value + "\n");
		}

		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		ex.printStackTrace(printWriter);
		Throwable cause = ex.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		printWriter.close();
		String result = writer.toString();
		sb.append(result);
		return sb;
	}

	public static boolean isNetworkConnected(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
			if (mNetworkInfo != null) {
				return mNetworkInfo.isAvailable() && mNetworkInfo.isConnected();
			}
		}
		return false;
	}

	private void upload(final String fileName, final StringBuffer buffer) {

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		new Thread() {
			@Override
			public void run() {
				try {
					if (!isNetworkConnected(mContext)) {
						Log.d(TAG, "isNetworkConnected:false");
						return;
					}
					OkHttpClient mOkHttpClient = new OkHttpClient();
					FormBody.Builder builder = new FormBody.Builder();
					builder.add("phone", getHandSetInfo());
					builder.add("fileName", fileName);
					builder.add("content", buffer.toString());
					SharedPreferences sharedPreferences = mContext.getSharedPreferences("setting", Context.MODE_PRIVATE);
				 	Request request = new Request.Builder()
						.url(sharedPreferences.getString("crashUrl", ""))
						.post(builder.build())
						.build();
					Response response = mOkHttpClient.newCall(request).execute();
					if (!response.isSuccessful()) {
						throw new IOException("" + response.code() + response.message());
					}
					String resultStr = response.body().string();
					Log.d(TAG, "resultStr:" + resultStr);
				} catch (Exception e) {
					Log.e(TAG, Log.getStackTraceString(e));
				} finally {
					countDownLatch.countDown();
				}
			}
		}.start();

		try {
			countDownLatch.await();
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}

	}
	private String getHandSetInfo(){
		String handSetInfo=
			"手机型号:" + android.os.Build.MODEL +
				",SDK版本:" + android.os.Build.VERSION.SDK +
				",系统版本:" + android.os.Build.VERSION.RELEASE+
				",软件版本:"+getAppVersionName(this.mContext);
		return handSetInfo;

	}

	//获取当前版本号
	private  String getAppVersionName(Context context) {
		String versionName = "";
		try {
			PackageManager packageManager = context.getPackageManager();
			PackageInfo packageInfo = packageManager.getPackageInfo("cn.testgethandsetinfo", 0);
			versionName = packageInfo.versionName;
			if (TextUtils.isEmpty(versionName)) {
				return versionName;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return versionName;
	}

}
