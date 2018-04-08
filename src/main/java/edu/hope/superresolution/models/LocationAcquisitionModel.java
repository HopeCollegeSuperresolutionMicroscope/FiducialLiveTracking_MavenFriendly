/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import edu.hope.superresolution.ImageJmodifieds.TwoSliceSelector;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.Utils.IJMMReportingUtils;
import edu.hope.superresolution.autofocus.FiducialAutoFocus;
import edu.hope.superresolution.exceptions.NoFiducialException;
import edu.hope.superresolution.genericstructures.VirtualDirectoryManager;
import edu.hope.superresolution.livetrack.LiveTracking;
import edu.hope.superresolution.views.FiducialSelectForm;
import edu.hope.superresolution.views.ImageViewController;
import edu.hope.superresolution.views.MicroscopeModelForm;
import edu.hope.superresolution.views.ModifiedLocalizationParamForm;
import edu.valelab.gaussianfit.utils.ReportingUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.process.ImageProcessor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.AutofocusManager;
import org.micromanager.utils.MMException;

/**
 *
 *  Overarching Model that pertains to a given Image set
 *   This image set is defined by its image window and 
 *     is characterized by multiple FiducialLcoationModels ( 1 to each image)
 * 
 *  A new LocationAcquisitionModel correlates to a new query
 *     To Support multiple stored acquisition models in the future for operation
 * 
 * As the Top level Model, this creates and links views to selectedAcquisitionLocations_
 *   Additionally, this model itself may be linked to view that are created in the calling context (plugin)
 * 
 * @author Microscope
 */
public class LocationAcquisitionModel {
    
    /**
     * Local Implementation of a ModelUpdateListener for use with the GaussianParamModel
     * This Allows for Changes to the GaussianParamModel to be updated into the 
     * inner acquisition stack_
     * 
     * @see LocationAcquisitionModel#refreshGaussianFitParamSettings() 
     */
    public class GaussianFitParameterListener implements ModelUpdateListener {

        @Override
        public void update(ModelUpdateDispatcher caller, int event) {
            if (caller instanceof GaussianFitParamModel) {
                switch (event) {
                    case GaussianFitParamModel.SETTINGS_UPDATE:
                        refreshGaussianFitParamSettings();
                        break;
                }
            }
        }

        //Do nothing here
        @Override
        public void guiEnable(boolean enable) {
            return;
        }

        //Callback for Unregistration to a model
        @Override
        public void onRegisteredToModel(ModelUpdateDispatcher registeredModel) {
            //this was built to just assume the parameter model, change later
        }

        //Callback for Registration of object to model
        @Override
        public void onUnregisteredToModel(ModelUpdateDispatcher unregisteredModel) {
            //this was built to just assume the parameter model, change later
        }
        
    }
  
    
    /**
     * Enumerated Class for Fit Settings Modes (to be expanded later possibly)
     * 
     * @see #gaussianParameterListener_
     */
    public enum FIT_SETTINGS_REFRESH_MODES {
        current( 1 ),
        all(2);
        
        private final int mode_;
        
        FIT_SETTINGS_REFRESH_MODES( int mode ) {
            this.mode_ = mode;
        }        
    }
    
    private int numTracked_ = 0; //This tracks the number of LocationAcquisitions that were tracked irregardless of whether they were saved
    private ArrayList< FiducialLocationModel > fLocationAcquisitions_ = 
                                                new ArrayList< FiducialLocationModel >();  //FiductionLocationModels as set 1 per imagePlus
    private FiducialLocationModel selectedLocationsAcquisition_ = null;
    private final ImageWindow imgWin_;
    private AcquisitionSubmitAction submitAction_;
    private ScriptInterface app_;
    private final LiveTracking pluginInstance_;  //Used For Calling in GUIs
    
    private boolean areaUISelection_ = true;  //Whether or not new LocationModels Will have the User Select Fiducials
    private FIT_SETTINGS_REFRESH_MODES settingRefreshMode_ = FIT_SETTINGS_REFRESH_MODES.current;
    
