/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import com.google.common.eventbus.EventBus;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.Utils.IJMMReportingUtils;
import edu.hope.superresolution.genericstructures.CopySourceListReference;
import ij.gui.Roi;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import edu.hope.superresolution.genericstructures.ListCallback;
import edu.hope.superresolution.genericstructures.MasterSpecificEvent;
import edu.hope.superresolution.genericstructures.StateBroadcaster;
import edu.hope.superresolution.processors.FiducialAreaProcessor;
import edu.valelab.gaussianfit.data.SpotData;
import ij.ImagePlus;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import org.micromanager.utils.ReportingUtils;

/**
 *
 * Container of Information pertaining to a user-selected area in a image area, 
 * in which a a user selected fiducial is contained
 * 
 * @author Microscope
 */
public class FiducialArea extends StateBroadcaster {    
    
    
     /**
     * Event Class - The Roi that this FiducialArea is searching has changed
     * 
     */
    public static class RoiChangeEvent extends MasterSpecificEvent {
        
        private final Roi prevRoi_;
        private final Roi curRoi_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for Roi change
         * @param prevRoi - The previous Roi of the ImagePlus
         * @param curRoi - The current Roi of the ImagePlus
         */
        public RoiChangeEvent( FiducialArea originator, Roi prevRoi, Roi curRoi ) {
            super(originator);

            prevRoi_ = prevRoi;
            curRoi_ = curRoi;

        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevRoi - The previous Roi of the ImagePlus
         * @param curRoi - The current Roi of the ImagePlus
         */
        public RoiChangeEvent( RoiChangeEvent originatorSource, Roi prevRoi, Roi curRoi ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevRoi_ = prevRoi;
            curRoi_ = curRoi;
            
        }
        
        public Roi getPreviousRoi() {
            return prevRoi_;
        }
        
        public Roi getCurrentRoi() {
            return curRoi_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New RoiChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof RoiChangeEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            RoiChangeEvent upCast = (RoiChangeEvent) event;
            return new RoiChangeEvent( upCast, prevRoi_, upCast.getCurrentRoi() );
        }
        
    }
    
    /**
     * Event Class - LightWeight Class that only provides an indication that a spot
     * search was performed and a handle to the originating FiducialArea
     * 
     * 
     */
    public static class SpotSearchRepopulatedEvent extends MasterSpecificEvent {
        
        //SubClass Level accessor
        private final FiducialArea fArea_;
        
         /**
         * Constructor
         * 
         * @param originator - originating FiducialArea instance that was searched
         */
        public SpotSearchRepopulatedEvent( FiducialArea originator ) {
            super(originator);
            fArea_ = originator;

        }
        
        public FiducialArea getFiducialAreaSearched() {
            return fArea_;
        }

        /**
         * Since this is a non-ordered event, Concatenate simply returns the same event.
         * <p>
         * This still checks to make sure the same type of event was passed and by
         * the same originator for the sake of any generalized Concatenate Usages in other code
         * 
         * @param event - the More Recent Event compared to this
         * @return New RoiChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof SpotSearchRepopulatedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            return this;
        }
        
    }
    
    /**
     * Event Class - The Selected Fiducial from the set has changed
     * 
     */
    public static class SelectedFiducialChangeEvent extends MasterSpecificEvent {
        
        private final BoundedSpotData prevSelected_;
        private final BoundedSpotData curSelected_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for Roi change
         * @param prevRoi - The previous Roi of the ImagePlus
         * @param curRoi - The current Roi of the ImagePlus
         */
        public SelectedFiducialChangeEvent( FiducialArea originator, BoundedSpotData prevSelected, BoundedSpotData curSelected ) {
            super(originator);

            prevSelected_ = prevSelected;
            curSelected_ = curSelected;

        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevSelected - The previous Selected Spot
         * @param curSelected - The current Selected Spot
         */
        public SelectedFiducialChangeEvent( SelectedFiducialChangeEvent originatorSource, BoundedSpotData prevSelected, BoundedSpotData curSelected ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevSelected_ = prevSelected;
            curSelected_ = curSelected;
            
        }
        
        public BoundedSpotData getPreviousSelectedFiducialSpot() {
            return prevSelected_;
        }
        
        public BoundedSpotData getCurrentSelectedFiducialSpot() {
            return curSelected_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New RoiChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof SelectedFiducialChangeEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            SelectedFiducialChangeEvent upCast = (SelectedFiducialChangeEvent) event;
            return new SelectedFiducialChangeEvent( upCast, prevSelected_, upCast.getCurrentSelectedFiducialSpot());
        }
        
    }
    
    
    
