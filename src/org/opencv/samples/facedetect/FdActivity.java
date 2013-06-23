package org.opencv.samples.facedetect;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class FdActivity extends Activity {
	private static final String TAG = "Activity";

	private MenuItem itemFace50;
	private MenuItem itemFace40;
	private MenuItem itemFace30;
	private MenuItem itemFace20;
	private MenuItem itemType;

	private FdView view;
	private TextView matchingMethod;
	public static int method = 1;

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
				view.setDetectorType(mDetectorType);
				view.setMinFaceSize(0.2f);

				VerticalSeekBar VerticalseekBar = new VerticalSeekBar(getApplicationContext());
				VerticalseekBar.setMax(5);
				VerticalseekBar.setPadding(20, 20, 20, 20);
				RelativeLayout.LayoutParams vsek = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT, 400);
				vsek.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				VerticalseekBar.setId(1);
				VerticalseekBar	.setOnSeekBarChangeListener(
                        new OnSeekBarChangeListener() {
							public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
								method = progress;

								switch (method) {
								case 0:
									matchingMethod.setText("TM_SQDIFF");
									break;
								case 1:
									matchingMethod.setText("TM_SQDIFF_NORMED");
									break;
								case 2:
									matchingMethod.setText("TM_CCOEFF");
									break;
								case 3:
									matchingMethod.setText("TM_CCOEFF_NORMED");
									break;
								case 4:
									matchingMethod.setText("TM_CCORR");
									break;
								case 5:
									matchingMethod.setText("TM_CCORR_NORMED");
									break;
								}
							}

							public void onStartTrackingTouch(SeekBar seekBar) {}

							public void onStopTrackingTouch(SeekBar seekBar) {}
						});

				matchingMethod = new TextView(getApplicationContext());
				matchingMethod.setText("TM_SQDIFF");
				matchingMethod.setTextColor(Color.YELLOW);
				RelativeLayout.LayoutParams matching_method_param = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				matching_method_param
						.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				matching_method_param.addRule(RelativeLayout.BELOW,
						VerticalseekBar.getId());

				Button btn = new Button(getApplicationContext());
				btn.setText("Create template");
				RelativeLayout.LayoutParams btnp = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				btnp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				btn.setId(2);

				btn.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						view.resetLearnFramesCount();
					}
				});

				RelativeLayout frameLayout = new RelativeLayout(
						getApplicationContext());
				frameLayout.addView(view, 0);
				frameLayout.addView(btn, btnp);

				frameLayout.addView(VerticalseekBar, vsek);
				frameLayout.addView(matchingMethod, matching_method_param);

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

	private int mDetectorType = 0;
	private String[] mDetectorName;

	public FdActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
		mDetectorName = new String[2];
		mDetectorName[FdView.JAVA_DETECTOR] = "Java";
		mDetectorName[FdView.NATIVE_DETECTOR] = "Native (tracking)";
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "onCreateOptionsMenu");
		itemFace50 = menu.add("Face size 50%");
		itemFace40 = menu.add("Face size 40%");
		itemFace30 = menu.add("Face size 30%");
		itemFace20 = menu.add("Face size 20%");
		itemType = menu.add(mDetectorName[mDetectorType]);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "Menu Item selected " + item);
		if (item == itemFace50)
			view.setMinFaceSize(0.5f);
		else if (item == itemFace40)
			view.setMinFaceSize(0.4f);
		else if (item == itemFace30)
			view.setMinFaceSize(0.3f);
		else if (item == itemFace20)
			view.setMinFaceSize(0.2f);
		else if (item == itemType) {
			mDetectorType = (mDetectorType + 1) % mDetectorName.length;
			item.setTitle(mDetectorName[mDetectorType]);
			view.setDetectorType(mDetectorType);
		}
		return true;
	}
}