    //View Elements
    private ImageViewController imgView_;
    private FiducialSelectForm fidSelectForm_;
    private ModifiedLocalizationParamForm paramForm_;
    //private MicroscopeModelForm microModelForm_;
    //Single Reference to be passed to all FiducialLocationModel
    private final GaussianFitParamModel gaussParamModel_;  //Parameters are not expected to change references across Acquisition Models
    private final GaussianFitParameterListener gaussianParameterListener_ = new GaussianFitParameterListener();
    //Unique Identifiers Used for ImageWindows, etc, pertaining to this Acquisition and any saving operations
    private static VirtualDirectoryManager acqSaveDirectory_ = new VirtualDirectoryManager();  //TempSave Directory for any VirtualStacks to Be Created
    public static final String ACQUISITIONTITLEBASE = "FiducialLocationAcquisition";
    private static int acquisitionTitleIdx_ = 0; //Used for cataloging of indices
    private final String uniqueAcquisitionTitle_; //Used for uniqueAcquisitionTitle

    
    /**
     * Inner Helper Class for Producing Smaller Events when a Window Submit (i.e. Fiducial Form Track Button) is clicked
     */
    public interface AcquisitionSubmitAction {
        
        /**
         *  Called after an intended (submit-function) button is pressed in a view
         *  This is so that validation of models may be permitted without unintentional multiple action events
         */
        public void submitResponse();
        
    }
    