    //To Be Changed Later
    public Color DefaultColor = Color.yellow;
    public Color SelectColor = Color.red;
    
    private final FiducialAreaProcessor fiducialAreaProcessor_;
    //private final BoundedSpotListAction spotListAction_;  //Reusable CallBack to Populate List
    
    private ImagePlus ip_;  //Store ImagePlus that belongs this FiducialArea
    private final ReentrantLock imLock_ = new ReentrantLock(); //Lock For Swapping the Image
    
    private Roi origSelectionArea_ = null;  //This correlates to the original Specified search Area
    private Roi trackSearchArea_ = null; //  This correlates to the searchArea ( typically movement Anticipating)
    //Booleans to indicate the state of the process
    private boolean isChanging_ = false;  //Whether or not FiducialArea is being updated
    private boolean hasChanged_ = false;  //Whether or not it has been updated
    private final Object changeLock_ = new Object();
    private final List< BoundedSpotData > sortedPossibleFiducials_ =
                        Collections.synchronizedList( new ArrayList< BoundedSpotData >() );  //This is the list of possible Fiducials
    private final MinWidthFirst minWidthComparator_ = new MinWidthFirst();
    private final MaxIntensityFirst maxIntensityComparator_ = new MaxIntensityFirst();
    private BoundedSpotData selectedSpot_ = null;  //This is the selected Spot chosen to correlate
    private double maxIntensity_ = 0;  //used for Image Normalization
    private final Object spotLock_ = new Object();
    
    private final int virtualFrameTrackNumber_;  //whether or not Fiducial Area is the result of a track on a virtual spot
    
    //Defocus Tracking members
    private double refSigmaNaut_ = 0;  //Gaussian SigmaNaut from referenced in focus plane (this is 0 unless being tracked)
    private double refSigmaNautUncertainty_ = 0; //Uncertainty in the SigmaNaut (stdDev in Average)
    private double avgSigma_;  //The AverageSigma of all found Spots
    private double avgSigmaUncertainty_;  //The AvgUncertainty of the Sigmas
    private double relativeDz_ = 0;  //dz Relative to a reference plane and sigmaNaut
    private double relativeDzUncertainty_ = 0;  //Uncertainty in the Relative dz
    
    //To Be Made a Variable
    private double frameTravelAnticipated_ = 100;  //In Pixels
    
    /**
    *   General Constructor (Unconcerned with Frame Track Number or reference plane sigmaNauts)
    */
    public FiducialArea( ImagePlus ip, Roi roi, FiducialAreaProcessor fAreaProcessor ) {
        
        //Depending on References, we may want to change the copy
        ip_ = ip;
        fiducialAreaProcessor_ = fAreaProcessor;
        fAreaProcessor.lockSettings();
        try {
            frameTravelAnticipated_ = fAreaProcessor.getCurrentSettings().getMaxTrackTravel();
        } finally {
            fAreaProcessor.unlockSettings();
        }
        
        virtualFrameTrackNumber_ = 0;
        
        setSearchArea(roi);   
    }
    
