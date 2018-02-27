/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import com.google.common.eventbus.Subscribe;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.Utils.CopyUtils;
import edu.hope.superresolution.Utils.IJMMReportingUtils;
import edu.hope.superresolution.exceptions.NoFiducialException;
import edu.hope.superresolution.exceptions.NoTrackException;
import edu.hope.superresolution.genericstructures.FiducialTravelDiff2D;
import edu.hope.superresolution.genericstructures.TravelMatchCase;
import edu.hope.superresolution.imagetrack.FiducialMoveFinder;
import edu.hope.superresolution.processors.FiducialAreaProcessor;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import org.micromanager.utils.ReportingUtils;

/**
 *
 * @author Microscope
 */
public class FiducialLocationModel extends ModelUpdateDispatcher implements FiducialCollection {
    
    //Event Constants
    public static final int EVENT_ELEMENT_ADDED = 0;
    public static final int EVENT_ELEMENT_REMOVED = 1;
    public static final int EVENT_ELEMENT_SET = 2;
    public static final int EVENT_ELEMENT_SELECTED = 3;
    public static final int EVENT_ELEMENT_MOD_FAILED = 4;
    public static final int EVENT_STORE_ROI_REQUEST = 5;
    public static final int EVENT_FIDUCIAL_AREA_DATA_CHANGED = 6;
    public static final int EVENT_FIDUCIAL_SELECTION_CHANGED = 7;
    public static final int EVENT_FIDCUIAL_REGION_CHANGED = 8;
    public static final int EVENT_SHOW_ALL = 9;
    public static final int EVENT_SHOW_CURRENT = 10;
    public static final int EVENT_FIDUCIAL_BOX_DISPLAY_CHANGE = 11;
    
    //Display Constants
    //Command Strings for Display Box Modes
    public static final String TRACK_BOX_DISPLAY = "TrackBox";
    public static final String SELECT_BOX_DISPLAY = "SelectBox";
    
    //Display Box Types
    public enum BoxDisplayTypes {
        track, select;        
    }
    private BoxDisplayTypes boxDisplayMode_ = BoxDisplayTypes.select;    
    
    //Index for number of Index (called in Track Constructor)
    private final int trackNumber_;
    private int minNumFiducialsForTrack_ = 3; //Used When Pushing next Location Model From a Track
    private double avgRelXPixelTranslate_ = 0;
    private double avgRelYPixelTranslate_ = 0;
    private double avgAbsoluteXPixelTranslation_ = 0;
    private double avgAbsoluteYPixelTranslation_ = 0;
    
    private final List<FiducialArea> fiducialAreaList_ = Collections.synchronizedList( new ArrayList< FiducialArea >() );  //List of Selected Areas
    private final FiducialAreaCallback fAreaCallBack_ = new FiducialAreaCallback();
    private FiducialArea selectedFiducialArea_ = null;
    private int selectedAreaIndex_ = -1;
    private Roi currentRoi_ = null;  //placeholder for when there are no List Selections but an ROI
    
    //Fiducial Area Processor Used for Detection of Fiducials
    private FiducialAreaProcessor fAreaProcessor_;
    private ImagePlus ip_;
    private FiducialMoveFinder fMoveFinder_ = null;
    
    private boolean showAll_ = false;  //Whether or not to show all changes or only current selected Fiducial Area    
    private final String acquisitionTitle_;
    
    //Loose Implementation of Listeners with update Method that will be called
    //These Views require a reference to the model, to do their own updating
    public FiducialLocationModel( ImagePlus ip, FiducialAreaProcessor fAreaProcessor, 
                                    String acquisitionTitle ) {
        
        trackNumber_ = 0;
        acquisitionTitle_ = acquisitionTitle;
        
        //This would be better off with a deep copy to isolate it for processing
        ip_ = ip;  
        try {
            fAreaProcessor_ = CopyUtils.abstractCopy( fAreaProcessor );
        } catch ( Exception ex ) {
            IJMMReportingUtils.logError(ex, "Failure to Copy Processor in New FiducialLocationModel");
            throw new RuntimeException( ex );  //Allow Micromanager to respond Appropriately
        }
        
    }
    
