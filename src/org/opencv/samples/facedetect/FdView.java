package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

class FdView extends SampleCvViewBase {

    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;

	private static final String TAG = "FdView";
    private static final String CASCADE_DIR = "cascade";
    private static final String CASCADE_FILENAME = "tmp.xml";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    private Mat rgba;
	private Mat gray;
    //todo zoom stuff can be removed
	private Mat zoomCorner;
	private Mat zoomWindow;
	private Mat zoomWindow2;
	private Mat result;
	private Mat teplateR;
	private Mat teplateL;
	private File CascadeFile;
	private CascadeClassifier javaDetector;
	private CascadeClassifier cascadeER;
	private CascadeClassifier cascadeEL;
    //todo Compare native and javadetector, this option is somewhere in the menu.
	private DetectionBasedTracker nativeDetector;

	private int detectorType = JAVA_DETECTOR;

	private float relativeFaceSize = 0;
	private int absoluteFaceSize = 0;
	private int learnFrames = 0;
	private static final int MAX_LEARN_FRAMES = 5;
	private int refreshTemplateCounter = 0;
	private double matchValue;
	private Rect eyeArea = new Rect();
	
	private MinMaxLocResult oldPosLeft;
	private MinMaxLocResult oldPosRight;

	public void setMinFaceSize(float faceSize) {
		relativeFaceSize = faceSize;
		absoluteFaceSize = 0;
	}

	public void setDetectorType(int type) {
		if (detectorType != type) {
			detectorType = type;

			if (type == NATIVE_DETECTOR) {
				Log.i(TAG, "Detection Based Tracker enabled");
				nativeDetector.start();
			} else {
				Log.i(TAG, "Cascade detector enabled");
				nativeDetector.stop();
			}
		}
	}

	public void resetLearnFramesCount() {
		learnFrames = 0;
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

			// --------------------------------- load left eye classificator
			// -----------------------------------
			InputStream iser = context.getResources().openRawResource(
                    R.raw.haarcascade_lefteye_2splits);
			File cascadeDirER = context.getDir("cascadeER",
                    Context.MODE_PRIVATE);
			File cascadeFileER = new File(cascadeDirER,
					"haarcascade_eye_right.xml");
			FileOutputStream oser = new FileOutputStream(cascadeFileER);

			byte[] bufferER = new byte[4096];
			int bytesReadER;
			while ((bytesReadER = iser.read(bufferER)) != -1) {
				oser.write(bufferER, 0, bytesReadER);
			}
			iser.close();
			oser.close();
			// ----------------------------------------------------------------------------------------------------

			// --------------------------------- load right eye classificator
			// ------------------------------------
			InputStream isel = context.getResources().openRawResource(
                    R.raw.haarcascade_lefteye_2splits);
			File cascadeDirEL = context.getDir("cascadeEL",
                    Context.MODE_PRIVATE);
			File cascadeFileEL = new File(cascadeDirEL,
					"haarcascade_eye_left.xml");
			FileOutputStream osel = new FileOutputStream(cascadeFileEL);

			byte[] bufferEL = new byte[4096];
			int bytesReadEL;
			while ((bytesReadEL = isel.read(bufferEL)) != -1) {
				osel.write(bufferEL, 0, bytesReadEL);
			}
			isel.close();
			osel.close();

			// ------------------------------------------------------------------------------------------------------

			javaDetector = new CascadeClassifier(
					mCascadeFile.getAbsolutePath());
			cascadeER = new CascadeClassifier(cascadeFileER.getAbsolutePath());
			cascadeEL = new CascadeClassifier(cascadeFileER.getAbsolutePath());
			if (javaDetector.empty() || cascadeER.empty()
					|| cascadeEL.empty()) {
				Log.e(TAG, "Failed to load cascade classifier");
				javaDetector = null;
				cascadeER = null;
				cascadeEL = null;
			} else
				Log.i(TAG,
						"Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());

			nativeDetector = new DetectionBasedTracker(
					mCascadeFile.getAbsolutePath(), 0);

			cascadeDir.delete();
			cascadeFileER.delete();
			cascadeDirER.delete();
			cascadeFileEL.delete();
			cascadeDirEL.delete();

		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
		}
	}

