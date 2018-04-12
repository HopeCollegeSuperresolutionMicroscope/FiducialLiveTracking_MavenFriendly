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
import java.awt.Polygon;

/**
 *  Extension of ImageRegionFitThreadTest that provides a common base for a Gaussian 
  fit assumption.  Any Classes That extend this super class are taking advantage
 *  of the shared abstract methods discoverPointsOfInterest() and roiIsValid().  
 *  Extending classes are expected to implement their own unique FitStackThread 
 *  Instance and so, when extending to a new subclass a new FitStackThread is more
 *  than likely necessary as well.
 * <p>
 *  Due to the nature of localMaxima search assumption, This class also provides 
 *  a setter function for a prefilterType in the discoverPointsOfInterest() method.
 *  See non-abstract methods for implementation details.
 * 
 * @author Microscope
 * @see edu.hope.superresolution.fitters.FitStackThread
 * 
 */
public abstract class GenericBaseGaussianFitThread extends ImageRegionFitThreadTest {
    
    /**
     * Enumerated Value for different modes used to ensure that a selected spot
     * encompasses out to expected values.  This allows for accounting for defocusing
     * (i.e. when a gaussian smooths and presents less linearity in the original search region)
     */
    public enum DataEnsureMode{
        none, averageCentered, directionalCentering
    }
    
    //Fit Process Specific Variables
    FindLocalMaxima.FilterType preFilterType_;
   
    /**
     * General Constructor - For Pass Throughs, see super class since they are 
     * expected to be handled by the abstract class.
     * 
     * @param ip - Pass Through of ImagePlus To Super
     * @param roi - Pass Through of Constraining Roi on ImagePlus
     * @param listCallback - Pass Through of Callback for calling Context
     * @param positionString - Pass Through of Micro-manager Position String
     * @param preFilterType - PreFilterType for Finding Points of Interest
     */
    public GenericBaseGaussianFitThread( ImagePlus ip, Roi roi, FitThreadCallback<SpotData> listCallback, String positionString, 
            FindLocalMaxima.FilterType preFilterType ) {
        super( ip, roi, listCallback, positionString );
        preFilterType_ = preFilterType;
    }

    /**
     * ExtendedGaussianInfo Copy Constructor
     * 
     * @param ip - Pass Through of ImagePlus To Super
     * @param roi - Pass Through of Constraining Roi on ImagePlus
     * @param listCallback - Pass Through of Callback for calling Context
     * @param extGaussInfo - ExtendedGaussianInfo to copy to base
     * @param preFilterType - PreFilterType for Finding Points of Interest
     */
    public GenericBaseGaussianFitThread( ImagePlus ip, Roi roi, FitThreadCallback<SpotData> listCallback,
                                ExtendedGaussianInfo extGaussInfo, FindLocalMaxima.FilterType preFilterType ) {
        super( ip, roi, listCallback, extGaussInfo );
        preFilterType_ = preFilterType;
    }    
    
    /**
     *  Sets the preFilterType for analysis
     * <p> 
     *  Note: Only changes preFilterType if Thread is not currently running
     * 
     * @param preFilterType - the Type of PreFilter from edu.valelab.gaussianFit.algorithm.FindLocalMaxima
     * @return <code>true</code> if the preFilterType was set 
     *         or <code>false</code> if called while process was running
     */
    public boolean setPreFilterType( FindLocalMaxima.FilterType preFilterType ) {
        
        if( isRunning() )
        {
            return false;
        }
        
        modifyLock_.lock();
        try{
            preFilterType_ = preFilterType;
        } finally {
            modifyLock_.unlock();
        }
        
        return true;
    }
    
    /**
     * Check to see if an roi is valid to this Thread for fitting.  Must be a Positive, non-zero Area.
     * <p>
     * The public nature of this function allows for external checking.  This function
     * is also called as a contingency before running the thread.
     * 
     * @param roi The Roi to check.  Checks the internally saved roi during setter operations
     * @return <code>true</code> if roi is a positive non-zero area
     */
    @Override
    public boolean roiIsValid( Roi roi ) {
        return roi.getFloatWidth() > 0 && roi.getFloatHeight() > 0 && roi.isArea();
    }
    
    /**
     * Discovers Points of Interest for Fitting a Gaussian Through use of FindLocalMaxima.FindMax()
     * <p>
     * This Method Looks for the Maxima Within a Given Region (Abbe Limit In this Case)
     * And above a given Threshold Value for the current ImageProcessor.
     * <p>
     * <pre>
     *  Threshold Value is part of GaussianInfo super class (manipulated by setNoiseTolerance())
     *  HalfSize Value is part of GaussianInfo super class (manipulated by setSpotImageAreaHalfSize())
     *  preFilterType is manipulated by setPreFiltertype()
     * </pre>
     * 
     * @param currentImageProcessor The current ImageProcessor passed in from BaseClass
     * @return A Polygon consisting of the maximum point in a given area above a threshold
     * 
     * @see #setSpotImageAreaHalfSize(int)  
     * @see #setNoiseTolerance(int)
     * @see #setPreFilterType(edu.hope.superresolution.fitters.FindLocalMaxima.FilterType) 
     */
    @Override
    protected Polygon discoverPointsOfInterest(ImageProcessor currentImageProcessor) {
        return FindLocalMaxima.FindMax( currentImageProcessor, halfSize_, getSNR(), noiseTolerance_, getIntensityThreshold(),
                                preFilterType_);
    }

}
