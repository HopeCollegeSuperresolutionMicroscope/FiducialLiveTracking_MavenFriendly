/**
 * Find local maxima in an Image (or ROI) using the algorithm described in
 * Neubeck and Van Gool. Efficient non-maximum suppression. 
 * Pattern Recognition (2006) vol. 3 pp. 850-855
 *
 * Jonas Ries brought this to my attention and sent me C code implementing one of the
 * described algorithms
 *
 */

package edu.hope.superresolution.fitters;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.process.ImageProcessor;
import ij.plugin.filter.GaussianBlur;
import java.awt.Polygon;
import java.awt.Rectangle;


/**
 * publicClass Exposing The Methods.  Slight Alterations to Nico-Stuurman's interpretation of 
 * the class, with a more liberal selection metric for allowing noise values in.
 * 
 *
 * @author nico
 */
public class FindLocalMaxima {
   private static final GaussianBlur filter_ = new GaussianBlur();
   private static final ImageCalculator ic_ = new ImageCalculator();
   
   public enum FilterType {
      NONE,
      GAUSSIAN1_5
   }

   /**
    * Static utility function to find local maxima in an Image    
    * <p>
    *  Alias for FindMax( ImageProcessor, int, int, FilterType )
    * <p>
    * Note:  Where this function may fail is in the identification of maxima for gradually sloped
    *        Intensities.  The noiseFilter used, simply checks the next closest pixel values to see if they deviate
    *        more than the snr*noiseMax value from the Max.  Please keep this in mind if that is an
    *        expectation.  A simple fix may be to simply reduce the noiseMax symbolically if 
    *        that is an expectation.
    * 
    * @param iPlus - ImagePlus object in which to look for local maxima
    * @param n - minimum distance to other local maximum (2n is the search data centered around the the point)
    * @param threshold - value below which a maximum will be rejected
    * @param filterType - Prefilter the image.  Either none or Gaussian1_5
    * @return Polygon with maxima 
    */
   public static Polygon FindMax(ImagePlus iPlus, int n, double snr, int noiseMax, int threshold, FilterType filterType) {
      return FindMax( iPlus.getProcessor(), n , snr, noiseMax, threshold, filterType );
   }