    private CascadeClassifier loadCascade(int resourceId) {
        // We need to write our resource to a file first
        // since CascadeClassifier takes a filename...
        //todo write resource to file -> load classifier from file
        return null;
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

		if (detectorType == JAVA_DETECTOR) {
			if (javaDetector != null)
				javaDetector.detectMultiScale(gray, faces, 1.1, 2,
						2 // TODO: objdetect.CV_HAAR_SCALE_IMAGE
						, new Size(absoluteFaceSize, absoluteFaceSize),
						new Size());

			if (zoomCorner == null || zoomWindow == null)
				CreateAuxiliaryMats();

			Rect[] facesArray = faces.toArray();

            //todo test this
            int largest = 0;
            double maxArea = 0;
            for(int i = 0; i < facesArray.length; i++) {
                if(facesArray[i].area() > maxArea) {
                    largest = i;
                    maxArea = facesArray[i].area();
                }
            }

			//Get face frame
			Rect r = facesArray[largest];
				
			Scalar red 	 = new Scalar( 255, 0, 0, 255 );
			Scalar green = new Scalar( 0, 255, 0, 255 );
			Scalar blue	 = new Scalar( 0, 0, 255, 255 );
				
			Core.rectangle(gray, r.tl(), r.br(), green, 3);
			Core.rectangle(rgba, r.tl(), r.br(), green, 3);
				
			int x = r.x + r.width / 8;
			int y = (int) (r.y + (r.height / 4.5));
			int width = r.width - 2 * r.width / 8;
			int height = (int) (r.height / 3.0);
				
			eyeArea = new Rect(x ,y ,width ,height);

			Core.rectangle(rgba, eyeArea.tl(), eyeArea.br(), blue, 2);
				
				
			//Determine Right eye rectangle
			x = r.x + r.width / 16;
			y = (int) (r.y + (r.height / 4.5));
			width = (r.width - 2 * r.width / 16) / 2;
			height = (int) (r.height / 3.0);
				
			Rect eyearea_right = new Rect(x, y, width, height );
				
			//Determine Left eye right angle
			x = r.x + r.width / 16 + (r.width - 2 * r.width / 16) / 2;
			y = (int) (r.y + (r.height / 4.5));
			width = (r.width - 2 * r.width / 16) / 2;
			height = (int) (r.height / 3.0);

			Rect eyearea_left = new Rect(x, y, width, height);

			Core.rectangle(rgba, eyearea_left.tl(), eyearea_left.br(),
					new Scalar(244, 27, 175, 255), 2);
			Core.rectangle(rgba, eyearea_right.tl(), eyearea_right.br(),
					new Scalar(27, 244, 221, 255), 2);
				
			
			if (learnFrames < MAX_LEARN_FRAMES) {
				teplateR = get_template(cascadeER, eyearea_right, 24);
				teplateL = get_template(cascadeEL, eyearea_left, 24);
				
				learnFrames++;
				
			} else {
				matchValue = match_eye(eyearea_right, teplateR,
						FdActivity.method);

				matchValue = match_eye(eyearea_left, teplateL,
						FdActivity.method);
					
				Mat leftEyeGray  = gray.submat( eyearea_left);
				Mat rightEyeGray = gray.submat( eyearea_right );
				MinMaxLocResult resultLeft = Core.minMaxLoc( leftEyeGray );
				MinMaxLocResult resultRight = Core.minMaxLoc( rightEyeGray );

				Scalar meanL = Core.mean( leftEyeGray );
				Scalar meanR = Core.mean( rightEyeGray );
					
//				Log.e( TAG, "meanL: "+ meanL + " dl: " + resultLeft.minVal + " lp: " + resultLeft.minLoc + " olp: " + oldPosLeft +
//						"meanR"+ meanR +"dr: " + resultRight.minVal + " rp: " + resultRight.minLoc + " olr: " + oldPosRight );
					
				if( oldPosLeft != null && oldPosRight != null ) {
						
					Point oldPosLeftPoint = oldPosLeft.minLoc;
					Point posLeftPoint = resultLeft.minLoc;

					Point oldPosRightPoint = oldPosLeft.minLoc;
					Point posRightPoint = resultLeft.minLoc;
						
					double distanceL = Math.sqrt( (oldPosLeftPoint.x-posLeftPoint.x)*(oldPosLeftPoint.x-posLeftPoint.x) + (oldPosLeftPoint.y-posLeftPoint.y)*(oldPosLeftPoint.y-posLeftPoint.y));
					double distanceR = Math.sqrt( (oldPosRightPoint.x-posRightPoint.x)*(oldPosRightPoint.x-posRightPoint.x) + (oldPosRightPoint.y-posRightPoint.y)*(oldPosRightPoint.y-posRightPoint.y));
						
					int offset = 30;
						
					if( distanceL > offset && distanceR > offset ) {
						Log.e(TAG, "Blink");
					}
				}
					
				oldPosLeft  = resultLeft;
				oldPosRight = resultRight;
					
				refreshTemplateCounter++;
				refreshTemplateCounter = refreshTemplateCounter % 20;
				if( refreshTemplateCounter >= 19 )
					learnFrames = 0;
				}

//				Imgproc.resize(rgba.submat(eyearea_left), zoomWindow2,
//						zoomWindow2.size());
//				Imgproc.resize(rgba.submat(eyearea_right), zoomWindow,
//						zoomWindow.size());


		} else if (detectorType == NATIVE_DETECTOR) {
			if (nativeDetector != null)
				nativeDetector.detect(gray, faces);
		} else {
			Log.e(TAG, "Detection method is not selected!");
		}

		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++)
			Core.rectangle(rgba, facesArray[i].tl(), facesArray[i].br(),
					FACE_RECT_COLOR, 3);

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

