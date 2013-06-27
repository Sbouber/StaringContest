package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceHolder;

class FdView extends SampleCvViewBase {
	private static final String TAG = "Sample::FdView";

	private static final Scalar RED = new Scalar(255, 0, 0, 255);
	private static final Scalar GREEN = new Scalar(0, 255, 0, 255);
	private static final Scalar BLUE = new Scalar(0, 0, 255, 255);
	private static final Scalar PINK = new Scalar(224, 27, 162, 255);
	private static final Scalar ORANGE = new Scalar(224, 162, 27, 255);

	private static final int MIN_THRESHOLD = 5;
	private static final int MIN_DEVIATION = 2;

	private Mat rgba;
	private Mat gray;
	private Mat zoomCorner;
	private Mat zoomWindow;
	private Mat zoomWindow2;
	private File cascadeFile;
	private DetectionBasedTracker nativeDetector;

	private float relativeFaceSize;
	private int absoluteFaceSize;

	private int thresholdLeft;
	private int thresholdRight;
	private double leftMean;
	private double rightMean;
	private double oldLeftMean = -1.0;
	private double oldRightMean = -1.0;
	private boolean alreadyBlinked;
	private int blinkCount = 0;
	private int faceEmptyCount;
	private Scalar color = RED;
	private boolean enableCountDown = true;
	private OnReadyCountDownListener ready;

	public FdView(Context context) {
		super(context);

		try {
			// We need to write our resource to a file first since
			// CascadeClassifier takes a filename
			InputStream is = context.getResources().openRawResource(
					R.raw.lbpcascade_frontalface);
			File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
			cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
			FileOutputStream os = new FileOutputStream(cascadeFile);

			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}

			is.close();
			os.close();

			nativeDetector = new DetectionBasedTracker(
					cascadeFile.getAbsolutePath(), 0);

			cascadeDir.delete();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Failed to load cascade, " + e);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		synchronized (this) {
			// initialize Mats before usage
			gray = new Mat();
			rgba = new Mat();
		}

