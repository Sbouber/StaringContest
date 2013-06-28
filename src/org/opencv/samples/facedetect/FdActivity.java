package org.opencv.samples.facedetect;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.RelativeLayout;

public class FdActivity extends Activity implements OnReadyCountDownListener {
	private static final String TAG = "Activity";
	private static String countDownText = "Wait...";
	private static Paint paint;

	private static boolean isReady = false;
	private static boolean stopCounter = false;
	private static long startTime = 0;
	private static long time = 0;
	private static FdView view = null;

	private FdActivity thiz = null;

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
					ad.setMessage("No front camera available!");
					ad.setButton("OK", new DialogInterface.OnClickListener() {
						@Override
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

	@Override
	public void onFinish() {
		long finalTime = System.currentTimeMillis() - startTime;
		view.surfaceDestroyed(view.getHolder());
		Intent intent = new Intent(this, NewScoreActivity.class);
		intent.putExtra(null, finalTime);
		startActivity(intent);
	}

	@Override
	public void onReady() {
		/* Enable the countdown. */
		isReady = true;
	}

	static void draw(Canvas canvas) {
		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setTextSize(100);

		if (isReady && !stopCounter) {
			/* Start the countdown..." */
			if (time == 0) {
				time = System.currentTimeMillis();
			}

			/* Calculate the countdown timer and output it. */
			long temp = System.currentTimeMillis();

			if (temp - time < 5000) {
				countDownText = "" + (5999 - (temp - time)) / 1000;
			} else if (temp - time <= 5500) {
				countDownText = "Start!";
			} else {
				countDownText = "";
				stopCounter = true;
				startTime = System.currentTimeMillis();
				view.startBlinkDetection();
			}
		}

		int xPos = (int) ((canvas.getWidth() - paint.getTextSize()
				* Math.abs(countDownText.length() / 2)) / 2);
		int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint
				.ascent()) / 2));

		canvas.drawText(countDownText, xPos, yPos, paint);
	}

	public void notifyBlink(int blinkCount) {
		return;
	}

	public FdActivity() {
		this.thiz = this;
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (view != null)
			view.releaseCamera();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (view != null && !view.openCamera()) {
			AlertDialog ad = new AlertDialog.Builder(this).create();
			ad.setCancelable(false); // This blocks the 'BACK' button
			ad.setMessage("Fatal error: can't open camera!");
			ad.setButton("OK", new DialogInterface.OnClickListener() {
				@Override
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
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this,
				openCVCallBack)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager");
		}
	}
}
