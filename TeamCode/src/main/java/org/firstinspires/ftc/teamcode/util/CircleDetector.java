package org.firstinspires.ftc.teamcode.util;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import org.firstinspires.ftc.teamcode.roboticslibrary.FXTCamera;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

/**
 * Created by Windows on 2017-03-24.
 */

public class CircleDetector {

        static Scalar lowBlue = new Scalar(120, 100, 0);
        static Scalar highBlue = new Scalar(160, 255, 255);

        static Scalar lowRed1 = new Scalar(0, 100, 100);
        static Scalar highRed1 = new Scalar(20, 255, 255);

        static Scalar lowRed2 = new Scalar(240, 100, 100);
        static Scalar highRed2 = new Scalar(255, 255, 255);

        static boolean blueAlliance = false;

        public final static Scalar OTHER_BLUE_AVG = new Scalar(140, 250, 127);
        public final static Scalar OTHER_RED_AVG = new Scalar(252, 219, 177);


    public static double [] findBestCircle(Bitmap bit, double vCamFOV, double hCamFOV) {

        Mat imgOriginal = OCVUtils.bitmapToMat(bit, CvType.CV_8UC4);
        Imgproc.cvtColor(imgOriginal, imgOriginal, Imgproc.COLOR_RGBA2BGR);

        if (blueAlliance) {
            Imgproc.rectangle(imgOriginal, new Point(imgOriginal.cols(), 0), new Point(imgOriginal.cols() * 0.53, imgOriginal.rows()), new Scalar(0, 0, 255), -1);
        } else {
            Imgproc.rectangle(imgOriginal, new Point(imgOriginal.cols(), 0), new Point(imgOriginal.cols() * 0.53, imgOriginal.rows()), new Scalar(255, 0, 0), -1);
        }//else

        double shrink = 1000.0 / imgOriginal.cols();

        Size scaled = new Size(1000, 1000 * imgOriginal.height() / imgOriginal.width());

        Mat shrunk = new Mat();
        Imgproc.resize(imgOriginal, shrunk, scaled);
        Mat imgHSV = new Mat();

        Imgproc.cvtColor(shrunk, imgHSV, Imgproc.COLOR_BGR2HSV_FULL);

        Mat imgThresholded = new Mat();

        if(blueAlliance){
            Core.inRange(imgHSV, lowBlue, highBlue, imgThresholded);
        } else {
            Mat red1 = new Mat();
            Mat red2 = new Mat();
            Core.inRange(imgHSV, lowRed1, highRed1, red1);
            Core.inRange(imgHSV, lowRed2, highRed2, red2);
            Core.add(red1,  red2, imgThresholded);

        }//else

        // morphological opening (removes small objects from the foreground)
        Size morph = new Size(10, 10);

        Imgproc.erode(imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph));
        Imgproc.dilate(imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph));

        // morphological closing (removes small holes from the foreground)
        Imgproc.dilate(imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph));
        Imgproc.erode(imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph));


        Mat circles = new Mat();

        Imgproc.HoughCircles(imgThresholded, circles, Imgproc.CV_HOUGH_GRADIENT, 5, 100, 100, 10, 30, 100);

        Imgproc.cvtColor(imgThresholded, imgThresholded, Imgproc.COLOR_GRAY2BGR);

        Log.i("Data", "" + circles.cols());

        for(int i = 0; !circles.empty() && (i < circles.cols() && i < 3); i++) {
            double[] vals = circles.get(0, i);

            Log.i("Circle!", Arrays.toString(vals));

            Rect bounds = new Rect(new Point(vals[0] - vals[2], vals[1] - vals[2]), new Point(vals[0] + vals[2], vals[1] + vals[2]));
            Mat subImage = safeSubMat(bounds, imgThresholded);

            Scalar result = Core.mean(subImage);

            if (result.val[0] < 150) {
                continue;
            }

            if (Math.PI * vals[2] * vals[2] > 100000) {
                continue;
            }


//                Rect bigBounds = scaleRect(bounds, 1.0 / shrink);
//                Mat originalSubImage = safeSubMat(bigBounds, imgOriginal);
//                Imgproc.cvtColor(originalSubImage, originalSubImage, Imgproc.COLOR_BGR2GRAY);
//
//                Mat edges = new Mat();
//
//                Imgproc.medianBlur(originalSubImage, edges, 3);
//                Imgproc.threshold(edges, edges, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
//
//                ArrayList<MatOfPoint> contours = new ArrayList<>();
//                Mat hierarchy = new Mat();
//                Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
//
//
//                Imgproc.drawContours(originalSubImage, contours, 1, new Scalar(255));
//
//                int numEllipses = 0;
//                for(int j = 0; j < contours.size(); j++) {
//                    MatOfPoint2f temp = new MatOfPoint2f();
//
//                    contours.get(j).convertTo(temp, CvType.CV_32F);
//                    if (temp.size().area() < 5) continue;
//                    RotatedRect ellipse = Imgproc.fitEllipse(temp);
//
//                    Mat elli = new Mat(ellipse.boundingRect().width, ellipse.boundingRect().height, CvType.CV_8UC1);
//                    Imgproc.ellipse(elli, ellipse, new Scalar(0));
//
//                    if (ellipse.boundingRect().area() > 1000 && ellipse.boundingRect().area() < bounds.area() && Imgproc.matchShapes(temp, elli, 1, 0) < 1.5) {
//                        numEllipses++;
//                        Imgproc.ellipse(originalSubImage, ellipse, new Scalar(0, 0, 0), 1);
//                        System.out.println(Imgproc.matchShapes(temp, elli, 1, 0));
//                        System.out.println(ellipse.boundingRect().area());
//                    }
//                }
//
//                if(numEllipses == 0) {
//                    continue;
//                }//if

//                Imgproc.circle(shrunk, new Point(vals[0], vals[1]), (int) vals[2], new Scalar(((i == 0)?255:0), 255, 0), 10);

                return new double[] {vals[0], vals[1], vals[2], shrunk.rows(), shrunk.cols()};
            }