    /**
    *   Modified Copy Constructor - Implied Track
    *   @param ip - new ImagePlus meant to be evaluated by new Fiducial Area
    *   @param roi - new Roi for this Fiducial Area (typically shifted)
    *   @param baseFArea - Fiducial Area that is used as the previous model in a track
    *                        Used for fAreaProcessor and virtualFrameTrackNumber_
    */
    public FiducialArea( ImagePlus ip, Roi roi, FiducialArea baseFArea ) {
        
        //Depending on References, we may want to change the copy
        ip_ = ip;
        fiducialAreaProcessor_ = baseFArea.fiducialAreaProcessor_;
        
        //If the base spot was virtual, increment the virtual track number of this
        if( baseFArea.getRawSelectedSpot().isVirtual() ) {
            virtualFrameTrackNumber_ = baseFArea.virtualFrameTrackNumber_ + 1;
        } else {
            virtualFrameTrackNumber_ = 0;
        }
        
        //For Focus Data (if no refSigmaNaut, then use the selectedSpot as a reference)
        //Selected Spot Divided By 4 must be changed later
        refSigmaNaut_ = (baseFArea.refSigmaNaut_ != 0) 
                                 ? baseFArea.refSigmaNaut_ : baseFArea.selectedSpot_.getWidth()/4;
        refSigmaNautUncertainty_ = (baseFArea.refSigmaNautUncertainty_ != 0) 
                                        ? baseFArea.refSigmaNautUncertainty_ : baseFArea.selectedSpot_.getSigma();
        
        //Use This as a reference for spot selection
        selectedSpot_ = baseFArea.selectedSpot_;
        
                
        //Update the settings for this track to adjust for intensity decay
        //This is Not Ideal for Multi-threading (offers some concurrency opportunities for error if expanded on)
        ExtendedGaussianInfo revertSettings, settings;
        fiducialAreaProcessor_.lockSettings();
        try {
            settings = fiducialAreaProcessor_.getCurrentSettings();
            frameTravelAnticipated_ = settings.getMaxTrackTravel();
            revertSettings = new ExtendedGaussianInfo(settings);
            if ( settings.getIntensityThreshold() > selectedSpot_.getMaxIntensity() * .8 ) {
                settings.setIntensityThreshold( (int) (selectedSpot_.getMaxIntensity() * .8) );
                ij.IJ.log( " Intensity lowered ");
            }
        } finally {
            fiducialAreaProcessor_.unlockSettings();
        }
        
        setSearchArea(roi);
        //Reset the settings to the regular search Settings
        fiducialAreaProcessor_.updateCurrentSettings( revertSettings );

    }
    
    /**
     * Returns the number of times that this Fiducial Area has been tracking a virtual (undetected particle).
     * This results from use of the Implied Track Constructor, since there is a potential for a fiducial area
     * to have a missing fiducial but to track with th set of fiducials.  If the particle is rediscovered in 
     * this FiducialArea, the VirtualFrameNumber will be reset to 0.  
     * 
     * @return 
     * @see FiducialArea##FiducialArea(ij.ImagePlus, ij.gui.Roi, edu.hope.superresolution.models.FiducialArea) 
     */
    public int getVirtualFrameTrackNumber() {
        return virtualFrameTrackNumber_;
    }
    
    /**
     * Gets the fiducialAreaProcessor that was used to populate the potential list of
     * fiducial spots
     * 
     * @return 
     */
    public FiducialAreaProcessor getFiducialAreaProcessor () {
        return fiducialAreaProcessor_;
    }
    
    /**
     * Gets A Reference to the current ImagePlus
     *
     * @return Returns the ImagePlus being used in fitting operations
     */
    public ImagePlus getImagePlus() {
        imLock_.lock();
        try {
            return ip_;
        } finally {
            imLock_.unlock();
        }
            
    }
    
    /**
     * Updates the ImagePlus Used For Fitting and then recalls the Fiducial Area Selection Roi 
     * to fit the new imagePlus.
     * 
     * @param ip - the new ImagePlus to reprocess and store fiducials over
     */
    public void updateImagePlus( ImagePlus ip ) {
        
        imLock_.lock();
        try {
            ip_ = ip;
            setSearchArea( origSelectionArea_ );
            
        } finally {
            imLock_.unlock();
        }
        
    }
    
    /**
     * Get the area that was used to create the Fiducial Area.  This can be the User-
     * drawn selection or, if this was just tracked, the track search area centered on the 
     * previous selectedSpot
     * 
     * @return 
     */
    public Roi getSelectionArea() {
        return origSelectionArea_;
    }
    
    /**
     * Get the tracksearch Area.  This will be the anticipated travel area centered on the 
     * selected spot (fiducial particle).
     * 
     * @return 
     */
    public Roi getTrackSearchArea() {
        return trackSearchArea_;
    }
    
