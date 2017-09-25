/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.views;

import edu.hope.superresolution.ImageJmodifieds.ImPlusListenerDockablePanel;
import edu.hope.superresolution.ImageJmodifieds.PropertyNotifyingImagePlus;
import edu.hope.superresolution.ImageJmodifieds.UnderDockCapableStackImageWindow;
import edu.hope.superresolution.models.FiducialLocationModel;
import edu.hope.superresolution.models.LocationAcquisitionModel;
import edu.valelab.gaussianfit.utils.ReportingUtils;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    protected LocationAcquisitionDisplayWindow( PropertyNotifyingImagePlus propImPlus, ImPlusListenerDockablePanel locationModelDock ) {
        //Sloppy Code, But Create A Dummy ImagePlus that we will Repopulate upon accessing the locAcquisition
        super( propImPlus, locationModelDock );
        
        pack();
        setVisible(true);

        
    }
    
    /**
     * Effective Constructor - Creates the Window Based off of the LocationAcquisitionModel Provided
     */
    public static LocationAcquisitionDisplayWindow createLocationAcquisitionDisplayWindow( LocationAcquisitionModel locAcq ) {
        
        int numFidLocModels = locAcq.getNumFiducialLocationModels();
        
        //Create An ImageStack
        ImageStack modeledStack = ImageStack.create(locAcq.getRepresentativeImageProc().getWidth(),
                                                     locAcq.getRepresentativeImageProc().getHeight(),
                                                       locAcq.getNumFiducialLocationModels(), locAcq.getRepresentativeImageProc().getBitDepth() );
        
        //Populate the ImageStack With the Corresponding Fiducial Models, and also the 
        // Display Plugin with the fiducialLocationModels
        for( int i = 0; i < numFidLocModels; ++i ) {
                FiducialLocationModel fLocModel = locAcq.getFiducialLocationModel(i);
                //Add the Processor at the set slice
                modeledStack.setProcessor( fLocModel.getImageProcessor(), i + 1);
        }
        
        //Create An ImagePlus that notifies On updates
        PropertyNotifyingImagePlus populatedImPlus = new PropertyNotifyingImagePlus( WindowManager.getUniqueName( locAcq.getAcquisitionTitle() ) , modeledStack );
        
        return new LocationAcquisitionDisplayWindow( populatedImPlus, null );
        
    }
     

    
   
}