    /**
     * General Constructor: Implements a New Location Acquisition which consists
     *  of an imageWindow for selection of Fiducials on a generalArea (intended to be
     *  Live Acquisition Window). An instance of Location Acquisition is meant to be tied to 
     *  one LiveTracking Instance.  The LiveTracking instance is meant only to be 
     *  used in the dispose methods of gui elements that would call for the close of the program.
     *  Use of the pluginInstance should not be made in the constructor as its calling context is malformed.
     * 
     * @param imgWin - The image Window in which selection should occur
     * @param submitAction - The action for when Fiducial Tracking View Clicked
     * @param pluginInstance - The instance of the plugin that created this Acquisition
     */
    public LocationAcquisitionModel( ImageWindow imgWin, /*ScriptInterface app,*/AcquisitionSubmitAction submitAction, LiveTracking pluginInstance ) {
        
        pluginInstance_ = pluginInstance;
        imgWin_ = imgWin;

        //app_ = app;
        submitAction_ = submitAction;
        //Set Gaussian Parameters graphical Components
        //This is a constrained structure currently given constructor updates
        gaussParamModel_ = new GaussianFitParamModel();
        //microModelForm_ = new MicroscopeModelForm();
        paramForm_ = new ModifiedLocalizationParamForm( /*gaussParamModel_*/ );
        gaussParamModel_.registerModelListener( paramForm_ );
        gaussParamModel_.registerModelListener(  gaussianParameterListener_ );
        
        //Create An Action Listener for Clicking on the track Button
        //Should be variable based on a new parameter for the sake of other acquisition types
        /*AcquisitionSubmitAction trackAction = new AcquisitionSubmitAction() {
            
            @Override
            public void submitResponse( ) {
                //Sets the autofocus to FiducialAutofocus
                AutofocusManager afMgr = app_.getAutofocusManager();
                afMgr.setAFPluginClassName(FiducialAutoFocus.class.getName());
                try {
                    afMgr.refresh();
                } catch (MMException ex) {
                    org.micromanager.utils.ReportingUtils.showError(ex);
                }
                //Notify User Of New Autofocus Options
                org.micromanager.utils.ReportingUtils.showMessage("New Autfocus Added: " + FiducialAutoFocus.DEVICE_NAME);
                try {
                    afMgr.selectDevice(FiducialAutoFocus.DEVICE_NAME);
                    //This is Stupidly Deprecated without a replacement for opening the Dialog
                    // runAcquisition does not work well
                } catch (MMException ex) {
                    org.micromanager.utils.ReportingUtils.showError(ex);
                }
                //Show Acquisition
                app_.getAcqDlg().setVisible(true);
            } 
        };*/
        
        //Set Location Model graphical components
        fidSelectForm_ = new FiducialSelectForm( /*selectedLocationsAcquisition_,*/ paramForm_, submitAction_, imgWin_, pluginInstance_ );
        
        imgView_ = new ImageViewController( imgWin_/*, selectedLocationsAcquisition_*/ );
        //gaussParamModel_.updateProcessorMicroscopeModel( microModelForm_.getMicroscopeModel() );
        
        try {
            //Create current Location Model and set to selectedLocationAcquisition
            pushNextFiducialLocationModel( imgWin_.getImagePlus().getProcessor(), true );
        } catch (NoFiducialException ex) {
            //Currently there should be no throw when creating the first FiducialLocatinModel
        }
 
        //turn on all GUIs for the model
        selectedLocationsAcquisition_.enableAllListenerGUIs(true);

        //Create A Unique AcquisitionTitle and Increment the acquisitionTitleIdx for uniqueness
        String temp = ACQUISITIONTITLEBASE + "_" + acquisitionTitleIdx_ + "_";
        uniqueAcquisitionTitle_ = temp;
        acquisitionTitleIdx_++;
        
    }

    
    /**
     * Copy Constructor - Shallow Copy of The Acquisition List and the final 
     * selectedLocationAcquisition.  Location Models are linked, but new models won't be registered.
     * @param source 
     */
    public LocationAcquisitionModel( LocationAcquisitionModel source ) {
        
        pluginInstance_ = source.pluginInstance_;
        //Should we keep this?
        imgWin_ = source.imgWin_;

        //app_ = source.app_;
        submitAction_ = source.submitAction_;
        //Set Gaussian Parameters graphical Components
        //This is a constrained structure currently given constructor updates
        gaussParamModel_ = source.gaussParamModel_;
        //microModelForm_ = new MicroscopeModelForm();
        paramForm_ = source.paramForm_;
        //This might need to be separate
        //gaussParamModel_.registerModelListener( paramForm_ );
        //gaussParamModel_.registerModelListener(  gaussianParameterListener_ );
        
        //Create An Action Listener for Clicking on the track Button
        //Should be variable based on a new parameter for the sake of other acquisition types
        AcquisitionSubmitAction trackAction = new AcquisitionSubmitAction() {
            
            @Override
            public void submitResponse( ) {
                //Sets the autofocus to FiducialAutofocus
                AutofocusManager afMgr = app_.getAutofocusManager();
                afMgr.setAFPluginClassName(FiducialAutoFocus.class.getName());
                try {
                    afMgr.refresh();
                } catch (MMException ex) {
                    org.micromanager.utils.ReportingUtils.showError(ex);
                }
                //Notify User Of New Autofocus Options
                IJMMReportingUtils.showMessage("New Autfocus Added: " + FiducialAutoFocus.DEVICE_NAME);
                try {
                    afMgr.selectDevice(FiducialAutoFocus.DEVICE_NAME);
                    //This is Stupidly Deprecated without a replacement for opening the Dialog
                    // runAcquisition does not work well
                } catch (MMException ex) {
                    org.micromanager.utils.ReportingUtils.showError(ex);
                }
                //Show Acquisition
                app_.getAcqDlg().setVisible(true);
            } 
        };
        
        //Set Location Model graphical components
        fidSelectForm_ = new FiducialSelectForm( /*selectedLocationsAcquisition_,*/ paramForm_, trackAction, imgWin_, pluginInstance_ );
        
        imgView_ = new ImageViewController( imgWin_/*, selectedLocationsAcquisition_*/ );
        
        //Copy the Current fLocationAcquisition
        Iterator< FiducialLocationModel > it = source.fLocationAcquisitions_.iterator();
        while( it.hasNext() ) {
            fLocationAcquisitions_.add(it.next());
        }
        selectedLocationsAcquisition_ = source.selectedLocationsAcquisition_;
 
        //turn on all GUIs for the model
        selectedLocationsAcquisition_.enableAllListenerGUIs(true);
        
        //Create A Unique AcquisitionTitle and Increment the acquisitionTitleIdx for uniqueness
        String temp = ACQUISITIONTITLEBASE + "_" +acquisitionTitleIdx_ + "_";
        uniqueAcquisitionTitle_ = temp;
        acquisitionTitleIdx_++;
        

    }
    