    public FiducialLocationModel( final ImageWindow imgWin, FiducialAreaProcessor fAreaProcessor,
                                    String acquisitionTitle ) {

        trackNumber_ = 0;
        acquisitionTitle_ = acquisitionTitle;
        
        ip_ = imgWin.getImagePlus();
        
        try {
            fAreaProcessor_ = CopyUtils.abstractCopy( fAreaProcessor );
        } catch ( Exception ex ) {
            IJMMReportingUtils.logError(ex, "Failure to Copy Processor in New FiducialLocationModel");
            throw new RuntimeException( ex );  //Allow Micromanager to respond Appropriately
        }
    }
    
    //Copy Constructor, with new ImagePlus

    /**
     *  Track Copy Constructor 
     *   (Tracks Fiducials From Previous FiducialLocatinModel on new ImagePlus)
     * 
     * @param ip - New ImagePlus
     * @param fLocationModel - Location Model to copy and track from on new ImagePlus
     * @throws NoFiducialException - Thrown if there are no viable options For Fiducials In the Track
     */
    public FiducialLocationModel( ImagePlus ip, FiducialLocationModel fLocationModel, 
                                        String acquisitionTitle ) throws NoFiducialException {
        
        acquisitionTitle_ = acquisitionTitle;
        
        ip_ = ip;
        minNumFiducialsForTrack_ = fLocationModel.minNumFiducialsForTrack_;
        //Copy subClass of fiducialAreaProcessor for thread-safety
        try {
            fAreaProcessor_ = CopyUtils.abstractCopy( fLocationModel.fAreaProcessor_ );
        } catch ( Exception ex ) {
            IJMMReportingUtils.logError(ex, "Failure to Copy Processor in New FiducialLocationModel");
            throw new RuntimeException( ex );  //Allow Micromanager to respond Appropriately
        }
        //Create a Movement Finder Based off of previous FiducialAreaList
        int maxMissingFrames = 15;
        fMoveFinder_ = new FiducialMoveFinder( fLocationModel.fiducialAreaList_, 
                                                ip_, maxMissingFrames, minNumFiducialsForTrack_,
                                                acquisitionTitle_ );
        TravelMatchCase mCase = fMoveFinder_.CorrelateDifferences();
        if( mCase == null ) {
            throw new NoFiducialException();
        }
        
        ApplySelectedMatchCase( mCase );
        //Since avg translation Values Calculated by Selecteed MatchCase, use source model for absolute
        avgAbsoluteXPixelTranslation_ = fLocationModel.avgAbsoluteXPixelTranslation_ + avgRelXPixelTranslate_;
        avgAbsoluteYPixelTranslation_ = fLocationModel.avgAbsoluteYPixelTranslation_ + avgRelYPixelTranslate_;
        
        //For consistency, copy currentSelected too
        selectedAreaIndex_ = fLocationModel.selectedAreaIndex_;
        selectedFiducialArea_ = fiducialAreaList_.get( selectedAreaIndex_ );
        trackNumber_ = fLocationModel.trackNumber_ + 1;

    }
    
    /**
     * Static method for creating a FiducialLocation Model that tracks Fiducials from another Fiducial Location Model.
     * 
     * @param ip
     * @param fLocationModel
     * @param acquisitionTitle
     * @return 
     */
    public static FiducialLocationModel createTrackedFiducialLocationModel( ImagePlus ip, FiducialLocationModel fLocationModel, 
                                        String acquisitionTitle ) throws NoFiducialException, NoTrackException {
        return new FiducialLocationModel( ip, fLocationModel, acquisitionTitle );
    }
    
    public static FiducialLocationModel createTrackedFiducialLocationModel( ImageProcessor iProc, FiducialLocationModel fLocationModel,
                                            String acquisitionTitle ) throws NoFiducialException, NoTrackException {
        return new FiducialLocationModel( iProc, fLocationModel, acquisitionTitle );
        /*if( !fModel.isTracked ) {
            throw new NoTrackException( "Failed to Track to previous FiducialModel" );
        }*/
    }
    
    /**
     * Track Copy Constructor 
     *   (Tracks Fiducials From Previous FiducialLocationModel while producing a
     *      new ImagePlus from an ImageProcessor)
     * 
     * @param iProc - The ImageProcessor To Build the ImagePlus (imagePlus used for Fitting)
     * @param fLocationModel - Location Model to copy and track from on new ImagePlus
     * @throws NoFiducialException - Thrown if there are no viable options For Fiducials In the Track
     */
    public FiducialLocationModel( ImageProcessor iProc, FiducialLocationModel fLocationModel,
                                            String acquisitionTitle ) throws NoFiducialException {
        this( new ImagePlus("Track Processed Num:" + (fLocationModel.trackNumber_ + 1), iProc), fLocationModel, acquisitionTitle );
    }
    