    //setTheSearchArea and corresponding tracked Fiducials
    //  Returns whether successful or not
    public final boolean setSearchArea( Roi roi ) {
        
        //Currently Operation is not finished
        if( getIsChanging() && !getHasChanged() )
        {
            return false;
        }
        
        setIsChanging(true);
        Roi prevRoi = null;
        if( origSelectionArea_ != null ) {
         prevRoi= (Roi) origSelectionArea_.clone();
        } 
        //Don't Analyze if there are no bounds or roi is null (some MM area erasure)
        //  setOrigSearchArea_ to null for less queries on Bounds
        if( roi == null || roi.getFloatWidth() <= 0 || roi.getFloatHeight() <= 0 ) {
            origSelectionArea_ = null;
            if( prevRoi != null ) {
                dispatchEvent( new RoiChangeEvent( this, prevRoi, origSelectionArea_ ));
            }
            setIsChanging(false);
            return false;
        }
        
        setHasChanged(false);
        
        origSelectionArea_ = roi;
        //Dispatch Roi Change Event
        dispatchEvent( new RoiChangeEvent( this, prevRoi, origSelectionArea_ ) );
        boolean runProcess;
        
        imLock_.lock();
        try {
          //Gaussian Fit To fiducials
          //BoundedSpotListAction may be called in Constructor, but will act after items are populated (this should be mitigated)
          runProcess = fiducialAreaProcessor_.fitRoiForImagePlus( ip_, roi, new BoundedSpotListAction(this)  );       
        } finally {
            imLock_.unlock();
        }
        
        setIsChanging( runProcess );

        if( !getIsChanging() ) {
            setHasChanged(true);
        }
        //BoundedSpotListAction is Called to Finish population    
        return true;
      
    }
    
    private void setHasChanged( boolean state ) {
        synchronized( changeLock_ ) {
            hasChanged_ = state;
        }       
    }
    
    private void setIsChanging( boolean state ) {
        synchronized( changeLock_ ) {
            isChanging_ = state;
        }
    }
    
    public boolean getIsChanging( ) {
        synchronized( changeLock_ ) {
            return isChanging_;
        }
    }
    
    //Public get function may find limited use in monitoring Fit threads
    public boolean getHasChanged( ) {
        synchronized( changeLock_ ) {
            return hasChanged_;
        }
    }
    
    //Takes a reference to a sychronized ArrayList of SpotData, fills and sorts sortedPossibleFiducials_
    //The selectedSpot_ is updated to the maximum spot and hasChanged_ is updated
    /**
     * Populates the List of sorted Possible Fiducials with the new list and selects a Fiducial
     * that is either in the same area as the previous list, or is the first fiducial in the list.
     * <p>
     * Will clear the previous list.
     * 
     * @param list - A List of SpotData or BoundedSpotData, that is is pushed into sortedPossibleFiducial List
     * @return - The number of spots that now are possible fiducials
     */
    private int populateBoundedSpotList( List<SpotData> list ) {
        
        SpotData spot;
        
        //Add remaining Spots to sortedPossible Fiducials
        sortedPossibleFiducials_.clear();
        for ( int i = 0; i < list.size(); ++i ) {
            spot = list.get(i);
            //In case the spot is explicit BoundedSpotData
            //Other SpotData Casts can be added here for variability
            if( spot instanceof BoundedSpotData) {
                sortedPossibleFiducials_.add(new BoundedSpotData( (BoundedSpotData) spot ) );
            }
            else {
                sortedPossibleFiducials_.add(new BoundedSpotData( spot, fiducialAreaProcessor_.getPixelSize()));
            }
        }

        Collections.sort(sortedPossibleFiducials_, maxIntensityComparator_);

        //Set Selected Spot = null if we have to because there were no values returned
        if ( sortedPossibleFiducials_.isEmpty() ) {
            if( selectedSpot_ != null ) {
                dispatchEvent( new SelectedFiducialChangeEvent(this, selectedSpot_, null ) );
                selectedSpot_ = null;
            }
        } else {
            //Assumes the selectedSpot hasn't moved much or pick the max intensity
            BoundedSpotData found = null;
            if( selectedSpot_ != null ) {
                found = searchForSelectedSpot( selectedSpot_);
            }
            try {
                if( found == null ) {
                    setSelectedSpotRaw( sortedPossibleFiducials_.get(0) );
                } else {
                    setSelectedSpotRaw( found );
                }
                maxIntensity_ = sortedPossibleFiducials_.get(0).getIntensity();
            } catch (Exception ex) {
                //We know there is a contained object in the list
                //  so the exception doesn't matter
                ex.getMessage();
            }
        }

        return sortedPossibleFiducials_.size();
    }
    