//            Bitmap save = OCVUtils.matToBitmap(imgThresholded);
//            FXTCamera.saveBitmap(save, "check4");

            return new double[]{-1, -1, -1};
        }

        public static Rect scaleRect(Rect r, double scale){
            Point tl = new Point(r.tl().x * scale, r.tl().y * scale);
            Point br = new Point(r.br().x * scale, r.br().y * scale);
            return new Rect(tl, br);
        }

        public static Mat safeSubMat(Rect bounds, Mat outer){


            if(bounds.br().x > outer.cols())
                bounds = new Rect(bounds.tl(), new Point(outer.cols(), bounds.br().y));

            if(bounds.br().y > outer.rows())
                bounds = new Rect(bounds.tl(), new Point(bounds.br().x, outer.rows()));

            if(bounds.tl().x < 0)
                bounds = new Rect(new Point(0, bounds.tl().y), bounds.br());

            if(bounds.tl().y < 0)
                bounds = new Rect(new Point(bounds.tl().x, 0), bounds.br());

            return outer.submat(bounds);
        }


        public static double[] findBestCircle2(Bitmap bit) {

            long startTime = System.nanoTime();

            Mat read = OCVUtils.bitmapToMat(bit, CvType.CV_8UC4);
            Imgproc.cvtColor(read, read, Imgproc.COLOR_RGBA2BGR);

            Imgproc.pyrDown(read, read);

            if (blueAlliance) {
                Imgproc.rectangle(read, new Point(read.cols(), 0), new Point(read.cols() * 0.53, read.rows()), new Scalar(0, 0, 255), -1);
            } else {
                Imgproc.rectangle(read, new Point(read.cols(), 0), new Point(read.cols() * 0.53, read.rows()), new Scalar(255, 0, 0), -1);
            }//else

            ArrayList<Mat> channels = new ArrayList<>();
            Core.split(read, channels);

            Mat transfer = new Mat();
            Imgproc.equalizeHist(channels.get(0), transfer);
            channels.set(0, transfer);

            transfer = new Mat();
            Imgproc.equalizeHist(channels.get(1), transfer);
            channels.set(1, transfer);

            transfer = new Mat();
            Imgproc.equalizeHist(channels.get(2), transfer);
            channels.set(2, transfer);

            Core.merge(channels, read);
            Imgproc.cvtColor(read, read, Imgproc.COLOR_BGR2HSV_FULL);

            Imgproc.medianBlur(read, read, 5);


            if (blueAlliance) {
                Core.absdiff(read, OTHER_BLUE_AVG, read);
            } else {
                Core.absdiff(read, new Scalar(0, 235, 177), transfer);
                Core.absdiff(read, new Scalar(255, 235, 177), read);
                Core.min(read, transfer, read);
            }//else

            Core.split(read, channels);

            Core.addWeighted(channels.get(0), 0.8, channels.get(1), 0.2, 0, read);

            Imgproc.threshold(read, read, 20, 255, Imgproc.THRESH_BINARY_INV);

            // morphological opening (removes small objects from the foreground)
            Size morph = new Size(5, 5);

            Imgproc.erode(read, read, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph), new Point(-1, -1), 1);
            Imgproc.dilate(read, read, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph), new Point(-1, -1), 1);

            // morphological closing (removes small holes from the foreground)
            Imgproc.dilate(read, read, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph));
            Imgproc.erode(read, read, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph));

            Mat circles = new Mat();
            Imgproc.HoughCircles(read, circles, Imgproc.CV_HOUGH_GRADIENT, 5, 100, 100, 10, 30, 100);

            if (circles.cols() > 0) {
                double[] circle = circles.get(0, 0);

                return new double[] {circle[0], circle[1], circle[2], read.rows(), read.cols()};
            }//if

//            for (int i = 0; i < circles.cols(); i++) {
//                double[] circle = circles.get(0, i);
//
//                Rect r = new Rect(new double[] {circle[0] - circle[2], circle[1] - circle[2], circle[2] * 2, circle[2] * 2});
//                r.x = Math.max(r.x, 0);
//                r.y = Math.max(r.y, 0);
//                r.width = Math.min(r.width, read.cols() - r.x);
//                r.height = Math.min(r.height, read.rows() - r.y);
//
//                if (r.x > 0 && r.y > 0 && r.x + r.width < read.cols() && r.y + r.height < read.rows()) {
//                    int numPixels = Core.countNonZero(new Mat(read, r));
//
//                    if (numPixels / r.area() > 0.1) {
//                        double angle = 36 * ((circle[1] / read.rows()) - 0.5);
//
//                        return new double[] {circle[0], circle[1], circle[2], read.rows(), read.cols()};
//                    }//if
//                }//if
//
//
//
//            }//for

            return new double[] {-1, -1, -1, read.rows(), read.cols()};
        }


}