    /**
     * Apply a Given Match Case (Returned from fMoveFinder_) to FiducialAreaList_
     *     Adds Listeners and calculates Average Translation of the Match Case.
     * 
     * @param mCase - The Match case for all Fiducial Areas With Best matches above
     *                the minimum requirements
     */
    private void ApplySelectedMatchCase( TravelMatchCase mCase) {
            
        List< FiducialArea > tempList = new ArrayList< FiducialArea >();
        
        double avgXTranslate = 0;
        double avgYTranslate = 0;
        FiducialArea fArea;
        //Apply Selected Spots to the FiducialAreas and Fiducial Areas to the List
        for( FiducialTravelDiff2D diff : mCase.getFiducialTranslationSpots() ) {
            fArea = diff.areaOwnerRef_;
            avgXTranslate += diff.xDiffs_;
            avgYTranslate += diff.yDiffs_;
            //Add Change Observer so that the area will register
            fArea.RegisterToStateEventBus(fAreaCallBack_);
            try {
                fArea.setSelectedSpotRaw( diff.spotRef_ );
            }
            catch ( Exception ex ) {
                throw new RuntimeException( ex );
            }
            tempList.add( fArea );
            
        }
        
        avgRelXPixelTranslate_ = avgXTranslate / (mCase.getFiducialTranslationSpots().size() * fAreaProcessor_.getPixelSize());
        avgRelYPixelTranslate_ = avgYTranslate / (mCase.getFiducialTranslationSpots().size() * fAreaProcessor_.getPixelSize());
        
        //Calculate Standard Deviation of those translations (unbiased)
        double avgRelXPixelTranslateStdDev_ = 0, avgRelYPixelTranslateStdDev_ = 0;
        int numFiducials = 0;
        //Apply Selected Spots to the FiducialAreas and Fiducial Areas to the List
        for( FiducialTravelDiff2D diff : mCase.getFiducialTranslationSpots() ) {
            fArea = diff.areaOwnerRef_;
            //We Can Really only Calculate Standard Deviation from the set of real to real reliably
            if( !diff.toVirtual_ || !diff.fromVirtual_ ) {
                avgRelXPixelTranslateStdDev_ += Math.pow((diff.xDiffs_ - avgXTranslate), 2);
                avgRelYPixelTranslateStdDev_ += Math.pow((diff.yDiffs_ - avgYTranslate), 2);
                //numFiducials
            }
            
            
        }
        
        
        //Update the List in case of references
        fiducialAreaList_.clear();
        fiducialAreaList_.addAll(tempList);
        
    }
    
    
    @Override
    public final FiducialAreaProcessor getFiducialAreaProcessor( ) {
        return fAreaProcessor_;
    }
    
    @Override
    public void setFiducialAreaProcessor( FiducialAreaProcessor fAreaProcessor ) {
        //Currently Stored By Reference, 
        //However this would be nice to have accessible to various other threads
        // 1 Per Location Model
        fAreaProcessor_ = fAreaProcessor;
    }
    
    /**
     * Adds a FiducialArea to the List of Fiducial Areas.
     * 
     * @param roi 
     */
    public void addFiducialArea( Roi roi ) {
        
        fiducialAreaList_.add( createFiducialArea( roi ) );
        dispatch( EVENT_ELEMENT_ADDED );        
    }
    
    public void addFiducialArea( ) {
        addFiducialArea( null );
    }
    
    public void removeFiducialArea( FiducialArea fArea ) {
        fiducialAreaList_.remove( fArea );
        resetSelectedFiducialArea();
        dispatch( EVENT_ELEMENT_REMOVED );
    }
    
    public void removeFiducialArea( int fIdx ) {
        fiducialAreaList_.remove(fIdx);
        resetSelectedFiducialArea();
        dispatch( EVENT_ELEMENT_REMOVED );
    }
    
