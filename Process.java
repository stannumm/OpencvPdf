import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
public class Process {

	final static ITesseract tessInstance = new Tesseract();

	public static List<String> run() throws TesseractException, IOException {
		
    	String file ="C:\\Users\\ahmet\\eclipse-workspace\\OpenCVTest".replace('\\', '/')+"/test.png"; 
    	String template ="C:\\Users\\ahmet\\eclipse-workspace\\OpenCVTest".replace('\\', '/')+"/template.png"; 
		
	     
	    Mat imgMat,originalImgMat,kernel,templateMat;

		System.loadLibrary( Core.NATIVE_LIBRARY_NAME ); 
	     
	    Imgcodecs imageCodecs = new Imgcodecs(); 
	     
	    originalImgMat = imageCodecs.imread(file); 
	    templateMat = imageCodecs.imread(template);
	    imgMat = originalImgMat.clone();
	     
	    //process(templateMat, new Size(20,20));
	    process(imgMat, new Size(20,20));
	    	    	     	    	    
	    //----------find 20,20 kernel mass points-----------	     
	    List<MatOfPoint> hullList = getHulls(imgMat);
	    
	    List<Point> points = pointMassCentre(getMoments(hullList));
	    
	    //------make map of hull and point data-------
	    LinkedHashMap<Point, MatOfPoint> hullCentreMap = new LinkedHashMap<Point,MatOfPoint>();
	    
	    System.out.println(points.size());
	    //------check if point inside a contour and make out of it------
	    int x = 20, y = 20;

	    do {
	    	Mat tempMat = originalImgMat.clone();
	    	process(tempMat,new Size(x,y));
	    	hullList = getHulls(tempMat);
	    	
	    	List<Point> mapPoints = pointMassCentre(getMoments(hullList));
	    	
	    	
	    	for (int i = 0 ;i<hullList.size();i++){
	    		
	    		int insidePoints = 0, iterateIndex = i;
	    		for(Point p : points) 
	    			if(Imgproc.pointPolygonTest(new MatOfPoint2f(hullList.get(i).toArray()), p, false) > 0)
	    				insidePoints++;

	    		if(insidePoints < 8 && insidePoints > 4) {
	    			
	    			Optional<Point> temp = hullCentreMap.keySet().stream()
	    			.filter(k -> pointDistance(k, mapPoints.get(iterateIndex)) < 50 )
	    			.findFirst();
	    				
	    			if(temp.isPresent()) {
	    				hullCentreMap.remove(temp.get());
	    			}
    				hullCentreMap.put(mapPoints.get(i), hullList.get(i));

	    		}	    			    		

	    	}

	    	x += 10;
	    	y += 10;
	    }while(y<150); //!hullCentreMap.values().stream().anyMatch(i -> i > 5)


	    
	    List<String> questions = new ArrayList<String>();
	    
	    for(Mat m : hullCentreMap.values().stream().collect(Collectors.toList())){
	    	
	    	List<Point> p = getLocs(m);
	    		    	
	    	//Imgproc.rectangle(originalImgMat, p.get(0),p.get(1), new Scalar(0,0,255,255),10);
	    	
	    	questions.add(tess(new Mat(originalImgMat,new Rect(p.get(0),p.get(1)))));
	    	
	    }
	    
	    
	    return questions;

	    
	    
	}
	
	
	public static String tess(Mat m) throws TesseractException, IOException {						
		
		return tessInstance.doOCR(Mat2BufferedImage(m));
		
	}
	
	public static BufferedImage Mat2BufferedImage(Mat mat) throws IOException{
	      //Encoding the image
	      MatOfByte matOfByte = new MatOfByte();
	      Imgcodecs.imencode(".jpg", mat, matOfByte);
	      //Storing the encoded Mat in a byte array
	      byte[] byteArray = matOfByte.toArray();
	      //Preparing the Buffered Image
	      InputStream in = new ByteArrayInputStream(byteArray);
	      BufferedImage bufImage = ImageIO.read(in);
	      return bufImage;
	   }
	
	/**
	 * Gives contour min max points
	 * @param 2 channeled mat
	 * @return min-max points of contour as list
	 */
	public static List<Point> getLocs(Mat m) {
		List<Mat> mv = new ArrayList<Mat>();
		List<Point> points = new ArrayList<Point>(2);
		Core.split(m, mv);
		
		MinMaxLocResult res = Core.minMaxLoc(mv.get(0));
		MinMaxLocResult res2 = Core.minMaxLoc(mv.get(1));
		
		points.add(new Point(res.maxVal,res2.maxVal));
		points.add(new Point(res.minVal,res2.minVal));
		
		return points;
		}
	/**
	 * 
	 * @param p1
	 * @param p2
	 * @return Euclidian distance of p1,p2 
	 */
	public static double pointDistance(Point p1, Point p2) {
		return Math.sqrt((Math.pow(p1.x-p2.x, 2) + Math.pow(p1.y-p2.y, 2)));
		
	}
	/**
	 * Template search over image
	 * @param imgMat -> processing mat
	 * @param templateMat -> template
	 * @param originalImgMat -> original mat for drawing rect
	 */
	public static void templateMatch(Mat imgMat, Mat templateMat, Mat originalImgMat) {
		Mat result = new Mat();
		Imgproc.matchTemplate(imgMat, templateMat, result , Imgproc.TM_CCOEFF);
		
		MinMaxLocResult mmr = Core.minMaxLoc(result);
        Point matchLoc=mmr.maxLoc;
       
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());        
        