    /**
     *  Takes a spot and checks the sortedPossibleFiducial List for the spot that
     *  would be in the same location (overlap of the spot) selects the closest to the center.
     * <p>
     *  The search looks for the center of spots in the list that are within the searchSpot Area Bounds only.
     *  Not area-over-area overlap.
     * <p>
     * Note: This is different from tracking across a set of Fiducial Areas.  This function is used for
     * image refresh while selecting in live mode.  As long as no large movement occurs, this
     * is a lightweight way of maintaining the selected Spot for the user.
     * 
     * @param searchSpot - the spot, whose position and size are to be correlated to the list
     * @return - The Bounded Spot closest to the center of the search spot or null if there's no overlap
     */
    private BoundedSpotData searchForSelectedSpot( BoundedSpotData searchSpot ) {
        double x = searchSpot.getXPixelLocation();
        double y = searchSpot.getYPixelLocation();
        double halfWidth = searchSpot.getWidth()/2;
        BoundedSpotData minSpot = null;
        double minDeviation = Double.MAX_VALUE;
        double curDeviation = Double.MAX_VALUE;
        double dx, dy;
        for( BoundedSpotData spot: sortedPossibleFiducials_ ) {
            dx = spot.getXPixelLocation() - x;
            dy = spot.getYPixelLocation() - y;
            if( Math.abs( dx ) < halfWidth 
                    && Math.abs( dy ) < halfWidth ) {
                curDeviation = Math.sqrt( Math.pow( dx, 2) + Math.pow( dy, 2 ) );
                if( curDeviation < minDeviation ) {
                    minDeviation = curDeviation;
                    minSpot = spot;
                }
            }
        }
        
        return minSpot;
    }
    
    /**
     * Gets a moderately protected accessor to the selectedSpot to what would otherwise be returned as a raw reference.
     * <p>
     * I got ambitious with this one, when learning about it.
     * 
     * @return 
     */
    public CopySourceListReference<BoundedSpotData, FiducialArea> getSelectedSpot( ) {
        synchronized (spotLock_) {
            if (selectedSpot_ == null) {
                return null;
            }

            return new CopySourceListReference(selectedSpot_, this);
        }
    }
    
    
   /**
    * Get the raw reference to the current selected spot
    * 
    * @return 
    */
    public BoundedSpotData getRawSelectedSpot( ) {
        return selectedSpot_;
    }
    
    /**
     * Set the selectedSpot to a selected Spot returned from getSelectedSpot().
     * 
     * @param boundedSpotCopySource
     * @throws IllegalAccessException -- If the argument was not originally retrieved from the FiducialArea Instance
     * @throws Exception 
     */
    public void setSelectedSpot( CopySourceListReference< BoundedSpotData, FiducialArea> boundedSpotCopySource ) throws IllegalAccessException, Exception {
        setSelectedSpotRaw( boundedSpotCopySource.getListObjRef(this) );  //
    }
    
    /**
     * Gets the maximum intensity that was fit to the potential fiducial spots in this area
     * 
     * @return 
     */
    public double getMaxIntensity() {
        return maxIntensity_;
    }
    
