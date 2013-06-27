package org.opencv.samples.facedetect;

import java.util.List;

import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class SampleCvViewBase extends SurfaceView implements
		SurfaceHolder.Callback, Runnable {
	private static final String TAG = "SurfaceView";

	private SurfaceHolder holder;
	private VideoCapture camera;

	public SampleCvViewBase(Context context) {
		super(context);
		holder = getHolder();
		holder.addCallback(this);
	}

	public boolean openCamera() {
		synchronized (this) {
			releaseCamera();
			/* Use front camera */
			camera = new VideoCapture(Highgui.CV_CAP_ANDROID + 1);

			if (!camera.isOpened()) {
				camera.release();
				camera = null;
				Log.e(TAG, "Failed to open native camera");
				return false;
			}
		}

		return true;
	}

	public void releaseCamera() {
		synchronized (this) {
			if (camera != null) {
				camera.release();
				camera = null;
			}
		}
	}

	public void setupCamera(int width, int height) {
		Log.i(TAG, "setupCamera(" + width + ", " + height + ")");
		synchronized (this) {
			if (camera != null && camera.isOpened()) {
				List<Size> sizes = camera.getSupportedPreviewSizes();
				int mFrameWidth = width;
				int mFrameHeight = height;

				// TODO: We could select a smaller size, might get a higher fps.
				// selecting optimal camera preview size
				double minDiff = Double.MAX_VALUE;

				for (Size size : sizes) {
					if (Math.abs(size.height - height) < minDiff) {
						mFrameWidth = (int) size.width;
						mFrameHeight = (int) size.height;
						minDiff = Math.abs(size.height - height);
					}
				}

				camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mFrameWidth);
				camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mFrameHeight);
			}
		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder _holder, int format, int width,
			int height) {
		setupCamera(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		(new Thread(this)).start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		releaseCamera();
	}

	protected abstract Bitmap processFrame(VideoCapture capture);

	@Override
	public void run() {
		Log.i(TAG, "Starting processing thread");

		while (true) {
			Bitmap bmp = null;

			synchronized (this) {
				if (camera == null)
					break;

				if (!camera.grab()) {
					Log.e(TAG, "mCamera.grab() failed");
					break;
				}

				bmp = processFrame(camera);
			}

			if (bmp != null) {
				Canvas canvas = holder.lockCanvas();

				if (canvas != null) {
					bmp.prepareToDraw();
					canvas.drawBitmap(bmp,
							(canvas.getWidth() - bmp.getWidth()) / 2,
							(canvas.getHeight() - bmp.getHeight()) / 2, null);
					FdActivity.draw(canvas);
					holder.unlockCanvasAndPost(canvas);
				}

				bmp.recycle();
			}
		}

		Log.i(TAG, "Finishing processing thread");
	}
}