    //Sets a new Fiducial Area to the Given Index
    // Returns the old Fiducial Area
    public FiducialArea setFiducialAreaRegion( Roi roi, int fIdx ) {

        FiducialArea oldFArea = fiducialAreaList_.get(fIdx);
        
        fiducialAreaList_.set(fIdx, createFiducialArea( roi ) );

        dispatch( EVENT_ELEMENT_SET );
        return oldFArea;
    }
    
    
    private FiducialArea createFiducialArea( Roi roi ) {
        FiducialArea newFArea = new FiducialArea( ip_, roi, fAreaProcessor_ );
        newFArea.RegisterToStateEventBus(fAreaCallBack_);
        return newFArea;
    }
    
    //Accessor Function for setting the Roi of the selectedFiducialArea_
    public void setCurrentFiducialAreaRegion( Roi roi ) {
        //Make sure the ROI is a region
        //Legacy, keep null roi
        if( roi != null && !roi.isArea() ) {
            return;
        }
        
        if( selectedFiducialArea_ != null )
        {
            selectedFiducialArea_.setSearchArea( roi );
        }
        else
        {
            //Otherwise just save for next selection
            currentRoi_ = roi;
        }
    }
    
    public List<FiducialArea> getFiducialAreaList( ) {
        return fiducialAreaList_;
    }
    
    public FiducialArea getSelectedFiducialArea() {
        return selectedFiducialArea_;
    }
    
    public int getSelectedFiducialAreaIndex() {
        return selectedAreaIndex_;
    }
    
    public void setSelectedFiducialArea( int idx ) {
        storeCurrentFiducialArea();
        selectedFiducialArea_ = fiducialAreaList_.get(idx);
        selectedAreaIndex_ = idx;
        //Update the fiducialArea if there was a currentRoi_ and the current is null
        if( selectedFiducialArea_.getSelectionArea() == null ) {
            selectedFiducialArea_.setSearchArea( currentRoi_ );
        }
        //currentRoi_ should not be persistent
        currentRoi_ = null;
        dispatch( EVENT_ELEMENT_SELECTED );
    }
    
    public void setSelectedFiducialArea( FiducialArea fArea ) throws Exception {
        storeCurrentFiducialArea();
        if( fiducialAreaList_.contains( fArea ) ) {
            throw new Exception( "FiducialArea Was Never Registered to Model");
        }
        selectedFiducialArea_ = fArea;
        selectedAreaIndex_ = fiducialAreaList_.indexOf( fArea );
        //Update the fiducialArea if there was a currentRoi_ and the current is null
        if( selectedFiducialArea_.getSelectionArea() == null ) {
            selectedFiducialArea_.setSearchArea( currentRoi_ );
        }
        //currentRoi_ should not be persistent
        currentRoi_ = null;
        dispatch( EVENT_ELEMENT_SELECTED );
    }
    
    public void updateFiducialProcessorSettings( ExtendedGaussianInfo newSettings, 
                                                  MicroscopeModel microscopeModel ) {
        fAreaProcessor_.updateCurrentSettings( newSettings );
        fAreaProcessor_.updateMicroscopeModel( microscopeModel );
    }
    
    //Gets the Current Fiducial Area and stores it
    //Meant to be used in selection Changes
    //Will Delete the previous FiducialArea if null ( this may be the case of GUI problems)
    private void storeCurrentFiducialArea() {
        
        //To Make sure there was not an external Plug-in Manipulating ROIS
        //This is not okay in MVC world because The view should not have this burden
        dispatch( EVENT_STORE_ROI_REQUEST );
        
        //Areas Should have been set via view/Controllers, null value means delete
        if( selectedFiducialArea_ != null && selectedFiducialArea_.getSelectionArea() == null )
        {
            removeFiducialArea( selectedFiducialArea_ );
        }
        
    }
    
    public void enableShowAll( boolean enable ) {
        showAll_ = enable;
        dispatch( enable ? EVENT_SHOW_ALL : EVENT_SHOW_CURRENT );
    }
    
    //Sets the Current Fiducial Area and Index back to null and -1
    //This is necessary for removals and such that are just cleaning up without selection
    private void resetSelectedFiducialArea() {
        selectedFiducialArea_ = null;
        selectedAreaIndex_ = -1;
    }
    
