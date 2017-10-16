package edu.hope.superresolution.processors;


import ij.gui.Roi;
import edu.hope.superresolution.genericstructures.ListCallback;
import edu.hope.superresolution.models.FiducialArea;
import ij.ImagePlus;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Interface that Exposes Method handles for Analyzing a Fiducial Area and returning
 * the results to that FiducialArea for storage.
 * 
 *
 * @author Microscope
 */
public interface iFiducialAreaProcessor {
    
    /**
     * Enables whether or not the fitting methods are implemented asynchronously or they wait 
     * for the the fitting method to process (and thus the resultListCallbacks to finish)
     * 
     * @param enable 
     */
    void enableAsyncProcessing( boolean enable );
    
    /**
     * Checks Whether the processor's method is meant to implement Asynchronously
     * @return 
     */
    boolean isAsyncProcessEnabled();
    
    /*
    *   @return - whether of not a fit was performed
    */
    public boolean fitRoiForImagePlus( ImagePlus ip, Roi roi, ListCallback resultListCallback );
    
    public boolean fitFiducialAreaForImagePlus( ImagePlus ip, FiducialArea fArea, ListCallback resultListCallback );
    
    /**
     * Process A FiducialArea.  Should obey the AsyncProcessing
     * settings of whether or not to wait on processing. This automatically registers the ListCallback
     * that is by default provided with the FiducialArea by the getDefaultListCallBack() method.
     * 
     * @param fArea
     * @return 
     */
    public boolean processFiducialArea( FiducialArea fArea );
    
    /**
     * Process A FiducialArea and implement the thirdPartyCallback.  This Will still implement the
     * defaultListCallBack() provided with the FiducialArea, but will also call the thirdPartyCallback in succession
     * specified to the default.  This means that a thirdPartyCallback can be provided as either a filter potentially
     * (pre-fiducialAreaCallback) or a reporter (post-fiducialAreaCallback) 
     * 
     * @param fArea - The FiducialArea to Process
     * @param thirdPartyCallback - A Third-party Instance to be called during process
     * @param preFAreaCallback - <code>true</code> if the thirdPartyCallback will be called before the defaultCallback of the FiducialArea
     *                           <code>false</code> if thirdPartyCallback will be called after
     * @return 
     */
    public boolean processFiducialArea( FiducialArea fArea, ListCallback thirdPartyCallback, boolean preFAreaCallback );
    
    //Required For BoundedSpot Size Calculations
    public double getPixelSize( );
    
}
