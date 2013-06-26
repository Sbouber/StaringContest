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
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

class FdView extends SampleCvViewBase {
	private static final String TAG = "Sample::FdView";
	
	private Mat mRgba;
	private Mat mGray;
	private Mat mZoomCorner;
	private Mat mZoomWindow;
	private Mat mZoomWindow2;
	private Mat mResult;
	private File mCascadeFile;
	private CascadeClassifier mJavaDetector;
	private DetectionBasedTracker mNativeDetector;

	private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

	public static final int JAVA_DETECTOR = 0;
	public static final int NATIVE_DETECTOR = 1;
	private int mDetectorType = JAVA_DETECTOR;

	private float mRelativeFaceSize = 0;
	private int mAbsoluteFaceSize = 0;
	
	public int thresholdLeft = 0;
	public int thresholdRight = 0;
	
	private Double leftMean = -1.0;
	private Double rightMean = -1.0;
	private Double oldLeftMean = -1.0;
	private Double oldRightMean = -1.0;
	private boolean alreadyBlinked = false;
	private int blinkCount = 0;
	private Rect face = null;
	private int faceEmptyCount = 0;
	private Scalar color = new Scalar( 255, 0, 0, 255 );
	
	private Rect eyearea = new Rect();

	public void setMinFaceSize(float faceSize) {
		mRelativeFaceSize = faceSize;
		mAbsoluteFaceSize = 0;
	}

	public void setDetectorType(int type) {
		if (mDetectorType != type) {
			mDetectorType = type;

			if (type == NATIVE_DETECTOR) {
				Log.i(TAG, "Detection Based Tracker enabled");
				mNativeDetector.start();
			} else {
				Log.i(TAG, "Cascade detector enabled");
				mNativeDetector.stop();
			}
		}
	}

