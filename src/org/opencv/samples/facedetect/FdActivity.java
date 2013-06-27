package org.opencv.samples.facedetect;

import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.widget.RelativeLayout;

public class FdActivity extends Activity implements OnReadyCountDownListener {
	private static final String TAG = "Activity";
	private static String countDownText = "Wait...";
	private static Paint paint;

	private static int count = 0;

	private FdActivity thiz = null;
	private FdView view;
	private long time;

	private BaseLoaderCallback openCVCallBack = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				// Load native libs after OpenCV initialization
				// System.loadLibrary("detection_based_tracker");

				// Create and set View
				view = new FdView(mAppContext);
				view.setListener(thiz);

				RelativeLayout frameLayout = new RelativeLayout(
						getApplicationContext());
				frameLayout.addView(view, 0);
				setContentView(frameLayout);

				// Check native OpenCV camera
				if (!view.openCamera()) {
					AlertDialog ad = new AlertDialog.Builder(mAppContext)
							.create();
					ad.setCancelable(false); // This blocks the 'BACK' button
					ad.setMessage("Fatal error: can't open camera!");
					ad.setButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}
					});
					ad.show();
				}
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public void onReady() {
		Log.e(TAG, "Calling countdown");
		countDown();
	}

	public void countDown() {
		Log.e(TAG, "CountDown called");
		Looper.prepare();
		new CountDownTimer(5000, 100) {
			public void onTick(long millisUntilFinished) {
				Log.e(TAG, "CountDown tick");
				countDownText = "" + ((millisUntilFinished / 1000) + 1);
			}

			public void onFinish() {
				Log.e(TAG, "CountDown finish");
				countDownText = "Start!";

				Timer t = new Timer(false);

				t.schedule(new TimerTask() {
					@Override
					public void run() {
						runOnUiThread(new Runnable() {
							public void run() {
								countDownText = "";
							}
						});
					}
				}, 1000);

				time = System.currentTimeMillis();
			}
		}.start();
		Log.e(TAG, "CountDown() return");
		Looper.loop();
	}

	static void draw(Canvas canvas) {
		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setTextSize(100);

		int xPos = (int) ((canvas.getWidth() - paint.getTextSize()
				* Math.abs(countDownText.length() / 2)) / 2);
		int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint
				.ascent()) / 2));

		// canvas.drawText(countDownText.concat("" + count++), xPos, yPos,
		// paint);
		canvas.drawText(countDownText, xPos, yPos, paint);
	}

	public void notifyBlink(int blinkCount) {
		return;
	}

	public FdActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
		this.thiz = this;
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();

		if (view != null)
			view.releaseCamera();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();

		if (view != null && !view.openCamera()) {
			AlertDialog ad = new AlertDialog.Builder(this).create();
			ad.setCancelable(false); // This blocks the 'BACK' button
			ad.setMessage("Fatal error: can't open camera!");
			ad.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					finish();
				}
			});
			ad.show();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		Log.i(TAG, "Trying to load OpenCV library");

		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this,
				openCVCallBack)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager");
		}
	}
}