    /**
     *   Sets the Display Box Mode for any image Viewers that are interested in display Fiducial Areas
     * <p>
     * <pre>
        Current Supported Modes:
           Track Mode - Shows the Anticipated Tracking Area box for the selected Fiducial
           Select Mode - Shows the Selection Area that the User first Used
      </pre>
     * @param cmd The String Correlating to a given display mode
     * @see #TRACK_BOX_DISPLAY
     * @see #SELECT_BOX_DISPLAY
     *
     */
    public void setDisplayBoxMode( String cmd ) {
        if( cmd.equals( TRACK_BOX_DISPLAY ) ) {
            boxDisplayMode_ = BoxDisplayTypes.track;
        } else if( cmd.equals( SELECT_BOX_DISPLAY ) ) {
            boxDisplayMode_ = BoxDisplayTypes.select;
        }
        else {
            //Bad Command 
            return;
        }
        
        dispatch( EVENT_FIDUCIAL_BOX_DISPLAY_CHANGE );
        
    }
    
    public BoxDisplayTypes getDisplayBoxMode( ) {
        return boxDisplayMode_;
    }
    
    /**
     * Updates the ImagePlus and all Fiducial Areas in the case of a live imaging sequence.
     * This resets and rescans all Fiducial Areas.
     * <p>
     * TODO: Make thread Safe
     * 
     * @param ip - the new imagePlus
     * @return - <code>true</code> if the ImagePlus was different or not null <code>false</code>
     *            if the ImagePlus was null or has not changed.
     */
    public boolean updateImagePlus( ImagePlus ip ) {
        if( ip == null ) {
            return false;
        }
        
        ip_ = ip;
        for( FiducialArea fArea : fiducialAreaList_ ) {
            fArea.updateImagePlus( ip_ );
        }
        
        return true;
    }
   
     /**
     * Updates the ImagePlus with a current ImageProcessor and reevaluates all Fiducial Areas.
     * This resets and rescans all Fiducial Areas.
     * <p>
     * TODO: Ensure thread Safety
     * 
     * @param ip - the new imagePlus
     * @return - <code>true</code> if the ImagePlus was different or not null <code>false</code>
     *            if the ImagePlus was null or has not changed.
     */
    public boolean updateImageProcessor( ImageProcessor iproc ) {
        if( iproc == null) return false;
        
        ip_.setProcessor(iproc);
        for( FiducialArea fArea : fiducialAreaList_ ) {
            fArea.updateImagePlus( ip_ );
        }
        
        return true;
    }
    
    /**
     * Returns the ImageProcessor on which all find operations are performed.
     * 
     * This May or may not be a reference to the internal processor given the nature of ImagePlus.
     * <p>
     * TODO: Make thread Safe
     * @return 
     */
    public ImageProcessor getImageProcessor() {
        return ip_.getProcessor();
    }
    
    /**
     *  Sets the minimum number of Fiducials that have to be found when trying
     *  to track a FiducialLocationModel for it not to throw a NoFiducialException.  
     *  This is used when using the track modified Copy Constructor of FiducialLocationModel
     *  with a populated fLocationAcquisitions_ list.
     * 
     * @param minNum - The minimum number of Fiducials to be found.  Should be greater
     *                 than 0 and less than the number of fiducial areas.
     * 
     * @see #FiducialLocationModel(ij.ImagePlus, edu.hope.superresolution.models.FiducialLocationModel) 
     */
    public void setMinNumFiducialTracks( int minNum ) {
        minNumFiducialsForTrack_ = minNum;
    }
    
    /**
     * Gets the current Registered minimum number of Fiducials that have to be found when trying
     *  to track a FiducialLocationModel for it not to throw a NoFiducialException.  This is used
     *  when using the track modified Copy Constructor of FiducialLocationModel with 
     *  a populated fLocationAcquisitions_ list.
     * 
     * @return Returns the minimum number of Fiducials that must be located for a track to be good
     * 
     * @see #FiducialLocationModel(ij.ImagePlus, edu.hope.superresolution.models.FiducialLocationModel) 
     */
    public int getMinNumFiducialTracks( ) {
        return minNumFiducialsForTrack_;
    }
    
