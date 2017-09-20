/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.fitters;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.fitprocesses.FitProcessContainer;
import edu.hope.superresolution.genericstructures.BlockingQueueEndConditionTest;
import edu.hope.superresolution.genericstructures.FitThreadCallback;
import edu.valelab.gaussianfit.data.SpotData;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 *  Class to Perform a Gaussian Fit to an Image Within a given Roi.  This Class In Particular
  draws distinct functionality from the GaussianFitStackThreadTest Class and its mathematical
  implementations.  (See GenericBaseGaussianFitThread for implementations of 
  points of interest and the Roi constraints).
 <p>
 *  This Class is intended to operate on a single imagePlus and roi, that are passed to it before
 *  being initiated through init().  New Rois, even on the same image, would be better served with 
 *  a new instance of this thread with the Roi added.
 * 
 * @author Microscope
 * 
 * @see edu.hope.superresolution.fitters.GaussianFitStackThreadTest
 * @see edu.hope.superresolution.fitters.GaussianFit
 */
public class GaussianFitThread extends GenericBaseGaussianFitThread {
      
    //Index Values for DataEnsure Functions
    private static final int X_IDX = 0;
    private static final int Y_IDX = 1;
    private static final int HALFBOX_IDX = 2;
  
    private DataEnsureMode dataEnsureMode_ = DataEnsureMode.directionalCentering;
    
    /**
     * General Constructor - For Pass Throughs, see super class since they are 
     * expected to be handled by the abstract class.
     * 
     * @param ip - Pass Through of ImagePlus To Super
     * @param roi - Pass Through of Constraining Roi on ImagePlus
     * @param listCallback - Pass Through of Callback for calling Context
     * @param positionString - Pass Through of Micro-manager Position String
     * @param preFilterType - Pass Through of PreFilterType for Finding Points of Interest
     */
    public GaussianFitThread( ImagePlus ip, Roi roi, FitThreadCallback<SpotData> listCallback, String positionString, 
            FindLocalMaxima.FilterType preFilterType ) {
        super( ip, roi, listCallback, positionString, preFilterType );
    }

    /**
     * ExtendedGaussianInfo Copy Constructor
     * 
     * @param ip - Pass Through of ImagePlus To Super
     * @param roi - Pass Through of Constraining Roi on ImagePlus
     * @param listCallback - Pass Through of Callback for calling Context
     * @param extGaussInfo - ExtendedGaussianInfo to copy to base
     * @param preFilterType - Pass Through of PreFilterType for Finding Points of Interest
     */
    public GaussianFitThread( ImagePlus ip, Roi roi, FitThreadCallback<SpotData> listCallback,
                                ExtendedGaussianInfo extGaussInfo, FindLocalMaxima.FilterType preFilterType ) {
        super( ip, roi, listCallback, extGaussInfo, preFilterType );
    }
    
   /**
    * Presents A GaussianFitStackThreadTest based on Parameters passed from the Super Class
    * <p>
    * All Parameters Are The Requirements for a FitStackThread passed from the super run()
    * 
    * @return A GaussianFitStackThreadTest that will perform Basic Gaussian Fits
    * 
    * @see GaussianFitStackThreadTest
    * @see #super
    */
    @Override
    protected FitStackThread createFitStackThreadInstance(BlockingQueue<SpotData> sourceList, BlockingQueueEndConditionTest<SpotData> endCondTest, List<SpotData> resultList, ImagePlus siPlus, int halfSize, int shape, FitProcessContainer.OptimizationModes fitMode) {
        return new GaussianFitStackThreadTest(sourceList, endCondTest, resultList, 
                 siPlus, halfSize, shape, fitMode );
    }

    /** 
     *  Filters out any Spots that are too close to the edge to be fully fit, as
     *  well as produces varied widths in the imageProcessor in the case of defocusing
     *  so that there is enough variation across data (to approximately 1.5 sigma)
     * 
     * @param ip - The image Processor corresponding to this spot
     * @param channel - The Channel Number as parsed by the super class
     * @param slice - The Slice Number as parsed by the super class
     * @param frame - the Frame Number as parsed by the super class
     * @param position - z-position number in the original stack
     * @param spotIdx - The Image Index Number for the area that the spot belongs to
     * @param x - the x image position (pixel) of the maximum this spot is nearby
     * @param y - the y image position (pixel) of the maximum this spot is nearby
     * @return - A Spot with width at least the abbe limit or 3*sigma around the 
     *           maximum to identifying this spot.  <code>null</code> otherwise
     */
    @Override
    protected SpotData produceSpot(  ImageProcessor ip, int channel,
                                              int slice, int frame, int position, 
                                              int spotIdx, int x, int y ) {         
        
        if (x > halfSize_ && x < ip.getWidth() - halfSize_
                && y > halfSize_ && y < ip.getHeight() - halfSize_) {
            //Check to make sure the edge pixel average is greater than approx 1.5*sigma
            //It is assumed a user has already specified 2*sigma abbe Limit
            int maxValue = ip.get(x, y) - (int) baseLevel_;
            double compValue = .5 * maxValue + baseLevel_;
            //Checks in case there is a problem with fitting parameters to avoid looping
            if( compValue - baseLevel_ < noiseTolerance_ ) {
                //ij.IJ.log( "Removed Spot Compared to BaseLevel ");
                return null;
            }
            int halfBox = halfSize_;
            
            //In case there's no ensure mode
            int[] values =new int []{x, y, halfBox};
            switch( getDataEnsureMode() ) {
                case averageCentered:
                    values = scaleSetToAverage( x, y, halfSize_, ip, compValue );
                    break;
                case directionalCentering:
                    values = scaleSettoDirectionalValues( x, y, halfSize_, ip, compValue );
                    break;
            }
            ImageProcessor sp = SpotData.getSpotProcessor(ip,
                    values[HALFBOX_IDX], values[X_IDX], values[Y_IDX]);
             
            if (sp != null) {
                //Since the initial Fit Criteria is supposed to be based around a maximum
                //Any shift in the x and y needs to be within the maximum box that was found
                if( Math.abs( values[X_IDX] - x ) < halfSize_ 
                        && Math.abs( values[Y_IDX] - y ) < halfSize_ ) {
                    //Such a Dumb way to lump getters and setters with the setData thing
                    return new SpotData(sp, channel, slice, frame,
                            position, spotIdx, values[X_IDX], values[Y_IDX]);
                }
                else {
                    ij.IJ.log( "position rejection");
                }
            }
            else {
                ij.IJ.log("null spotProc Rejection");
            }
        }
        else {
            ij.IJ.log("Rejected For Being Too Close");
        }
        return null;
    }
    