    //Deprecated: Was Developed Under Assumption that ImagePlus would be 1 slice processors.
    //To Be Deleted Once Usage is proven unnecessary.
    /**
    *  Pushes a new Fiducial Location Model Onto fLocationAcquistions_ (internal stack)
    * <p>
    *  In the event of an empty fLocationAcquisitions stack, a new FiducialLocationModel
    *   is created and added.
    * <p>
    *  In the event that fLocationAcquisitions_ is not empty, the next FiducialLocationModel
    *   is a copy of the last FiducialLocation Model and should track the previous
    *   fiducials as selected in their corresponding Fiducial Areas
    *
    *   @param ip          New Image Plus to track the Next Location Model To
    *   @param setSelected Whether or not, the new Location Model is Selected 
    *                      ( mainly for display )
    *   @return            The New FiducialLocationModel that was just appended
    *                      to fLocationAcquistions_
    *   @throws NoFiducialException If FiducialLocationModel is a track and 
    *                               Cannot Find any Fiducials, the model construction 
    *                               if aborted and not added but throws this Exception
    *   @see FiducialLocationModel
    */
    /*public FiducialLocationModel pushNextFiducialLocationModel( ImagePlus ip, boolean setSelected ) throws NoFiducialException {
        
        FiducialLocationModel locModel = null;
        if( /*areaUISelection_ ||*/ /*fLocationAcquisitions_.size() <= 0 ) {
            //Produce the GUIs and wait...
            //Currently No GUIS or Blocking
            locModel = new FiducialLocationModel( dummyPlus, gaussParamModel_.getCurrentProcessor(),
                                                        uniqueAcquisitionTitle_ );
        }       
        else {
            //Copy From Last FiducialLocationModel
            try {
                locModel = new FiducialLocationModel( dummyPlus, 
                    fLocationAcquisitions_.get( fLocationAcquisitions_.size() - 1 ),
                                                        uniqueAcquisitionTitle_ );
            } catch (NoFiducialException ex) {
                throw ex;
            } 
        }
        
        fLocationAcquisitions_.add( locModel );
        if( setSelected || selectedLocationsAcquisition_ == null ) {
            try {
                setSelectedLocationAcquistion( fLocationAcquisitions_.size() - 1 );
            } catch ( Exception ex ) {
                ex.printStackTrace();
                ReportingUtils.logError( ex, ex.getMessage() );
            }
        }
        
        return locModel;
    }*/
    
    /**
     *  Overloaded Version of pushNextFiducialLocationModel, only requiring an ImageProcessor
     *   from which the new LocationModel will create an ImagePlus for reference.
     * <p>
     *  See the reference to the other overloaded function for parameters
     * 
     * @see #pushNextFiducialLocationModel(ij.ImagePlus, boolean) 
     * 
     */
    public FiducialLocationModel pushNextFiducialLocationModel( ImageProcessor iProc, boolean setSelected ) throws NoFiducialException {
               FiducialLocationModel locModel = null;
        if( /*areaUISelection_ ||*/ fLocationAcquisitions_.size() <= 0 ) {
            //Produce the GUIs and wait...
            //Currently No GUIS or Blocking
            locModel = new FiducialLocationModel( new ImagePlus("", iProc), 
                                                    gaussParamModel_.getCurrentProcessor(),
                                                    uniqueAcquisitionTitle_ );
        }       
        else {
            //Copy From Last FiducialLocationModel
            try {
                locModel = FiducialLocationModel.createTrackedFiducialLocationModel(iProc, 
                        fLocationAcquisitions_.get( fLocationAcquisitions_.size() - 1 ), uniqueAcquisitionTitle_, numTracked_);
                        /*new FiducialLocationModel( iProc, 
                    fLocationAcquisitions_.get( fLocationAcquisitions_.size() - 1 ),
                                                        uniqueAcquisitionTitle_ );*/
            } catch (NoFiducialException ex) {
                throw ex;
            } 
        }
        
        numTracked_++;
        
        fLocationAcquisitions_.add( locModel );
        if( setSelected || selectedLocationsAcquisition_ == null ) {
            try {
                setSelectedLocationAcquistion( fLocationAcquisitions_.size() - 1 );
            } catch ( Exception ex ) {
                ex.printStackTrace();
                IJMMReportingUtils.logError( ex, ex.getMessage() );
            }
        }
        
        return locModel;
    }
    
    
    /**
     *  Get the FiducialLocationModel in fLocationAcquisitions_ at the given Index
     * 
     * @param idx The index to get from in fLocationAcquisitions_ (inner Acquisition stack) 
     * @return    The FiducialLocationModel at the given (zero-based) idx
     */
    public FiducialLocationModel getFiducialLocationModel( int idx ) {
        return fLocationAcquisitions_.get( idx );
    }
    
