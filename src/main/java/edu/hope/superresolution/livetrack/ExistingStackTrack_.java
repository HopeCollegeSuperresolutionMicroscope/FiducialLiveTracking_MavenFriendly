/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.livetrack;

import edu.hope.superresolution.autofocus.FiducialAutoFocus;
import edu.hope.superresolution.models.FiducialLocationModel;
import edu.hope.superresolution.models.LocationAcquisitionModel;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import org.micromanager.utils.AutofocusManager;
import org.micromanager.utils.MMException;


/**
 *
 * @author Microscope
 */
public class ExistingStackTrack_ implements PlugInFilter {

    private ImagePlus imp_;
    private LocationAcquisitionModel locAcq_;
    
    @Override
    public int setup(String arg, ImagePlus imp) {
       
       if( imp == null ) {
            return STACK_REQUIRED;
       }
        
       imp_ = imp;
       //Add This imp_ and its ImageWindow to LocationAcquisition
       ImageWindow impWindow;
       if( imp_.isVisible() ) {
            impWindow = imp_.getWindow();
       } else {
           //Somehow the ImageWindow was removed in the middle of this
           //This should attach itself to imp_, weirdly non-transparent
           impWindow = new ImageWindow(imp_);
       }
       
        //Create An Action Listener for Clicking on the track Button
        //This One Assumes a stackImagePlus
        LocationAcquisitionModel.AcquisitionSubmitAction trackAction = new LocationAcquisitionModel.AcquisitionSubmitAction() {
            
            @Override
            public void submitResponse( ) {
                //This is a rudimentary Way of utilizing the current Classes
                int numSlices = imp_.getNSlices();
                for( int i = 1; i <= numSlices; i++ ) {
                    imp_.setSlice(i);
                    FiducialLocationModel fModel = locAcq_.pushNextFiducialLocationModel(imp_.getProcessor(), true);
                    
                }
            } 
        };
       
        //Need to change for plugin Instance changes
       locAcq_ = new LocationAcquisitionModel( impWindow, trackAction, null );
       //This is okay because we've separated Fiducial Form and response
       locAcq_.enableSelectedLocationModelGUIs(true);
       
       return DOES_8G+DOES_16+STACK_REQUIRED+PlugInFilter.DOES_STACKS;
        
    }
    
    @Override
    public void run(ImageProcessor ip) {
        //This unfortunately is immediately and asynchronously run on every image in a stack
        //This may be statically interrupted if we'd like to do it?
    }

        
    
}
