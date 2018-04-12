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
import edu.valelab.gaussianfit.data.GaussianInfo;
import edu.valelab.gaussianfit.data.SpotData;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 *  Class to Perform a Gaussian Fit With Simple Defocus Term (Zernike[2,0]) to an
 *  Image Within a given Roi.  This Class In Particular draws distinct functionality
 *  from the GaussianWithDefocusFitStackThread Class and its mathematical implementations.  
 *  (See GenericBaseGaussianFitThread for implementations of points of interest 
 *  and the Roi constraints).
 * <p>
 *  This Class is intended to operate on a single imagePlus and roi, that are passed to it before
 *  being initiated through init().  New Rois, even on the same image, would be better served with 
 *  a new instance of this thread with the Roi added.
 * 
 * @author Microscope
 * 
 * @see edu.hope.superresolution.fitters.GaussianWithDefocusFitStackThread
 * @see edu.hope.superresolution.fitters.GaussianFit
 */
public class GaussianWithSimpleDefocusFitThread extends GenericBaseGaussianFitThread {
   
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
    public GaussianWithSimpleDefocusFitThread( ImagePlus ip, Roi roi, FitThreadCallback<SpotData> listCallback, String positionString, 
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
    public GaussianWithSimpleDefocusFitThread( ImagePlus ip, Roi roi, FitThreadCallback<SpotData> listCallback,
                                ExtendedGaussianInfo extGaussInfo, FindLocalMaxima.FilterType preFilterType ) {
        super( ip, roi, listCallback, extGaussInfo, preFilterType );
    }
    
   /**
    * Presents A GaussianWithDefocusFitStackThread based on Parameters passed from the Super Class
    * <p>
    * All Parameters Are The Requirements for a FitStackThread passed from the super run()
    * 
    * @return A GaussianWithDefocusFitStackThread that will perform Gaussian With Simple Defocus Fits
    * 
    * @see GaussianWithDefocusFitStackThread
    */
    @Override
    protected FitStackThread createFitStackThreadInstance(BlockingQueue<SpotData> sourceList, BlockingQueueEndConditionTest<SpotData> endCondTest, List<SpotData> resultList, ImagePlus siPlus, int halfSize, int shape, FitProcessContainer.OptimizationModes fitMode) {
        return new GaussianWithDefocusFitStackThread(sourceList, endCondTest, resultList, 
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
            int maxValue = ip.get(x, y);
            int halfBox = halfSize_;
            int avgEdges = 0;
            do {
                avgEdges = 0;
                avgEdges += ip.get(x - halfBox, y);
                avgEdges += ip.get(x + halfBox, y);
                avgEdges += ip.get(x, y - halfBox);
                avgEdges += ip.get(x, y + halfBox);
                avgEdges /= 4;
                halfBox += 1;
            } while (avgEdges > maxValue * .33);
            ImageProcessor sp = SpotData.getSpotProcessor(ip,
                    halfBox, x, y);
            if (sp != null) {
                return new SpotData(sp, channel, slice, frame,
                        position, spotIdx, x, y);
            }
        }
        return null;
    }

}