    /**
     *  Set The Current Selected Location Acquisition from index.
     * <p>
     * This ties GUI interfaces to the current Object for Display
     * 
     * @param idx The index to set from in fLocationAcquisitions_ (inner Acquisition stack)
     * @throws Exception Thrown if idx exceeds the size of the given stack
     */
    public void setSelectedLocationAcquistion( int idx ) throws Exception {
        if( idx >= fLocationAcquisitions_.size() ) {
            throw new Exception( "Fiducial Location Model index out of Bounds");
        }
        
        unvalidatedSetSelectedLocationAcquisition( fLocationAcquisitions_.get( idx ) );
    }
    
    /**
     *  Set The Current Selected Location Acquisition from a model object that is
     *  already stored in fLocationAcquisitions_ (acquisition inner stack)
     * <p>
     * This ties GUI interfaces to the current Object for Display
     * 
     * @param fLocationModel FiducialLocationModel from fLocationAcquisitions_ 
     *                       to be set as selected
     * @throws Exception     Exception Thrown if FLocationModel is not a part of
     *                       fLocationAcquisitions_ (acquisition inner stack)
     */
    public void setSelectedLocationAcquisition( FiducialLocationModel fLocationModel ) throws Exception {
        if( fLocationModel == null || fLocationAcquisitions_.contains( fLocationModel ) ) {
            throw new Exception( "Fiducial Location Model is not found in set");
        }
        
        unvalidatedSetSelectedLocationAcquisition( fLocationModel );        
    }
    
    /**
     * Get the currently selected FiducialLocationModel (contained in fLocationAcquisitions_
     * (acquisition inner stack)
     * 
     * @return currently selected FiducialLocationModel
     * @see    #setSelectedLocationAcquisition(edu.hope.superresolution.models.FiducialLocationModel) 
     * @see    #setSelectedLocationAcquisition(edu.hope.superresolution.models.FiducialLocationModel) 
     */
    public FiducialLocationModel getSelectedLocationAcquisition( ) {
        return selectedLocationsAcquisition_;
    }
    
    /**
     *  Method where actual selection activities are performed that correspond to 
     *  setting the Selected Location Acquisition (FiducialLocationModel).  This method
     *  does not check to see if the FiducialLocationModel being set is part of 
     *  the acquisition inner stack and is private as such.
     * <p>
     *  Unregisters GUI elements from previous selection and registers to new selection
     * 
     * @param fLocationModel The corresponding acquisition from the inner acquisition 
     *                       stack to set as selected.
     * @see #setSelectedLocationAcquisition(edu.hope.superresolution.models.FiducialLocationModel) 
     * @see #setSelectedLocationAcquisition(edu.hope.superresolution.models.FiducialLocationModel)
     */
    private void unvalidatedSetSelectedLocationAcquisition( FiducialLocationModel fLocationModel ) {
        //unregister old model
        if( selectedLocationsAcquisition_ != null ) {
            //Unregisters the imageView from the old model
            selectedLocationsAcquisition_.unregisterModelListener( imgView_ );
        }
        
        selectedLocationsAcquisition_ = fLocationModel;
        fLocationModel.registerModelListener(imgView_);
        selectedLocationsAcquisition_.registerModelListener( fidSelectForm_ );
    }
    
    /**
     *  Enable All Graphical Elements that are registered ModelUpdateListeners to 
     *  the currently selected LocationAquisition through their guiEnable( boolean ) methods
     * 
     * @param enable <code>true</code> - on <code>false</code> - off
     * @see ModelUpdateListener
     * @see ModelUpdateListener#guiEnable(boolean) 
     */
    public void enableSelectedLocationModelGUIs( boolean enable ) {
        selectedLocationsAcquisition_.enableAllListenerGUIs(enable);
    }
    