		super.surfaceCreated(holder);
	}

	private void resetThreshold() {
		faceEmptyCount++;

		if (faceEmptyCount > 50) {
			thresholdLeft = 0;
			thresholdRight = 0;
			faceEmptyCount = 0;
			color = new Scalar(255, 0, 0, 255);
		}
	}

	private Rect getLargestRect(Rect[] rects) {
		double area = 0;
		Rect r = null;

		for (int i = 0; i < rects.length; i++) {
			if (rects[i].area() > area) {
				area = rects[i].area();
				r = rects[i];
			}
		}

		return r;
	}

	@Override
	protected Bitmap processFrame(VideoCapture capture) {
		capture.retrieve(rgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
		capture.retrieve(gray, Highgui.CV_CAP_ANDROID_GREY_FRAME);

		if (absoluteFaceSize == 0) {
			int height = gray.rows();

			if ((int) (height * relativeFaceSize + 0.5) > 0) {
				absoluteFaceSize = (int) (height * relativeFaceSize + 0.5);
			}

			nativeDetector.setMinFaceSize(absoluteFaceSize);
		}

		MatOfRect faces = new MatOfRect();

		if (nativeDetector != null) {
			nativeDetector.detect(gray, faces);

			if (zoomCorner == null || zoomWindow == null) {
				CreateAuxiliaryMats();
			}

			Rect[] facesArray = faces.toArray();

			if (facesArray.length == 0) {
				resetThreshold();
			}

			Rect face = getLargestRect(facesArray);

			// Face found
			if (face != null) {
				Core.rectangle(gray, face.tl(), face.br(), color, 3);
				Core.rectangle(rgba, face.tl(), face.br(), color, 3);

				int x = face.x + face.width / 8;
				int y = (int) (face.y + (face.height / 4.5));
				int width = (face.width - 2 * face.width / 8);
				int height = (int) (face.height / 3.0);

				Rect eyearea = new Rect(x + 10, y, width - 10, height);

				Core.rectangle(rgba, eyearea.tl(), eyearea.br(), BLUE, 2);

				// Determine right eye area
				x = (face.x + face.width / 16) + 50;
				y = (int) (face.y + (face.height / 4.5));
				width = ((face.width - 2 * face.width / 16) / 2) - 50;
				height = (int) (face.height / 3.0);

				Rect rightEyeArea = new Rect(x, y, width, height);

				// Determine left eye area
				x = (face.x + face.width / 16 + (face.width - 2 * face.width / 16) / 2);
				y = (int) (face.y + (face.height / 4.5));
				width = ((face.width - 2 * face.width / 16) / 2) - 50;
				height = (int) (face.height / 3.0);

				Rect leftEyeArea = new Rect(x, y, width, height);

				Core.rectangle(rgba, leftEyeArea.tl(), leftEyeArea.br(), PINK,
						2);
				Core.rectangle(rgba, rightEyeArea.tl(), rightEyeArea.br(),
						ORANGE, 2);

				Mat leftEyeGray = null;
				Mat rightEyeGray = null;

				try {
					leftEyeGray = gray.submat(leftEyeArea);
					rightEyeGray = gray.submat(rightEyeArea);

					// Find left eye threshold
					Imgproc.threshold(leftEyeGray, leftEyeGray, thresholdLeft,
							255, Imgproc.THRESH_BINARY);
					Imgproc.medianBlur(leftEyeGray, leftEyeGray, 11);

					double minLeft = Core.minMaxLoc(leftEyeGray).minVal;
					leftMean = Core.mean(leftEyeGray).val[0];

					// Find right eye threshold
					Imgproc.threshold(rightEyeGray, rightEyeGray,
							thresholdLeft, 255, Imgproc.THRESH_BINARY);
					Imgproc.medianBlur(rightEyeGray, rightEyeGray, 11);

					double minRight = Core.minMaxLoc(rightEyeGray).minVal;
					rightMean = Core.mean(leftEyeGray).val[0];

					// No dark color found, increase threshold
					if (minRight > 0) {
						thresholdRight++;
					}

					// No dark color found, increase threshold
					if (minLeft > 0) {
						thresholdLeft++;
					}

					// Left and right eye found, calibration complete
					if (minRight <= 0 && minLeft <= 0) {
						color = GREEN;

						if (enableCountDown) {
							Log.e(TAG, "Calling onReady()");
							enableCountDown = false;
							ready.onReady();
						}
					}

				} catch (Exception e) {
					Log.e(TAG, "error: " + e.getMessage());
				}

				// Blink detection
				if (oldRightMean > 0 && oldLeftMean > 0
						&& thresholdLeft > MIN_THRESHOLD
						&& thresholdRight > MIN_THRESHOLD) {

					double totalOld = (oldRightMean + oldLeftMean) / 2;
					double totalNew = (leftMean + rightMean) / 2;

					if (Math.abs(totalOld - totalNew) > MIN_DEVIATION) {
						if (!alreadyBlinked) {
							blinkCount++;
							alreadyBlinked = true;
							Log.e(TAG, "blink nr: " + blinkCount);

							Vibrator vibrator = (Vibrator) this.getContext()
									.getSystemService(Context.VIBRATOR_SERVICE);
							vibrator.vibrate(300);
						} else {
							alreadyBlinked = false;
						}
					}
				}

				oldLeftMean = leftMean;
				oldRightMean = rightMean;

				try {
					// Convert back to RGBA
					Imgproc.cvtColor(leftEyeGray, leftEyeGray,
							Imgproc.COLOR_GRAY2BGRA);
					Imgproc.cvtColor(rightEyeGray, rightEyeGray,
							Imgproc.COLOR_GRAY2BGRA);

					Imgproc.resize(leftEyeGray, zoomWindow, zoomWindow.size());
					Imgproc.resize(rightEyeGray, zoomWindow2,
							zoomWindow2.size());
				} catch (Exception e) {
					Log.e(TAG, "error cv: " + e.getLocalizedMessage());
				}
			}
		}

		Bitmap bmp = Bitmap.createBitmap(rgba.cols(), rgba.rows(),
				Bitmap.Config.ARGB_8888);

		try {
			Utils.matToBitmap(rgba, bmp);
		} catch (Exception e) {
			Log.e(TAG,
					"Utils.matToBitmap() throws an exception: "
							+ e.getMessage());
			bmp.recycle();
			bmp = null;
		}

		return bmp;
	}

	public void setListener(OnReadyCountDownListener ready) {
		Log.e(TAG, "set ready field");
		this.ready = ready;
	}

	private void CreateAuxiliaryMats() {
		if (gray.empty())
			return;

		int rows = gray.rows();
		int cols = gray.cols();

		if (zoomWindow == null) {
			zoomWindow = rgba.submat(rows / 2 + rows / 10, rows, cols / 2
					+ cols / 10, cols);
			zoomWindow2 = rgba.submat(0, rows / 2 - rows / 10, cols / 2 + cols
					/ 10, cols);
		}

	}

	@Override
	public void run() {
		super.run();

		synchronized (this) {
			// Explicitly deallocate Mats
			if (rgba != null)
				rgba.release();
			if (gray != null)
				gray.release();
			if (cascadeFile != null)
				cascadeFile.delete();
			if (nativeDetector != null)
				nativeDetector.release();

			rgba = null;
			gray = null;
			cascadeFile = null;
		}
	}
}
