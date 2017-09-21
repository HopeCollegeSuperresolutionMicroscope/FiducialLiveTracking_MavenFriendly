/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.views;

import edu.hope.superresolution.ImageJmodifieds.PropertyNotifyingImagePlus;
import edu.hope.superresolution.ImageJmodifieds.UnderDockCapableStackImageWindow;
import edu.hope.superresolution.models.LocationAcquisitionModel;
import ij.ImagePlus;

/**
 *
 * @author Desig
 */
public class LocationAcquisitionDisplayWindow extends UnderDockCapableStackImageWindow {
    
    /**
     * General Constructor - A Window is created By the LocationAcquisition, Pairing 
     * Every Fiducial Location Model to the Slices.
     * 
     * @param locAcq 
     */
    public LocationAcquisitionDisplayWindow( LocationAcquisitionModel locAcq ) {
        super( new PropertyNotifyingImagePlus("This is a title"), null );
        
    }
    
}
