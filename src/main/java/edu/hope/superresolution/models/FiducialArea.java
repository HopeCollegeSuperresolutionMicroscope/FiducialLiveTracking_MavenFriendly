/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.Utils.IJMMReportingUtils;
import edu.hope.superresolution.genericstructures.CopySourceListReference;
import edu.hope.superresolution.genericstructures.FiducialTravelDiff2D;
import ij.gui.Roi;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import edu.hope.superresolution.genericstructures.ListCallback;
import edu.hope.superresolution.genericstructures.MasterSpecificEvent;
import edu.hope.superresolution.genericstructures.QueuedStateBroadcaster;
import edu.hope.superresolution.genericstructures.RefreshEvent;
import edu.hope.superresolution.genericstructures.iDriftModel;
import edu.hope.superresolution.processors.FiducialAreaProcessor;
import edu.valelab.gaussianfit.data.SpotData;
import ij.ImagePlus;
import java.awt.geom.Point2D;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * Container of Information pertaining to a user-selected area in a image area, 
 * in which a a user selected fiducial is contained
 * 
 * @author Microscope
 */
public class FiducialArea extends QueuedStateBroadcaster {    
    
    /**
     * Event Class - Aliasing Definition For periodic RefreshEvent when SelectedFiducialChangeEvent Occurs
     */
    public static class SpotSelectionRefreshEvent extends RefreshEvent<FiducialArea, SelectedFiducialChangeEvent> {
        
        /**
         * Generic Constructor
         * 
         * @param originator
         * @param evt 
         */
        public SpotSelectionRefreshEvent(FiducialArea originator, SelectedFiducialChangeEvent evt) {
            super(originator, evt);
        }

        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource
         * @param evt 
         */
        public SpotSelectionRefreshEvent(SpotSelectionRefreshEvent originatorSource, SelectedFiducialChangeEvent evt ) {
            super(originatorSource, evt);
        }
        
    }
    
    /**
     * Event Class - Aliasing Definition For periodic RefreshEvent when RefSigmaNautChangeEvent Occurs
     */
    public static class RefSigmaNautRefreshEvent extends RefreshEvent<FiducialArea, RefSigmaNautChangeEvent> {
        
        /**
         * Generic Constructor
         * 
         * @param originator
         * @param evt 
         */
        public RefSigmaNautRefreshEvent(FiducialArea originator, RefSigmaNautChangeEvent evt) {
            super(originator, evt);
        }

        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource
         * @param evt 
         */
        public RefSigmaNautRefreshEvent(RefSigmaNautRefreshEvent originatorSource, RefSigmaNautChangeEvent evt ) {
            super(originatorSource, evt);
        }
        
    }
    
    /**
     * Event Class - The PSF that is used as a reference for Z-depth and deviation has changed
     * 
     */
    public static class RefSigmaNautChangeEvent extends MasterSpecificEvent {
        
        private final double prevRefSigmaNautValue_;
        private final double curRefSigmaNautValue_;
        private final double prevRefSigmaNautUncertainty_;
        private final double curRefSigmaNautUncertainty_;
        
