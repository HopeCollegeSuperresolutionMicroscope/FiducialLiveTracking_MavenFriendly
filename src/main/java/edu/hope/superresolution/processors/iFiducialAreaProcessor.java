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
 *
 * @author Microscope
 */
public interface iFiducialAreaProcessor {
    
    //Enable Whether This processor's methods are joined
    void enableAsyncProcessing( boolean enable );
    
    //Check whether the processor's methods are meant to be async
    boolean isAsyncProcessEnabled();
    
    /**
    *   @return - whether of not a fit was performed
    */
    public boolean fitRoiForImagePlus( ImagePlus ip, Roi roi, ListCallback resultListCallback );
    
    public boolean fitFiducialAreaForImagePlus( ImagePlus ip, FiducialArea fArea, ListCallback resultListCallback );
    
    //Required For BoundedSpot Size Calculations
    public double getPixelSize( );
    
}