    /** 
     * Sets the Selected Spot From a Raw Reference Instead of guaranteeing a copy
     * <p>
    *    Behavior - if the BoundedSpotData does not have isVirtual_ flag, it must be a reference contained in the list
    *                if isVirtual_ = true, it is set and added to the set until next set Operation, which replaces it
    * <p>
    *   Note: Virtual Spots are meant to be used sparingly due to their ability to increase the size of sortedPossibleFiducials_ 
    * 
    *   @param boundedSpotDataRef - reference to a BoundedSpotDataObject (expected within the set, or isVirual_)
    *   @throws - Exception when non-virtual Spot was not a part of sortedPossibleFiducials_
    */
    public void setSelectedSpotRaw( BoundedSpotData boundedSpotDataRef ) throws Exception {

        if ( !boundedSpotDataRef.isVirtual() && !sortedPossibleFiducials_.contains( boundedSpotDataRef )) {
            throw new Exception("Spot To Select Was not found in this Fiducial Area ");
        }
        
        synchronized( spotLock_ ) {
            if( boundedSpotDataRef.isVirtual() ) {
                if( selectedSpot_ != null && selectedSpot_.isVirtual() ) {
                    //Replace the virtual Spot
                    sortedPossibleFiducials_.set( sortedPossibleFiducials_.lastIndexOf(selectedSpot_), boundedSpotDataRef );
                }
                else {
                    sortedPossibleFiducials_.add( boundedSpotDataRef );
                }
            }
            //Create evt for notification
            SelectedFiducialChangeEvent evt = new SelectedFiducialChangeEvent(this, selectedSpot_, boundedSpotDataRef);
            //This can probably be done by other callers, but it's nice to maintain the references
            if (selectedSpot_ != null) {
                selectedSpot_.getBoundingBox().setStrokeColor(DefaultColor);
            }
            selectedSpot_ = boundedSpotDataRef;
            selectedSpot_.getBoundingBox().setStrokeColor(SelectColor);
            //Change Later to be form model structure
            //Set Search Area to fit the Selected Spot...
            if ( selectedSpot_ != null ) {
                boolean widthChange = false;
                fiducialAreaProcessor_.lockSettings();
                try {
                    if( frameTravelAnticipated_ !=  fiducialAreaProcessor_.getCurrentSettings().getMaxTrackTravel() )
                    {
                        frameTravelAnticipated_ = fiducialAreaProcessor_.getCurrentSettings().getMaxTrackTravel();
                        widthChange = true;
                    }
                    
                } finally {
                  fiducialAreaProcessor_.unlockSettings();
                }
                //Solution for setWidths of Roi
                if ( trackSearchArea_ == null || widthChange ) {
                    trackSearchArea_ = new Roi(selectedSpot_.getXPixelLocation() - frameTravelAnticipated_, selectedSpot_.getYPixelLocation() - frameTravelAnticipated_, frameTravelAnticipated_ * 2, frameTravelAnticipated_ * 2);
                } else {
                    trackSearchArea_.setLocation(selectedSpot_.getXPixelLocation() - frameTravelAnticipated_, selectedSpot_.getYPixelLocation() - frameTravelAnticipated_);
                }
                
                //Modify for refDimension if this is in reference to an FArea Series
                if ( refSigmaNaut_ != 0 ) {
                    //Selected Spot Divided By 4 must be changed later
                    double dSigma = selectedSpot_.getWidth() / 4 - refSigmaNaut_;
                    double refIdx = fiducialAreaProcessor_.getMicroscopeModel().getObjectiveRefractiveIdx();
                    double numAp = fiducialAreaProcessor_.getMicroscopeModel().getNumericalAperture();
                    relativeDz_ = dSigma * refIdx / numAp ;
                    relativeDzUncertainty_ = Math.sqrt(Math.pow(refSigmaNautUncertainty_, 2)
                            + Math.pow(selectedSpot_.getSigma(), 2)) * refIdx / numAp;
                    //ij.IJ.log("The Spot Sigma is: " + selectedSpot_.getWidth()/4 + "\nThe Relative Change in z is Calculated to be: " + relativeDz_
                            //+ "\nThe Uncertainty is: " + relativeDzUncertainty_);
                }

            } else {
                //Contingency for faulty selectedSpot_ in a track
                if( refSigmaNaut_ != 0 ) {
                    IJMMReportingUtils.showError(  "Somehow A TrackRegion Could not"
                                                + " create a spot to track" );
                }
            }
            
            //Notify after all changes
            dispatchEvent(evt);
        }
    }
    
    //Unsafe For Threads
    public List<BoundedSpotData> getAllRawPossibleSpots( ) {
        return sortedPossibleFiducials_;
    }
    
    /**
     * Copy Constructs with no link to List but to objects
     */
    public List< BoundedSpotData > getAllPossibleSpotsCopy() {
        return new ArrayList< BoundedSpotData >( sortedPossibleFiducials_ );
    }
    
    /**
     * This is an expensive Operation, but it is typically going to be called from UI Threads, which are not processing heavy
     */
    public ArrayList<CopySourceListReference < BoundedSpotData, FiducialArea> > getAllPossibleSpots() {
        ArrayList< CopySourceListReference < BoundedSpotData, FiducialArea> > safeList = 
                new ArrayList<CopySourceListReference < BoundedSpotData, FiducialArea> >();
        for( BoundedSpotData spot : sortedPossibleFiducials_ ) {
            safeList.add( new CopySourceListReference < BoundedSpotData, FiducialArea>( spot, this ) );
        }
        return safeList;
    }
    
    /**
     *  Comparator for ordering SpotData By Maximum Value first [0-index]
     *  This should allow for isolation of brightest points in the future
     */
    private class MaxIntensityFirst implements Comparator<SpotData> {

        @Override
        public int compare(SpotData o1, SpotData o2) {
            if( o1.getIntensity() > o2.getIntensity() ) {
                return -1;
            }
            if( o1.getIntensity() < o2.getIntensity() ) {
                return 1;
            }
            return 0;
        }
               
    }
    