        /**
         * Constructor
         * 
         * @param originator - originating instance for Roi change
         * @param prevRefSigmaNautValue The previous sigmaNaut value of the PSF
         * @param prevRefSigmaNautUncertainty The uncertainty associated with the previous SigmaNaut
         * @param curRefSigmaNautValue The current sigmaNaut value of the reference PSF
         * @param curRefSigmaNautUncertainty The uncertainty associated with the current SigmaNaut
         */
        public RefSigmaNautChangeEvent( FiducialArea originator, double prevRefSigmaNautValue, double prevRefSigmaNautUncertainty,
                                            double curRefSigmaNautValue, double curRefSigmaNautUncertainty ) {
            super(originator);

            prevRefSigmaNautValue_ = prevRefSigmaNautValue;
            curRefSigmaNautValue_ = curRefSigmaNautValue;
            prevRefSigmaNautUncertainty_ = prevRefSigmaNautUncertainty;
            curRefSigmaNautUncertainty_ = curRefSigmaNautUncertainty;

        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource The Event that already has an Originator to copy into this instance
         * @param prevRefSigmaNautValue The previous sigmaNaut value of the PSF
         * @param prevRefSigmaNautUncertainty The uncertainty associated with the previous SigmaNaut
         * @param curRefSigmaNautValue The current sigmaNaut value of the reference PSF
         * @param curRefSigmaNautUncertainty The uncertainty associated with the current SigmaNaut
         */
        public RefSigmaNautChangeEvent( RefSigmaNautChangeEvent originatorSource, double prevRefSigmaNautValue, double prevRefSigmaNautUncertainty,
                                            double curRefSigmaNautValue, double curRefSigmaNautUncertainty ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevRefSigmaNautValue_ = prevRefSigmaNautValue;
            curRefSigmaNautValue_ = curRefSigmaNautValue;
            prevRefSigmaNautUncertainty_ = prevRefSigmaNautUncertainty;
            curRefSigmaNautUncertainty_ = curRefSigmaNautUncertainty;
            
        }
        
        public double getPreviousRefSigmaNaut() {
            return prevRefSigmaNautValue_;
        }
        
        public double getCurrentRefSigmaNaut() {
            return curRefSigmaNautValue_;
        }
        
        public double getPreviousRefSigmaNautUncertainty() {
            return prevRefSigmaNautUncertainty_;
        }

        public double getCurrentRefSigmaNautUncertainty() {
            return curRefSigmaNautUncertainty_;
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
            if( !(event instanceof RefSigmaNautChangeEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            RefSigmaNautChangeEvent upCast = (RefSigmaNautChangeEvent) event;
            return new RefSigmaNautChangeEvent( upCast, prevRefSigmaNautValue_, prevRefSigmaNautUncertainty_, upCast.curRefSigmaNautUncertainty_, upCast.curRefSigmaNautValue_ );
        }
        
    }
    
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
         * @param originator originating instance for Roi change
         * @param prevSelected The previous Spot that was the selected Fiducial of the FiducialArea
         * @param curSelected The current Spot that was the selected Fiducial of the FiducialArea
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
         * @param prevSelected The previous Spot that was the selected Fiducial of the FiducialArea
         * @param curSelected The current Spot that was the selected Fiducial of the FiducialArea
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
    private BoundedSpotData trackRefSpot_ = null;
    private int trackRefFrameNum_ = 0;
    
    //Defocus Tracking members
    private boolean externalSigmaNautRef_ = false; //whether or not this has an external sigmaNautRef or the SelectedSpot is the reference
    private double refSigmaNaut_ = 0;  //Gaussian SigmaNaut from referenced in focus plane (this is 0 unless being tracked)
    private double refSigmaNautUncertainty_ = 0; //Uncertainty in the SigmaNaut (stdDev in Average)
    
    //Drift Model Storing any track information for distance from the Focus Plane and any Track Spot
    private iDriftModel trackDriftInfo_ = new LinearDriftModel3D(1,0,0,0,0,0,0,iDriftModel.DriftUnits.nm); //Model of where the current FiducialArea is tracked to have drifted from a previous area
    //storage for updating FiducialAreas that are listeners, or anything listening for update cycles
    private int refreshDependentQueueHandle_; //The handle to a queue that will dispatch events on properties that are finalized through update Refreshed
    private FiducialArea sourceDependentFArea_ = null; //FiducialArea that this Fiducial Area is dependent on changes for
    private int immediateProcessEndQueueHandle_; //The handle to a queue that will dispatch events nearly immediately.  By that I mean, at the end of a context, like after spot selection has done everything, and the refSigmaNaut has potentially changed. 
    
    //To Be Made a Variable
    private double frameTravelAnticipated_ = 100;  //In Pixels
    
    /**
    *   General Constructor (Unconcerned with Frame Track Number or reference plane sigmaNauts)
    */
    public FiducialArea( ImagePlus ip, Roi roi, FiducialAreaProcessor fAreaProcessor ) {
       
        //Create queue
        refreshDependentQueueHandle_ = generateNewQueue();
        immediateProcessEndQueueHandle_ = generateNewQueue();
        
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
        externalSigmaNautRef_ = false;
        
        setSearchArea(roi);   
    }
    
    /**
    *   Modified Copy Constructor - Implied Track
    *   <p>
    *   This Constructor assumes that the FiducialArea being copied is a reference point.  What this means is that
    *   the Fiducial Area's PSF reference dimensions will be treated as the same reference dimensions. If none exist,
    *   it is assumed the FiducialArea selectedSpot will be the reference dimension.  In this way, a reference plane (or instance)
    *   can be shared by all FiducialAreas created through this constructor.
    *   <p>
    *   Additionally, since every Fiducial Area is the result of a particular fiducialAreaProcessor and some of these processor settings must be changed, 
    *   the FiducialArea will also obtain the reference to the pertaining processor to its base and modify intensity.  (To Be Changed)
    *   <p>
    *   If the previous FiducialArea was had a virtually generated spot (which is normally meant to be a placeholder operation during tracks),
    *   The currently constructed instance will also increment it's virtualFrameTrackNumber_ to account for fact that it is a part of 
    *   a continuous set of non-real spots.
    * 
    *   @param ip - new ImagePlus meant to be evaluated by new Fiducial Area
    *   @param roi - new Roi for this Fiducial Area (typically shifted)
    *   @param baseFArea - Fiducial Area that is used as the previous model in a track
    *                        Used for fAreaProcessor and virtualFrameTrackNumber_
    */
    private FiducialArea( ImagePlus ip, Roi roi, FiducialArea baseFArea ) {
        
        //Create queue
        refreshDependentQueueHandle_ = generateNewQueue();
        immediateProcessEndQueueHandle_ = generateNewQueue();
        
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
        refSigmaNaut_ = baseFArea.refSigmaNaut_;//(baseFArea.refSigmaNaut_ != 0) 
                                 //? baseFArea.refSigmaNaut_ : baseFArea.selectedSpot_.getWidth()/4;
        refSigmaNautUncertainty_ = baseFArea.refSigmaNautUncertainty_;//(baseFArea.refSigmaNautUncertainty_ != 0) 
                                        //? baseFArea.refSigmaNautUncertainty_ : baseFArea.selectedSpot_.getSigma();
        externalSigmaNautRef_ = true;
                                        
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
        
        //Store the baseFArea reference for stronger busEvent Type Checking
        sourceDependentFArea_ = baseFArea;

    }
    
    /**
    *   Creates a FiducialArea that draws certain traits from another Fiducial Area and tracks them.  This means
    *   the resulting FiducialArea is dependent on certain features of previous FiducialArea and can be seen as implicitly 
    *   tracking the base Fiducial Area and the set that they make together.
    *   <p>
    *   This Constructor assumes that the FiducialArea being copied is a reference point.  What this means is that
    *   the Fiducial Area's PSF reference dimensions will be treated as the same reference dimensions. If none exist,
    *   it is assumed the base FiducialArea selectedSpot will be the reference dimension.  In this way, a reference plane (or instance)
    *   can be shared by all FiducialAreas with a common ancestor created through this construction method.
    *   <p>
    *   Additionally, since every Fiducial Area is the result of a particular fiducialAreaProcessor and some of these processor settings must be changed, 
    *   the FiducialArea will also obtain the reference to the pertaining processor to its base and modify intensity.  (To Be Changed)
    *   <p>
    *   If the previous FiducialArea had a virtually generated spot (which is normally meant to be a placeholder operation during tracks),
    *   The currently constructed instance will also increment it's virtualFrameTrackNumber_ to account for the fact that it is a part of 
    *   a continuous set of non-real spots.
    *   <p>
    *   The FiducialArea created by this method will be registered to the EventBus of the base FiducialArea and will
    *   listen for dispatched events from the RefresehDependentObjects() method of that instance.
    * 
    *   @param ip - new ImagePlus meant to be evaluated by new Fiducial Area
    *   @param roi - new Roi for this Fiducial Area (typically shifted)
    *   @param baseFArea - Fiducial Area that is used as the previous model in a track
    *                        Used for fAreaProcessor and virtualFrameTrackNumber_
    * 
    *   @see #FiducialArea(ij.ImagePlus, ij.gui.Roi, edu.hope.superresolution.models.FiducialArea) 
    *   @see #refreshDependentObjects() 
    */
    public static FiducialArea createLinkedFiducialArea( ImagePlus ip, Roi roi, FiducialArea baseFArea ) {
        FiducialArea fArea = new FiducialArea(ip, roi, baseFArea);
        
        //Register the FiducialArea to the the baseFArea after construction
        baseFArea.RegisterToStateEventBus(fArea);
        
        return fArea;
    }
    
    /**
     * Returns the number of times that this Fiducial Area has been tracking a virtual (undetected particle).
     * This results from use of the Implied Track Constructor, since there is a potential for a fiducial area
     * to have a missing fiducial but to track with th set of fiducials.  If the particle is rediscovered in 
     * this FiducialArea, the VirtualFrameNumber will be reset to 0.  
     * 
     * @return 
     * @see FiducialArea#FiducialArea(ij.ImagePlus, ij.gui.Roi, edu.hope.superresolution.models.FiducialArea) 
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
    /**
     * Sets and Processes the Area over which the ImagePlus of the FiducialArea is processed by the current {@link FiducialAreaProcessor}.
     * <p>
     * In order to make this function thread safe, the method will return false if the FiducialArea is still currently processing, 
     * as indicated by {@link #getIsChanging()} and {@link #getHasChanged()}.  Callers of this method in a multi-threaded context, should thus
     * check to make sure the operation is successful and block on the result if waiting.
     * <p>
     * Note: Setting the Search Area will result in the necessary reprocessing of the search area and so the return only deals with the sucessful setting
     * of the search Area.
     * 
     * @hope.superresolution.todo Change this rudimentary thread locking to something that actually allows interrupts and waking patterns
     * @hope.superresoltion.dispevent {@link RoiChangeEvent}
     * 
     * @param roi - The Roi to store as the 
     * @return - true or false depending on if the searchArea was actually set or if it returned due to busy implementations. 
     */
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
                //Immediately dispatch the Event Queue
                postToEventQueue( immediateProcessEndQueueHandle_, new RoiChangeEvent( this, prevRoi, origSelectionArea_ ));
                dispatchEventQueue(immediateProcessEndQueueHandle_);
            }
            setIsChanging(false);
            return false;
        }
        