    /**
     * One Solution to Scaling the Data Set to ensure it includes the correct values.
     * This solution takes the x and y values a distance from the halfBox centered 
     * on the maximum value.  When that value is below the given threshold, the new box
     * size is returned to match.
     * 
     * @param x - the x position of the max Value
     * @param y - the y position of the max Value
     * @param halfBox - the halfSize of the initial finding box
     * @param iProc - The Image Processor in which these coordinates correspond
     * @param thresholdVal - Threshold Value for the edge of the Gaussian to accept
     * @return - returns an array of the [x, y, adjusted halfBox]
     */
    int[] scaleSetToAverage( int x, int y, int halfBox, ImageProcessor iProc, double thresholdVal ) {
            
            int avgEdges;
            do {
                avgEdges = 0;
                avgEdges += iProc.getPixel(x - halfBox, y);
                avgEdges += iProc.getPixel(x + halfBox, y);
                avgEdges += iProc.getPixel(x, y - halfBox);
                avgEdges += iProc.getPixel(x, y + halfBox);
                avgEdges /= 4;
                halfBox++;
            } while (avgEdges > thresholdVal);
            halfBox--;
            
            return new int[]{ x, y, halfBox};
    }
    
    /**
     * One Solution to Scaling the Data Set to ensure it includes the correct values.
     * This solution individually moves the x and y values away from each pixel at 
     * the maxValue until they are below the thresholdVal to produce a data set of certain
     * pixels or until there is an obvious shift in the x and y only (shared edge)
     * 
     * @param x - the x position of the max Value
     * @param y - the y position of the max Value
     * @param halfBox - the halfSize of the initial finding box
     * @param iProc - The Image Processor in which these coordinates correspond
     * @param thresholdVal - Threshold Value for the edge of the Gaussian to accept
     * @return - returns an array of the [x, y, adjusted halfBox]
     */
    int[] scaleSettoDirectionalValues( int x, int y, int halfBox, ImageProcessor iProc, double thresholdVal ) {
            boolean leftLow = false, rightLow = false, topLow = false, botLow = false;
            int prevLeft = 0, prevRight = 0, prevTop = 0, prevBot = 0;
            int val;
            int lx = 0, rx = 0, ty = 0, by = 0;
            //int max = (int) Math.ceil( widthMax_ / pixelSize_ );
            int noiseAmplitude = getNoiseTolerance();
            do {
                if (!leftLow) {
                    val = iProc.getPixel(x - (halfBox + lx), y);
                    if ( val < thresholdVal || 
                            (prevLeft > 0 && val > prevLeft + noiseAmplitude ) ) {
                        leftLow = true;
                    } else {
                        prevLeft = val;
                        lx++;
                    }
                }
                if (!rightLow) {
                    val = iProc.getPixel(x + (halfBox + rx), y);
                    if ( val < thresholdVal || 
                            (prevRight > 0 && val > prevRight + noiseAmplitude ) ) {
                        rightLow = true;
                    } else {
                        prevRight = val;
                        rx++;
                    }
                }
                if (!topLow) {
                    val = iProc.getPixel(x, y + (halfBox + ty));
                    if (val  < thresholdVal || 
                            (prevTop > 0 && val > prevTop + noiseAmplitude ) ) {
                        topLow = true;
                    } else {
                        prevTop = val;
                        ty++;
                    }
                }
                if (!botLow) {
                    val = iProc.getPixel(x, y - (halfBox + by));
                    if ( val < thresholdVal ||
                            (prevBot > 0 && val > prevBot + noiseAmplitude ) ) {
                        botLow = true;
                    } else {
                        prevBot = val;
                        by++;
                    }
                }
            }while ( (leftLow && rightLow && topLow && botLow) == false );
            //Readjust x to the center
            double xdiffMax = Math.abs( rx - lx );
            double ydiffMax = Math.abs( ty - by );
            int xTotal = rx + lx, yTotal = by + ty;
            //x += (rx > lx ) ? (int) Math.ceil( xdiffMax/2 ) : (int) -1 * Math.ceil( xdiffMax/2 );
            //y += (ty > by ) ? (int) Math.ceil( ydiffMax/2 ) : (int) -1 * Math.ceil( ydiffMax/2 );
            halfBox += (xTotal < yTotal) ? xTotal/2 : yTotal/2;
            
            return new int[]{ x, y, halfBox };
    }

}