	private void CreateAuxiliaryMats() {
		if (gray.empty())
			return;

		int rows = gray.rows();
		int cols = gray.cols();

		if (zoomWindow == null) {
			zoomWindow = rgba.submat(rows / 2 + rows / 10, rows, cols / 2
					+ cols / 10, cols);
			zoomWindow2 = rgba.submat(0, rows / 2 - rows / 10, cols / 2
					+ cols / 10, cols);
		}

	}

	private double match_eye(Rect area, Mat mTemplate, int type) {
		Point matchLoc;
		Mat mROI = gray.submat(area);
		int result_cols = gray.cols() - mTemplate.cols() + 1;
		int result_rows = gray.rows() - mTemplate.rows() + 1;
		if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
			return 0.0;
		}
		result = new Mat(result_cols, result_rows, CvType.CV_32FC1);

		switch (type) {
		case TM_SQDIFF:
			Imgproc.matchTemplate(mROI, mTemplate, result, Imgproc.TM_SQDIFF);
			break;
		case TM_SQDIFF_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, result,
					Imgproc.TM_SQDIFF_NORMED);
			break;
		case TM_CCOEFF:
			Imgproc.matchTemplate(mROI, mTemplate, result, Imgproc.TM_CCOEFF);
			break;
		case TM_CCOEFF_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, result,
					Imgproc.TM_CCOEFF_NORMED);
			break;
		case TM_CCORR:
			Imgproc.matchTemplate(mROI, mTemplate, result, Imgproc.TM_CCORR);
			break;
		case TM_CCORR_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, result,
					Imgproc.TM_CCORR_NORMED);
			break;
		}

		Core.MinMaxLocResult mmres = Core.minMaxLoc(result);

		if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
			matchLoc = mmres.minLoc;
		} else {
			matchLoc = mmres.maxLoc;
		}

		Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
		Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
				matchLoc.y + mTemplate.rows() + area.y);

		Core.rectangle(rgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
				255));

		if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
			return mmres.maxVal;
		} else {
			return mmres.minVal;
		}

	}

	private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
		Mat template = new Mat();
		Mat mROI = gray.submat(area);
		MatOfRect eyes = new MatOfRect();
		Point iris = new Point();
		Rect eye_template = new Rect();
		clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
				Objdetect.CASCADE_FIND_BIGGEST_OBJECT
						| Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
				new Size());

		Rect[] eyesArray = eyes.toArray();
		for (int i = 0; i < eyesArray.length; i++) {
			Rect e = eyesArray[i];
			e.x = area.x + e.x;
			e.y = area.y + e.y;
			Rect eye_only_rectangle = new Rect((int) e.tl().x,
					(int) (e.tl().y + e.height * 0.4), (int) e.width,
					(int) (e.height * 0.6));
			mROI = gray.submat(eye_only_rectangle);
			Mat vyrez = rgba.submat(eye_only_rectangle);
			Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

			Core.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
			iris.x = mmG.minLoc.x + eye_only_rectangle.x;
			iris.y = mmG.minLoc.y + eye_only_rectangle.y;
			eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
					- size / 2, size, size);
			Core.rectangle(rgba, eye_template.tl(), eye_template.br(),
					new Scalar(255, 0, 0, 255), 2);
			template = (gray.submat(eye_template)).clone();
			return template;
		}
		return template;
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
			if (mCascadeFile != null)
				mCascadeFile.delete();
			if (nativeDetector != null)
				nativeDetector.release();

			rgba = null;
			gray = null;
			mCascadeFile = null;
		}
	}
}