        setHasChanged(false);
        
        origSelectionArea_ = roi;
        //Dispatch Roi Change Event
        postToEventQueue( immediateProcessEndQueueHandle_, new RoiChangeEvent( this, prevRoi, origSelectionArea_ ) );
        dispatchEventQueue(immediateProcessEndQueueHandle_);
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
    
    /**
     * Sets whether or not the object should have changed.  This is only really an indicator
     * if a spot list was returned and it was successful submitted to repopulate.  It is not an indepth 
     * indication of if the spot list is any different from the last one.
     * @param state 
     */
    private void setHasChanged( boolean state ) {
        synchronized( changeLock_ ) {
            hasChanged_ = state;
        }       
    }
    
    /**
     * Sets whether or not the FiducialArea is being processed.
     * 
     * @param state 
     */
    private void setIsChanging( boolean state ) {
        synchronized( changeLock_ ) {
            isChanging_ = state;
        }
    }
    
    /**
     * Gets Whether or not the FiducialArea is being processed by the FiducialProcessor or is in the process of being stored.
     * 
     * @return 
     */
    public boolean getIsChanging( ) {
        synchronized( changeLock_ ) {
            return isChanging_;
        }
    }
    
    /**
     * Gets whether or not a fiducialprocessor and spot store operation was successfully executed on the FiducialArea.
     * 
     * @hope.superresolution.todo Make this much more specific and guard against incomplete operations that still may indicate a change. 
     *                             This is leftover from the pre-event model and early thread synchronization designs...
     * 
     * @return 
     */
    public boolean getHasChanged( ) {
        synchronized( changeLock_ ) {
            return hasChanged_;
        }
    }
    