    /**
     * Comparator for ordering SpotData By Minimum Width First
     *   This is used to PreFilter Diffraction Limited Spots That May Defocus
     *   Since Any Defocus in Diffraction Limit should be within 2*MinWidth
     */
    private class MinWidthFirst implements Comparator<SpotData> {
        
        @Override
        public int compare( SpotData o1, SpotData o2) {
            if( o1.getWidth() < o2.getWidth() ) {
                return -1;
            }
            if( o1.getWidth() > o2.getWidth() ) {
                return 1;
            }
            return 0;
        }
        
    }
    
    /**
     *  Gets the refSigmaNaut_ associated with the first fiducial Area Copied.  This 
     *   Sigma refers to the sigma of the point spread function for the measured intensity
     *   across the image. This should be used in Conjuction with getFocusPlaneSigmaRefUncertainty()
     *   which returns the uncertainty on the sigma.  This constitutes a focus plane point and circle
     *   that is used for simple calculation of expansion of the image (out of defocus plane)
     * 
     * @return The Gaussian sigma of a Gaussian Distribution center spot on the image at the assumed reference focus plane
     */
    public double getFocusPlaneSigmaRef ( ) {
        return refSigmaNaut_;
    }
    
    /**
     * Returns the distance (in nm) from the reference (in focus) plane.  This number
     *  is set by comparing the sigma of any spot set with SetSelectedSpot() and 
     *  refSigmaNaut_.  refSigmaNaut_ is only set with regards to the modified (track) 
     *  copy constructor, which assumes the  previous area is a track reference.
     * <p>
     *  Note: If This Fiducial Area is set by a virtual selected Spot, the returned value 
     *  can only be based on the previous (copied) FiducialArea and is suspect for any measurement.
     *  It is advised to exclude this value if getVirtualFrameTrackNumber() > 0.
     * 
     * @return returns the distance from the reference plane as calculated via a selectedSpot
     * 
     * @see #FiducialArea(ij.ImagePlus, ij.gui.Roi, edu.hope.superresolution.models.FiducialArea) 
     * @see #getVirtualFrameTrackNumber() 
     */
    public double getRelativeFocusDistance( ) {
        return relativeDz_;
    }
    
    /**
     *  Gets the uncertainty associated with the value returned by getRelativeFocusDistance
     * 
     * @return 
     * 
     * @see #getRelativeFocusDistance() 
     */
    public double getRelativeFocusDistanceUncertainty( ) {
        return relativeDzUncertainty_;
    }
    
    /**
     *  Gets the uncertainty associated with the value returned by getFocusPlaneSigmaRef
     * 
     * @return 
     * 
     * @see #getFocusPlaneSigmaRef() 
     */
    public double getFocusPlaneSigmaRefUncertainty( ) {
        return refSigmaNautUncertainty_;
    }
    
    
    /**
     * Allows for External Settings of the Reference Sigma of the PSF.  This reference 
     *  Sigma is in relation to the "most focused plane" And will be adjusted in AutoFocus
     * If there is a negative Z value.
     *
     * @param newSigmaNaut - The new Sigma Naut of the diffraction limited psf for the "focused plane"
     */
    public void setRefSigmaNaut( double newSigmaNaut ){
        refSigmaNaut_ = newSigmaNaut;
    }
    
    /**
     * Returns the sigma naut of the intensity psf that is currently associated with the 
     * focus plane.
     * 
     * @return 
     */
    public double getRefSigmaNaut() {
        return refSigmaNaut_;
    }
    
    //Local Callback for updating the BoundedSpotList when a Gaussian Fit is performed
    //Runs of opposite Thread
    public class BoundedSpotListAction implements ListCallback<SpotData> {

        private final FiducialArea refInstance_;
        
        public BoundedSpotListAction( FiducialArea refInstance ) {
            refInstance_ = refInstance;
        }
        
        @Override
        public void onListFull( List<SpotData> list) {
            try {
                //populate and Sort From Max to Min
                populateBoundedSpotList(list);
            } catch (Exception ex) {
                IJMMReportingUtils.showError(ex);
            }
          //Indicate that we have performed a full List Operation
          setIsChanging(false);
          setHasChanged(true);
          
          //Notify that the list has been populated
          dispatchEvent( new SpotSearchRepopulatedEvent( refInstance_ ) );
               
        }

        @Override
        public void onListElementAdded(List<SpotData> list) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onListElementRemoved(List<SpotData> list) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    
    }
    
}