    /**
     *  Triggers dispose() on all Graphical Elements that were created for use 
     *  with this and other contained classed (FiducialLocationModel)
     */
    public void dispose( ) {
        imgView_.dispose();
        fidSelectForm_.dispose();
        paramForm_.dispose();
    }
    
    //Enables Manual Fiducial Selection per new image made
    public void enableManualFiducialSelection( boolean enable ) {
        areaUISelection_ = enable;
    }
    
    /**
     * Acquires new GaussianFitProcessor Settings From GaussianFitParamModel that
     *  is registered to this LocationAcquisitionModel
     * <p>
     *  Settings are applied in 1 of 2 ways to LocationModels in the (acquisition 
     *  inner stack).  These are controlled by settingRefreshMode_ member which is an
     * enumeration of LocationAcquisitionModel#FIT_SETTINGS_REFRESH_MODES
     * <p>
     *   current - refreshes Current Location Model and any copies from it (ex: pushNextFiducialLocationModel )
     * <p>
     *   all - Refreshes all Location Model Data with the same settings
     * <p> Note: This method is called from registered GaussianFitParameterListener 
     *     to gaussParamModel_ member
     * 
     * @see #FIT_SETTINGS_REFRESH_MODES
     * @see #GaussianFitParameterListener
     * @see GaussianFitParamModel
     */
    private void refreshGaussianFitParamSettings() {
        
        ExtendedGaussianInfo newSettings = gaussParamModel_.getCurrentProcessorModelSettings();
        MicroscopeModel newMicroscope = gaussParamModel_.getCurrentMicroscopeModel();
        switch( settingRefreshMode_ ) {
            case current:
                    //get the Selected Location Acquisition and set
                    getSelectedLocationAcquisition().updateFiducialProcessorSettings( newSettings, newMicroscope );
                break;
            case all:
                for( FiducialLocationModel fLocationModel: fLocationAcquisitions_ ) {
                    fLocationModel.updateFiducialProcessorSettings( newSettings, newMicroscope );
                }
                break;
        }
    }
    
    /**
     *  Removes the last FiducialLocationModel that was appended to the list.
     *  This method is applied so that tracks may not be interrupted except at their
     *  edge.  Quietly returns if the list of FiducialLocationModels is only of size 1.
     * 
     */
    public void removeLastLocationAcquistion() {
        if( fLocationAcquisitions_.size() > 1 ) {
            if( selectedLocationsAcquisition_ == 
                    fLocationAcquisitions_.remove( fLocationAcquisitions_.size() - 1 ) ) {
                selectedLocationsAcquisition_ = 
                        fLocationAcquisitions_.get( fLocationAcquisitions_.size() - 1 );
            }
        }
    }
    
    /**
     * Returns the number of FiducialLocationModels that have been taken in this moment
     * 
     * @return 
     */
    public int getNumFiducialLocationModels( ) {
        return fLocationAcquisitions_.size();
    }
    
    /**
     * Returns A LocationAcquisitionModel encompassing the indices from start to end.
     * <p>
     * Note: If startIdx or endIdx is out of order, the minimum index will be chosen.
     * 
     * @return 
     */
    public LocationAcquisitionModel getCurrentLocationAcquisitionCopy( ) {
        return new LocationAcquisitionModel(this);
    }    
    
    /**
     * Returns a Representative ImageProcessor of the Acquisitions being polled.  For Live Windows, this will return
     * the most recent ImagePlus while for static contexts it will return the current ImagePlus.
     * <p>
     * Returned objects are references to copies potentially so only use for referencing attributes.
     * 
     * @return - An ImagePlus instance with the same general properties of the last ImageWindow Instance.
     */
    public ImageProcessor getRepresentativeImageProc() {
        return imgWin_.getImagePlus().getProcessor();
    }
    
    /**
     * Gets the name of this Acquisition (unique Among These Models)
     * @return 
     */
    public String getAcquisitionTitle() {
        return uniqueAcquisitionTitle_;
    }
    
    /**
     * Returns the Instance of VirtualDirectory Management that pertains to LocationAcquisition Models
     * And their actions.  This was made so that Virtual Stack Windows might be created to show
     * Operations in real time.
     *    
     */
    public static VirtualDirectoryManager getAcquisitionSaveDirectory() {
        return acqSaveDirectory_;
    }
    
}