    /**
     * Populates the List of sorted Possible Fiducials with the new list and selects a Fiducial
     * that is either in the same area as the previous list, or is the first fiducial in the list.
     * <p>
     * Will clear the previous list.
     * 
     * @hope.superresolution.genevent SelectedFiducialChangeEvent If new search yields no potential spots when the previous one did
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
                postToEventQueue( immediateProcessEndQueueHandle_, new SelectedFiducialChangeEvent(this, selectedSpot_, null ) );   
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
    *   @hope.superresolution.dispevent {@link SelectedFiducialChangeEvent} upon finishing the function and any other pending events
    *   @hope.superresolution.genevent {@link SpotSelectionRefreshEvent} for disaptching with {@link #refreshDependentObjects()}
    * 
    *   @param boundedSpotDataRef - reference to a BoundedSpotDataObject (expected within the set, or isVirual_)
    *   @throws Exception when non-virtual Spot was not a part of sortedPossibleFiducials
    * 
    *   @hope.superresolution.todo Better Handling for if there is a Null Selected Spot But a SigmaRef
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
                
                //If this is the reference plane FiducialArea, we set it's ref sigmanaut to be the selectedSpot_
                if( !externalSigmaNautRef_ ) {
                    //This will not update the drift model, since there is no change from 0 to the Z Plane
                    setRefSigmaNautInternal(selectedSpot_.getWidth()/4, selectedSpot_.getSigma() );
                } else {
                    //Selected Spot Divided By 4 must be changed later
                    /*double dSigma = selectedSpot_.getWidth() / 4 - refSigmaNaut_;
                    double refIdx = fiducialAreaProcessor_.getMicroscopeModel().getObjectiveRefractiveIdx();
                    double numAp = fiducialAreaProcessor_.getMicroscopeModel().getNumericalAperture();
                    relativeDz_ = dSigma * refIdx / numAp ;
                    relativeDzUncertainty_ = Math.sqrt(Math.pow(refSigmaNautUncertainty_, 2)
                            + Math.pow(selectedSpot_.getSigma(), 2)) * refIdx / numAp;*/
                    double xTranslation = 0, yTranslation = 0, xUncertainty = 0, yUncertainty = 0;
                    //ij.IJ.log("The Spot Sigma is: " + selectedSpot_.getWidth()/4 + "\nThe Relative Change in z is Calculated to be: " + relativeDz_
                            //+ "\nThe Uncertainty is: " + relativeDzUncertainty_);
                    //This is the Z distance from the reference plane at which the reference PSF was taken
                    Point2D.Double dZObj = computeRelativeZDistanceAndUncertainties();
                    //If this has a TravelDiff associated with it, and therefore a track spot from a different FiducialArea
                    if( trackRefSpot_ != null ) {
                        //Reuse of static functions for uniformity since this is likely to be changed in the future
                        //Does induce overhead
                        xTranslation = FiducialTravelDiff2D.computeXTravelDiff(trackRefSpot_, selectedSpot_);
                        yTranslation = FiducialTravelDiff2D.computeYTravelDiff(trackRefSpot_, selectedSpot_);
                        xUncertainty = FiducialTravelDiff2D.computeXTravelDiffUncertainty(trackRefSpot_, selectedSpot_);
                        yUncertainty = FiducialTravelDiff2D.computeYTravelDiffUncertainty(trackRefSpot_, selectedSpot_);
                    } else {
                        //Redundancy here again but this is for certainty
                        trackRefFrameNum_ = 0;
                        //trackDriftInfo_ = new LinearDriftModel3D( 0, 0, 0, 0, 0, 0, 0, iDriftModel.DriftUnits.nm );
                    }
                    updateDriftInfo( trackRefFrameNum_, xTranslation, xUncertainty, yTranslation, yUncertainty, dZObj.x, dZObj.y );
                }

            } else {
                //Contingency for faulty selectedSpot_ in a track
                if( refSigmaNaut_ != 0 ) {
                    IJMMReportingUtils.showError(  "Somehow A TrackRegion Could not"
                                                + " create a spot to track" );
                }
            }
            
            //Notify after all changes
            //Immediate Change
            postToEventQueue(immediateProcessEndQueueHandle_, evt);
            //RefreshEvent Change
            postToEventQueue( refreshDependentQueueHandle_, new SpotSelectionRefreshEvent(this, evt));
        }
        //Dispatch any immediate event changes now that process if over
        dispatchEventQueue(immediateProcessEndQueueHandle_);
    }
    
    /**
     * Take a travel difference and apply it to the FiducialArea.
     * This will change the selectedSpot to the selected spot in the travel difference set
     * and will create a non-zero iDriftModel Object for reference.  
     * <p>
     * Note that, while FiducialLocationModels will aggregate this normally, this allows some level of 
     * meaningful FiducialArea Drift Resolution.
     * 
     * @hope.superresolution.dispevent {@link #setSelectedSpotRaw(edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData) } Event dispatch
     * 
     * @param travelDiff - The travel difference set that's info will be used to select the spot
     * @param frameNumber - The frameNumber of the current acquistion this track is applied to.  Should be context specific
     * @throws java.lang.Exception If the spotRef from the travelDiff is non-virtual and is not a spot that exists in the set of potential spots
     */
    public void applyTravelDifferenceAsDrift( FiducialTravelDiff2D travelDiff, int frameNumber ) throws Exception {
        assert( travelDiff.areaOwnerRef_ == this );
        
        trackRefSpot_ = travelDiff.prevSpot_;
        //This will change with changes to iDriftModel
        trackRefFrameNum_ = frameNumber;
        
        //Set the Selected spot using just the curSpot_ and recalculating
        //This is a redundancy right now, but will need to change with a reworking of translational data and methods
        //Note, this public facing method will dispatch its own spots
        setSelectedSpotRaw(travelDiff.spotRef_);
       
    }
    
    /**
     * Separable function for improvement of DriftModel manipulation features later
     * Use this function rather than reassigning trackDriftInfo_.  
     * TODO - Make a SuperClass method for this sake
     * 
     */
    private void updateDriftInfo( int frameNumber, double xDriftNm, double xDriftUncertainty, double yDriftNm, 
                                           double yDriftUncertainty, double ZdriftNm, double zDriftUncertainty ) {
        
        trackDriftInfo_ = new LinearDriftModel3D( frameNumber, xDriftNm, xDriftUncertainty, 
                                    yDriftNm, yDriftUncertainty, ZdriftNm, zDriftUncertainty, iDriftModel.DriftUnits.nm );
    }
    
    /**
     * Will return the current DriftInfo.
     * <p>
     * The information is valuable as such:
     * <p>
     * x and y translation will be meaningless and should be 0, unless a trackSpot was created by applying applyTravelDifferenceAsDrift().
     * <p>
     * z translation will be meaningful in regards to a &gt;zero FocusPlaneSigmaRef.  The estimate will depend on the estimation technique, which at this point is not flagged.
     * <p>
     *  Note about z: If This Fiducial Area is set by a virtual selected Spot, the returned value 
     *  is suspect for any calculation. It is advised to exclude this value if getVirtualFrameTrackNumber() &gt; 0.
     * <p>
     * The frame number will be delegate from an external context and is expected to relate to some imageStack or combination of image stacks being tracked by the  context
     * 
     * @return The current lateral movement from a reference spot and the estimated z distance of the fiducial from a given reference plane
     * 
     * @see #applyTravelDifferenceAsDrift(edu.hope.superresolution.genericstructures.FiducialTravelDiff2D, int) 
     * @see #getFocusPlaneSigmaRef() 
     * @see #getFocusPlaneSigmaRefUncertainty() 
     * @see #getVirtualFrameTrackNumber() 
     */
    public iDriftModel getDriftInfo() {
        return trackDriftInfo_;
    }
    
    /**
     * Computes the RelativeDistance the current selectedSpot is from the Reference Sigma Naut of a PSF and the associated Z Uncertainties
     * <p>
     * Note:  Currently makes use of static processes in FiducialTravelDiff2D and microscope model parameters store in a fiducialAreaProcessor
     * 
     * @return A Point Object where the x axis is Z and the y axis is Zuncertainty
     * 
     * @see FiducialTravelDiff2D
     * @see FiducialAreaProcessor
     */
    private Point2D.Double computeRelativeZDistanceAndUncertainties() {
        double refIdx = fiducialAreaProcessor_.getMicroscopeModel().getObjectiveRefractiveIdx();
        double numAp = fiducialAreaProcessor_.getMicroscopeModel().getNumericalAperture();
        double relativeDz = FiducialTravelDiff2D.computeZLocationOfSpot(refSigmaNaut_, refIdx, numAp, selectedSpot_);
        double relativeDzUncertainty = FiducialTravelDiff2D.computeZLocationUncertainty(refSigmaNautUncertainty_, refIdx, numAp, selectedSpot_);
        return new Point2D.Double(relativeDz, relativeDzUncertainty);
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
     * Returns the sigma naut of the Fiducial PSF that is currently associated with the 
     * focus plane.
     * <p>
     *  This Sigma refers to the sigma of the point spread function for the measured intensity
     *  across the image. This should be used in Conjuction with getFocusPlaneSigmaRefUncertainty()
     *  which returns the uncertainty on the sigma.  This constitutes a focus plane point and circle
     *  that is used for simple calculation of expansion of the image (out of defocus plane)
     * 
     * @return The Gaussian sigma of a Gaussian Distribution center spot on the image at the assumed reference focus plane
     * @see #getFocusPlaneSigmaRefUncertainty() 
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
     *  It is advised to exclude this value if getVirtualFrameTrackNumber() &gt; 0.
     * 
     * @return returns the distance from the reference plane as calculated via a selectedSpot
     * 
     * @see #FiducialArea(ij.ImagePlus, ij.gui.Roi, edu.hope.superresolution.models.FiducialArea) 
     * @see #getVirtualFrameTrackNumber() 
     * @see #getRelativeFocusDistanceUncertainty() 
     */
    /*public double getRelativeFocusDistance( ) {
        return relativeDz_;
    }*/
    
    /**
     *  Gets the uncertainty associated with the value returned by getRelativeFocusDistance
     * 
     * @return 
     * 
     * @see #getRelativeFocusDistance() 
     */
    /*public double getRelativeFocusDistanceUncertainty( ) {
        return relativeDzUncertainty_;
    }*/
    
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
     * Allows for External Setting of the Reference Sigma of the PSF.  This reference 
     *  Sigma is in relation to the "most focused plane" And will be adjusted in AutoFocus
     * If there is a negative Z value.
     *
     * @hope.superresolution.dispevent RefSigmaNautChangeEvent upon each change
     * @hope.superresolution.genevent RefreshEvent&lt;FiducialArea, RefSigmaNautChangeEvent&gt; toward the Event Queue
     * 
     * @param newSigmaNaut - The new Sigma Naut of the diffraction limited psf for the "focused plane"
     * @param newSigmaNautUncertainty - The uncertainty associated with this value
     */
    public void setRefSigmaNaut( double newSigmaNaut, double newSigmaNautUncertainty ){
        
        externalSigmaNautRef_ = true;
        setRefSigmaNautInternal(newSigmaNaut, newSigmaNautUncertainty);
        
        //Dispatch the Event Queue since internal operation is just generating immediate events
        dispatchEventQueue( immediateProcessEndQueueHandle_ );
    }
    
    /**
     * Sets the refSigmaNaut_ and refSigmaNautUncertainty_ of the Fiducial Area.
     * <p>
     * Separate the events and encapsulates the dispatching of refSigmaNautChangeEvents.
     * Will update the Z information for Drift Tracking if the FiducialArea references an external refSigmaNaut
     * 
     * @hope.superresolution.genevent RefSigmaNautChangeEvent upon each change
     * @hope.superresolution.genevent RefreshEvent&lt;FiducialArea, RefSigmaNautChangeEvent&gt; toward the Event Queue
     * 
     * @param newSigmaNaut - The new Sigma Naut of the diffraction limited psf for the "focused plane"
     * @param newSigmaNautUncertainty - The uncertainty associated with this value
     */
    private void setRefSigmaNautInternal( double newSigmaNaut, double newSigmaNautUncertainty ) {
        RefSigmaNautChangeEvent evt = new RefSigmaNautChangeEvent(this, refSigmaNaut_, refSigmaNautUncertainty_, newSigmaNaut, newSigmaNautUncertainty );
        
        refSigmaNaut_ = newSigmaNaut;
        refSigmaNautUncertainty_ = newSigmaNautUncertainty;
        
        if( this.externalSigmaNautRef_ ) {
            Point2D.Double dZObj = computeRelativeZDistanceAndUncertainties();

            //We Will need to update the Dz of this model
            updateDriftInfo(trackDriftInfo_.getFrameNumber(), trackDriftInfo_.getXFromStartTranslation(),
                    trackDriftInfo_.getXTranslationUncertainty(), trackDriftInfo_.getYFromStartTranslation(), trackDriftInfo_.getYTranslationUncertainty(),
                    dZObj.x, dZObj.y);
        }
        
        //Post to the immediate Dispatch Queue for release by calling methods
        postToEventQueue( immediateProcessEndQueueHandle_, evt );
        //Add to Event Queue As a RefreshEvent that will be dispatched on refresh call
        postToEventQueue( refreshDependentQueueHandle_, new RefSigmaNautRefreshEvent( this, evt ) );
    }
    
    /**
     * Refreshes all objects that are dependent on this FiducialArea.  These objects have been subscribed to
     * the eventQueue that stores events until this method is called.  In this way, events added to the
     * refreshEventQueue will not update immediately on a change. This is the difference of an indecisive user selection of the correct fiducial,
     * (graphically showing every time) and an "I'm certain" button press which all non-visual models will need to update on.
     * <p>
     * As a matter of event separation, since the object is using the same bus, the refreshQueue should only dispatch RefreshEvents of any events
     * that are simultaneously
     * 
     * @see #addToRefreshQueue(edu.hope.superresolution.genericstructures.RefreshEvent) 
     */
    public void refreshDependentObjects() {
        dispatchEventQueue(refreshDependentQueueHandle_);
    }
    
    /*
     * Subscriber Functions For Fiducial Area Implicit Track Functions
     */
     
    /**
     * Subscription Event to the dispatch of a SigmaNaut of the PSF for the given focus plane changing.
     * <p>
     * This will be registered in the Implied Track Constructor, and will listen for any dispatches from the RefreshDependentObjects 
     * method that match this event type.  It is intended to only listen to the FiducialArea that provided its SigmaNaut reference.
     * <p>
     * This method will discard any event that is broadcast to it from a FiducialArea that is not the referring source
     * 
     * @param event The Event object encapsulating the change form the last SigmaNaut Reference to the current one.
     * @see #refreshDependentObjects() 
     * @see #setRefSigmaNaut(double, double) 
     * @see #FiducialArea(ij.ImagePlus, ij.gui.Roi, edu.hope.superresolution.models.FiducialArea) 
     */
    public void onSigmaNautRefresh(RefSigmaNautRefreshEvent event) {
        
        if( event.sameOriginator(sourceDependentFArea_) ) {
            RefSigmaNautChangeEvent evt = event.getEvent();
            setRefSigmaNaut(evt.curRefSigmaNautValue_, evt.curRefSigmaNautUncertainty_);
            //Dispatch the event Generated by setRefSigmaNaut
            refreshDependentObjects();
        }
        
    }
    
    /**
     * For Extending classes, adds a RefreshEvent to the eventQueue that is dispatched when refreshDependentObjects is 
     * called.
     * <p>
     * Note, call immediate events are meant to be dispatched through use of postEvent(evt), and are recommended not to be RefreshEvents
     * 
     * @param refreshEvent - The Specific RefreshEvent generated
     * @see #refreshDependentObjects() 
     * @see RefreshEvent
     */
    protected void addToRefreshQueue( RefreshEvent<FiducialArea, MasterSpecificEvent> refreshEvent ) {
       postToEventQueue( refreshDependentQueueHandle_, refreshEvent );  
    }
    
    //Local Callback for updating the BoundedSpotList when a Gaussian Fit is performed
    //Runs of opposite Thread
    public class BoundedSpotListAction implements ListCallback<SpotData> {

        private final FiducialArea refInstance_;
        
        public BoundedSpotListAction( FiducialArea refInstance ) {
            refInstance_ = refInstance;
        }
        
        /**
         * Callback Method for when All potential spots have been determined for the search region.
         * 
         * @hope.superresoltion.genevent {@link SpotSearchRepopulatedEvent}
         * @hope.superresolution.dispevent Dispatches {@link SpotSearchRepopulatedEvent} and any events reported by populateBoundedSpotList
         * @param list 
         */
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
          //And dispatch any pending Events
          postToEventQueue(immediateProcessEndQueueHandle_, new SpotSearchRepopulatedEvent( refInstance_ ) );
          dispatchEventQueue(immediateProcessEndQueueHandle_);
          
               
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