    /**
     * Sets All the Current Fiducials to refSigmaNaut_ so that they are now the relative measure
     * for z score in changes in the psf.
     */
    public void setFocusPlaneToCurrentFiducials() {
        
        double avgDiffs = 0;
        int numReal = 0;
        //Store the Real Spots Widths First
        for( int i = 0; i< fiducialAreaList_.size(); ++i ) {
            FiducialArea fArea = fiducialAreaList_.get(i);
            BoundedSpotData spot = fArea.getRawSelectedSpot();
            if( spot!= null && !spot.isVirtual() ) {
                fArea.setRefSigmaNaut( spot.getWidth()/4 );
                avgDiffs += spot.getWidth()/4 - fArea.getRefSigmaNaut();
                numReal++;
            }
        }
        
        avgDiffs /= numReal;
        
        //For Virtuals, reiterate with the average change applied to their current refSigmaNauts
        for( int i = 0; i< fiducialAreaList_.size(); ++i ) {
            FiducialArea fArea = fiducialAreaList_.get(i);
            BoundedSpotData spot = fArea.getRawSelectedSpot();
            if( spot!= null && spot.isVirtual() ) {
                fArea.setRefSigmaNaut( fArea.getRefSigmaNaut() + avgDiffs );
            }
        }
        
        return;
    }
    
    /**
     *  Get the Average X Translation from the last Fiducial Model to this one
     * @return 
     */
    public double getAvgRelXPixelTranslation() {
        return avgRelXPixelTranslate_;
    }

    /**
     *  Get the Average X Translation from the last Fiducial Model to this one 
     * @return 
     */
    public double getAvgRelYPixelTranslation() {
        return avgRelYPixelTranslate_;
    }
    
    /**
     *  Gets the Absolute X Translation Relative to the First Fiducial Location Model
     *   used to start Track Copy Constructors.
     *   This Value will only be non-zero when a FiducialLocationModel is the result 
     *   of a Track Copy Constructor.
     * 
     * @return the Value in Image coordinates (Pixels) of X Translation
     * @see #FiducialLocationModel(ij.process.ImageProcessor, edu.hope.superresolution.models.FiducialLocationModel) 
     */
    public double getAvgAbsoluteXPixelTranslation() {
        return avgAbsoluteXPixelTranslation_;
    }
    
        /**
     *  Gets the Absolute Y Translation Relative to the First Fiducial Location Model
     *   used to start Track Copy Constructors.
     *   This Value will only be non-zero when a FiducialLocationModel is the result 
     *   of a Track Copy Constructor.
     * 
     * @return the Value in Image coordinates (Pixels) of Y Translation
     * @see #FiducialLocationModel(ij.process.ImageProcessor, edu.hope.superresolution.models.FiducialLocationModel) 
     */
    public double getAvgAbsoluteYPixelTranslation() {
        return avgAbsoluteYPixelTranslation_;
    }
    
    /**
     * FiducialArea State Listener Callback Class
     * <p>
     * Implemented as inner class to avoid constructor initialization issues that may arise with registration
     */
    private class FiducialAreaCallback {

        /**
         * Callback for handling when a selectedFiducialArea Has Changed
         * <p>
         * TODO: Change internals to dispatch StateBroadcaster instead of
         * current Observer Paradigm TODO: See if Async Callback is more
         * appropriate
         *
         * @param evt
         */
        @Subscribe
        public void onSelectedFiducialChanged(FiducialArea.SelectedFiducialChangeEvent evt) {
            //Should be reworked, but currently just call update for listeners
            dispatch(EVENT_FIDUCIAL_AREA_DATA_CHANGED);
        }

        /**
         * Callback for handling when a FiducialArea's Search Reagion Has
         * Changed
         * <p>
         * TODO: Change internals to dispatch StateBroadcaster instead of
         * current Observer Paradigm TODO: See if Async Callback is more
         * appropriate
         *
         * @param evt
         */
        @Subscribe
        public void onFAreaSearchRegionChanged(FiducialArea.RoiChangeEvent evt) {
            //Should be reworked, but currently just call update for listeners
            dispatch(EVENT_FIDCUIAL_REGION_CHANGED);
        }

        /**
         * Callback for handling when a FiducialArea's Possible Fiducials List
         * Has Changed
         * <p>
         * TODO: Change internals to dispatch StateBroadcaster instead of
         * current Observer Paradigm TODO: See if Async Callback is more
         * appropriate
         *
         * @param evt
         */
        @Subscribe
        public void onSelectedFiducialChanged(FiducialArea.SpotSearchRepopulatedEvent evt) {
            //Should be reworked, but currently just call update for listeners
            dispatch(EVENT_FIDUCIAL_SELECTION_CHANGED);
        }
    }
}