        Imgproc.rectangle(originalImgMat, matchLoc, new Point(matchLoc.x + templateMat.cols(),
                matchLoc.y + templateMat.rows()), new Scalar(0,0, 255, 255),10);
	}
	
	/**
	 * Converts grayscale
	 * Applies otsu threshold
	 * Dilates using kernel with specified kernelSize
	 * @param imgMat
	 * @param kernelSize
	 */
	public static void process(Mat imgMat, Size kernelSize) {
		Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_BGR2GRAY);
	    
	    Imgproc.threshold(imgMat, imgMat, 0, 255,
	                Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY_INV);
	     
	    Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
	    		kernelSize);
	     
	    Imgproc.dilate(imgMat, imgMat, kernel);
	}
	/**
	 * Draw contours 
	 * @param originalImgMat
	 * @param contourList
	 */
	public static void drawContour(Mat originalImgMat, List<MatOfPoint> contourList) {
		 for (int i = 0; i < contourList.size(); i++) 
	            Imgproc.drawContours(originalImgMat, contourList, i, new Scalar(0, 0, 255, 255),
	                    2, 8, new Mat(), 0, new Point());
	}
	/**
	 * Returns moments of given contours in order to find mass center 
	 * @param hullList
	 * @return
	 */
	public static List<Moments> getMoments(List<MatOfPoint> hullList) {
		List<Moments> mu = new ArrayList<>(hullList.size());
		 //List<Double> areas = new ArrayList<Double>(hullList.size());
	     for (int i = 0; i < hullList.size(); i++) {
	            mu.add(Imgproc.moments(hullList.get(i)));
	            //areas.add(Imgproc.contourArea(hullList.get(i)));
	        }
	     return mu;
	}
	/**
	 * Returns mass center points with moments
	 * @param mu
	 * @return
	 */
	public static List<Point> pointMassCentre(List<Moments> mu) {
		List<Point> points = new ArrayList<Point>();
		Moments m = null;
	    for(int i = 0 ;i<mu.size();i++) {
	    	 m = mu.get(i);
	    	 points.add(new Point(m.m10/m.m00,m.m01/m.m00));
	    }
	    return points;
	}
	/**
	 * Returns approximated line of given mat
	 * @param imgMat
	 * @return
	 */
	public static MatOfPoint2f getPoly(Mat imgMat) {
		List<MatOfPoint> contourList = new ArrayList<>();
		
		Imgproc.findContours(imgMat, contourList, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		double maxArea = 0;
	       MatOfPoint max_contour = new MatOfPoint();

	       Iterator<MatOfPoint> iterator = contourList.iterator();
	       while (iterator.hasNext()){
	           MatOfPoint contour = iterator.next();
	           double area = Imgproc.contourArea(contour);
	           if(area > maxArea){
	               maxArea = area;
	               max_contour = contour;
	           }
	       }
	       double epsilon = 0.1*Imgproc.arcLength(new MatOfPoint2f(max_contour.toArray()),true);
	       MatOfPoint2f approx = new MatOfPoint2f();
	       Imgproc.approxPolyDP(new MatOfPoint2f(max_contour.toArray()),approx,epsilon,true);
	
	       
	       return approx;
	}
	/**
	 * Returns convexed contours ??
	 * @param imgMat
	 * @return
	 */
	public static List<MatOfPoint> getHulls(Mat imgMat) {
		
	    List<MatOfPoint> contourList = new ArrayList<>();
		
		Imgproc.findContours(imgMat, contourList, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		List<MatOfPoint> hullList = new ArrayList<>();
		
        for (MatOfPoint contour : contourList) {
            MatOfInt hull = new MatOfInt();
            Imgproc.convexHull(contour, hull);
            Point[] contourArray = contour.toArray();
            Point[] hullPoints = new Point[hull.rows()];
            List<Integer> hullContourIdxList = hull.toList();
            for (int i = 0; i < hullContourIdxList.size(); i++) {
                hullPoints[i] = contourArray[hullContourIdxList.get(i)];
            }
            hullList.add(new MatOfPoint(hullPoints));
        }
        
        return contourList;
	}
	
	public static ImageIcon imageScale(Image i, int x, int y) {
		return new ImageIcon(new ImageIcon(i).getImage().
				getScaledInstance(x, y, Image.SCALE_DEFAULT));
	}
	
	public static BufferedImage createAwtImage(Mat mat) {

	    int type = 0;
	    if (mat.channels() == 1) {
	        type = BufferedImage.TYPE_BYTE_GRAY;
	    } else if (mat.channels() == 3) {
	        type = BufferedImage.TYPE_3BYTE_BGR;
	    } else {
	        return null;
	    }

	    BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
	    WritableRaster raster = image.getRaster();
	    DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
	    byte[] data = dataBuffer.getData();
	    mat.get(0, 0, data);

	    return image;
	}

}