	public FdView(Context context) {
		super(context);

		try {
			InputStream is = context.getResources().openRawResource(
					R.raw.lbpcascade_frontalface);
			File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
			mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
			FileOutputStream os = new FileOutputStream(mCascadeFile);

			byte[] buffer = new byte[4096];
			int bytesRead;
			
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			
			is.close();
			os.close();

			mJavaDetector = new CascadeClassifier(
					mCascadeFile.getAbsolutePath());
			
			if (mJavaDetector.empty()) {
				Log.e(TAG, "Failed to load cascade classifier");
				mJavaDetector = null;
			} else
				Log.i(TAG,
						"Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());

			mNativeDetector = new DetectionBasedTracker(
					mCascadeFile.getAbsolutePath(), 0);

			cascadeDir.delete();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		synchronized (this) {
			// initialize Mats before usage
			mGray = new Mat();
			mRgba = new Mat();
		}

		super.surfaceCreated(holder);
	}

	@Override
	protected Bitmap processFrame(VideoCapture capture) {
		capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
		capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);

		if (mAbsoluteFaceSize == 0) {
			int height = mGray.rows();

			if ((int) (height * mRelativeFaceSize + 0.5) > 0) {
				mAbsoluteFaceSize = (int) (height * mRelativeFaceSize + 0.5);
			}

			mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
		}

		MatOfRect faces = new MatOfRect();

		if (mDetectorType == JAVA_DETECTOR) {
			if (mJavaDetector != null)
				mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2,
						2 // TODO: objdetect.CV_HAAR_SCALE_IMAGE
						, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
						new Size());

			if (mZoomCorner == null || mZoomWindow == null)
				CreateAuxiliaryMats();

			Rect[] facesArray = faces.toArray();
			/*
			 * No faces found, reset threshold
			 */
			if( facesArray.length == 0 ) {	
				faceEmptyCount++;
				
				if( faceEmptyCount > 50 ) {
					thresholdLeft  = 0;
					thresholdRight = 0;
					faceEmptyCount = 0;
					color 		   = new Scalar( 255, 0, 0, 255 );
				}
			}
		
			double area  = 0.0;
			
			/*
			 * Find largest face area
			 */
			for (int i = 0; i < facesArray.length; i++) {
				
				if( facesArray[ i ].area() > area ) {
					area = facesArray[ i ].area();
					face = facesArray[ i ];
				}
			
			}
			
			/*
			 * Face found
			 */
			if( face != null ) {
				int minThreshold = 5;
				int minDeviation = 2;
				
				/*
		         * Get face rectangle
		         */
		        Scalar red       = new Scalar( 255, 0, 0, 255 );
		        Scalar blue      = new Scalar( 0, 0, 255, 255 );
		        
		        Scalar pink		= new Scalar( 224,27,162, 255);
		        Scalar orange	= new Scalar( 224,162,27, 255);
		        
		        
		        Core.rectangle(mGray, face.tl(), face.br(), color, 3);
		        Core.rectangle(mRgba, face.tl(), face.br(), color, 3);
		        	
		        
		        int x           = face.x + face.width / 8;
		        int y           = (int) (face.y + (face.height / 4.5));
		        int width       = (face.width - 2 * face.width / 8 );
		        int height  	= (int) (face.height / 3.0);
		        
		        eyearea = new Rect(x + 10 ,y ,width - 10 ,height);
		        
		        Core.rectangle(mRgba, eyearea.tl(), eyearea.br(), blue, 2);
		        /*
		         * Determine Left eye rectangle
		         */
		        x               = (face.x + face.width / 16) + 50;
		        y               = (int) (face.y + (face.height / 4.5));
		        width   		= ((face.width - 2 * face.width / 16) / 2) - 50;
		        height  		= (int) (face.height / 3.0);
		        
		        Rect eyearea_right = new Rect(x, y, width, height );
		        
		        /*
		         * Determine Right eye angle
		         */
		        x               = (face.x + face.width / 16 + (face.width - 2 * face.width / 16) / 2);
		        y               = (int) (face.y + (face.height / 4.5));
		        width   		= ( (face.width - 2 * face.width / 16) / 2 ) - 50;
		        height  		= (int) (face.height / 3.0);
		        
		        Rect eyearea_left = new Rect(x, y, width, height);
		        
		        Core.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(),pink, 2);
		        Core.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(),orange, 2);
				
				Mat leftEyeGray	 = mGray.submat( eyearea_left );
				Mat rightEyeGray = mGray.submat( eyearea_right );
				
				
				
				try {
					/*
					 * Find Left eye threshold
					 */
					Imgproc.threshold(leftEyeGray, leftEyeGray, thresholdLeft, 255, Imgproc.THRESH_BINARY);
					Imgproc.medianBlur(leftEyeGray, leftEyeGray, 11 );
		
					double minLeft = Core.minMaxLoc( leftEyeGray ).minVal;
					leftMean   	   = Core.mean( leftEyeGray ).val[ 0 ];
					
					/*
					 * Find Right eye threshold
					 */
					Imgproc.threshold(rightEyeGray, rightEyeGray, thresholdLeft, 255, Imgproc.THRESH_BINARY);
					Imgproc.medianBlur(rightEyeGray, rightEyeGray, 11 );
		
					double minRight = Core.minMaxLoc( rightEyeGray ).minVal;
					rightMean   = Core.mean( leftEyeGray ).val[ 0 ];
					
					/*
					 * No dark color found, increase threshold
					 */
					if( minRight > 0 ) {
						thresholdRight++;
					}
					
					/*
					 * No dark color found, increase threshold
					 */
					if( minLeft > 0 ) {
						thresholdLeft++;
					}
					
					/*
					 * Left and right eye found, calibration complete
					 */
					if( minRight <= 0 && minLeft <= 0 ) {
						
						color = new Scalar( 0, 255, 0, 255 );
						
					}
		
				}
				catch( Exception e ) {
					Log.e( TAG, "error: " + e.getMessage());
				}
		
				
				/*
				 * Blink detection
				 */
				if( oldRightMean > 0 && oldLeftMean > 0 && thresholdLeft > minThreshold && thresholdRight > minThreshold ) {
					
					double totalOld = ( oldRightMean + oldLeftMean ) / 2;
					double totalNew = ( leftMean + rightMean ) / 2;
					
					if( Math.abs( totalOld - totalNew ) > minDeviation )  {
						
						if( !alreadyBlinked ) {
							Log.e( TAG, "["+ blinkCount + "]blink");
							alreadyBlinked = true;
							blinkCount++;
						} else {
							alreadyBlinked = false;
//							Log.e( TAG, "["+ blinkCount + "]blink");
						}
					}
				}
				
				oldLeftMean  = leftMean;
				oldRightMean = rightMean;
				
				try {
					/*
					* Convert back to RGBA
					*/
					Imgproc.cvtColor(leftEyeGray, leftEyeGray, Imgproc.COLOR_GRAY2BGRA);
					Imgproc.cvtColor(rightEyeGray, rightEyeGray, Imgproc.COLOR_GRAY2BGRA);
		
					Imgproc.resize(leftEyeGray, mZoomWindow,
					mZoomWindow.size());
		
					Imgproc.resize(rightEyeGray, mZoomWindow2,
					mZoomWindow2.size());
				} catch( Exception e ) {
					Log.e( TAG, "error cv: " + e.getLocalizedMessage() );
				}
			}
			
			face = null;

		}
		else if (mDetectorType == NATIVE_DETECTOR) {
			if (mNativeDetector != null)
				mNativeDetector.detect(mGray, faces);
		} else {
			Log.e(TAG, "Detection method is not selected!");
		}

		Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(),
				Bitmap.Config.ARGB_8888);

		try {
			Utils.matToBitmap(mRgba, bmp);
		} catch (Exception e) {
			Log.e(TAG,
					"Utils.matToBitmap() throws an exception: "
							+ e.getMessage());
			bmp.recycle();
			bmp = null;
		}

		return bmp;
	}
	
	

	private void CreateAuxiliaryMats() {
		if (mGray.empty())
			return;

		int rows = mGray.rows();
		int cols = mGray.cols();

		if (mZoomWindow == null) {
			mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2
					+ cols / 10, cols);
			mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2
					+ cols / 10, cols);
		}

	}

	@Override
	public void run() {
		super.run();

		synchronized (this) {
			// Explicitly deallocate Mats
			if (mRgba != null)
				mRgba.release();
			if (mGray != null)
				mGray.release();
			if (mCascadeFile != null)
				mCascadeFile.delete();
			if (mNativeDetector != null)
				mNativeDetector.release();

			mRgba = null;
			mGray = null;
			mCascadeFile = null;
		}
	}
}