   /**
    * Static utility function to find local maxima in an Image that are not simply the result of noise
    * <p>
    * Note:  Where this function may fail is in the identification of maxima for gradually sloped
    *        Intensities.  The noiseFilter used, simply checks the next closest pixel values to see if they deviate
    *        more than the snr*noiseMax value from the Max.  Please keep this in mind if that is an
    *        expectation.  A simple fix may be to simply reduce the noiseMax symbolically if 
    *        that is an expectation.
    * 
    * @param iProc - ImageProcessor object in which to look for local maxima
    * @param n - minimum distance to other local maximum (2n is the search data centered around the the point)
    * @param snr - The minimum Signal To Noise Ratio Acceptable (to be multiplied by noiseMax)
    * @param noiseMax- the maximum anticipated value of the noise from mean
    * @param threshold - value below which a maximum will be rejected
    * @param filterType - Prefilter the image.  Either none or Gaussian1_5
    * @return Polygon with maxima 
    */
   public static Polygon FindMax( ImageProcessor iProc, int n, double snr, int noiseMax, int threshold, FilterType filterType ) {
      
      Polygon maxima = new Polygon();
       
      Rectangle roi = iProc.getRoi();
      
      // Prefilter if needed
      switch (filterType) {
         case GAUSSIAN1_5 : 
            // TODO: if there is an ROI, we only need to filter_ in the ROI
            ImageProcessor iProcG1 = iProc.duplicate();
            ImageProcessor iProcG5 = iProc.duplicate();
            filter_.blurGaussian(iProcG1, 0.4, 0.4, 0.01); 
            filter_.blurGaussian(iProcG5, 2.0, 2.0, 0.01);
            ImagePlus p1 = new ImagePlus("G1", iProcG1);
            ImagePlus p5 = new ImagePlus("G5", iProcG5);
            ic_.run("subtract", p1, p5);
            iProc = p1.getProcessor();
                      
            break;
      }


      // divide the image up in blocks of size 2n and find local maxima
      int n2 = 2*n + 1;
      // calculate borders once
      int xRealEnd = roi.x + roi.width;
      int xEnd = xRealEnd - n;
      int yRealEnd = roi.y + roi.height;
      int yEnd = yRealEnd - n;
      for (int i=roi.x; i <= xEnd - n - 1; i+=n2) {
         for (int j=roi.y; j <= yEnd - n - 1; j+=n2) {
            int mi = i;
            int mj = j;
            for (int i2=i; i2 < i + n2 && i2 < xRealEnd; i2++) {
               for (int j2=j; j2 < j + n2 && j2 < yRealEnd; j2++) {
                  // revert getPixel to get after debugging
                  if (iProc.getPixel(i2, j2) > iProc.getPixel(mi, mj)) {
                     mi = i2;
                     mj = j2;
                  }
               }
            }
            // is the candidate really a local maximum?
            // check surroundings (except for the pixels that we already checked)
            boolean stop = false;
            // columns in block to the left
            if (mi - n < i && i>0) {
               for (int i2=mi-n; i2<i; i2++) {
                  for (int j2=mj-n; j2<=mj+n; j2++) {
                     if (iProc.getPixel(i2, j2) > iProc.getPixel(mi, mj)) {
                        stop = true;
                     }
                  }
               }
            }
            // columns in block to the right
            if (!stop && mi + n >= i + n2 ) {
               for (int i2=i+n2; i2<=mi+n; i2++) {
                   for (int j2=mj-n; j2<=mj+n; j2++) {
                     if (iProc.getPixel(i2, j2) > iProc.getPixel(mi, mj)) {
                        stop = true;
                     }
                  }
               }
            }
            // rows on top of the block
            if (!stop && mj - n < j && j > 0) {
               for (int j2 = mj - n; j2 < j; j2++) {
                  for (int i2 = mi - n; i2 <= mi + n; i2++) {
                     if (iProc.getPixel(i2, j2) > iProc.getPixel(mi, mj))
                        stop = true;
                  }
               }
            }
            // rows below the block
            if (!stop && mj + n >= j + n2) {
               for (int j2 = j + n2; j2 <= mj + n; j2++) {
                  for (int i2 = mi - n; i2 <= mi + n; i2++) {
                     if (iProc.getPixel(i2, j2) > iProc.getPixel(mi, mj))
                        stop = true;
                  }
               }
            }
            
            //Check to See if the Value is above the overall SNR permitted
            if( !stop && ( iProc.getPixel(mi, mj)  > threshold )) {
                maxima.addPoint(mi, mj);
            }
         }
      }
      
      //Filters out any Noisy Points
      return noiseFilter( iProc, maxima, (int) snr * noiseMax );
   }

   /**
    * Filters a Set of Local Maxima Points in an ImageProcessor to see if they are above the
    * assumed amplitude for the noise. 
    * 
    * @param iProc - ImageProcesor with Pixel Data Where Points were found
    * @param localMaxPoints - List of localMaximums that were discovered without Noise Detection
    * @param noiseThreshold - The allowed amplitude for expected Noise
    * @return 
    */
   public static Polygon noiseFilter(ImageProcessor iProc, Polygon localMaxPoints, int noiseThreshold)
   {
      Polygon outputPoints = new Polygon();

      for (int i=0; i < localMaxPoints.npoints; i++) {
         int x = localMaxPoints.xpoints[i];
         int y = localMaxPoints.ypoints[i];
         int value = iProc.getPixel(x, y) - noiseThreshold;
         if (    value > iProc.getPixel(x-1, y-1) ||
                 value > iProc.getPixel(x-1, y)  ||
                 value > iProc.getPixel(x-1, y+1)||
                 value > iProc.getPixel(x, y-1) ||
                 value > iProc.getPixel(x, y+1) ||
                 value > iProc.getPixel(x+1, y-1) ||
                 value > iProc.getPixel(x+1, y) ||
                 value > iProc.getPixel(x+1, y+1)
               )
            outputPoints.addPoint(x, y);
      }

      return outputPoints;
   }

}
