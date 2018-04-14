/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import com.google.common.eventbus.Subscribe;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.Utils.AdditionalGaussianUtils;
import edu.hope.superresolution.Utils.CopyUtils;
import edu.hope.superresolution.Utils.IJMMReportingUtils;
import edu.hope.superresolution.exceptions.NoFiducialException;
import edu.hope.superresolution.exceptions.NoTrackException;
import edu.hope.superresolution.genericstructures.FiducialTravelDiff2D;
import edu.hope.superresolution.genericstructures.TravelMatchCase;
import edu.hope.superresolution.genericstructures.iDriftModel;
import edu.hope.superresolution.imagetrack.FiducialMoveFinder;
import edu.hope.superresolution.processors.FiducialAreaProcessor;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.exception.OutOfRangeException;

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
    /**
     * {@link ModelUpdateDispatcher} Flag - When the data regarding a fiducialArea in the FiducialLocation Model has changed (inconclusive on whether selected or other)
     */
    public static final int EVENT_FIDUCIAL_SELECTION_CHANGED = 6;
        /**
     * {@link ModelUpdateDispatcher} Flag - When the data regarding a fiducialArea in the FiducialLocation Model has changed (inconclusive on whether selected or other)
     */
    public static final int EVENT_FIDUCIAL_AREA_DATA_CHANGED = 7;
    public static final int EVENT_FIDUCIAL_REGION_CHANGED = 8;
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
    private final int acquisitionTrackNumber_;
    private int minNumFiducialsForTrack_ = 3; //Used When Pushing next Location Model From a Track
    //private double avgRelXPixelTranslate_ = 0;
    //private double avgRelYPixelTranslate_ = 0;
    //private double avgAbsoluteXPixelTranslation_ = 0;
    //private double avgAbsoluteYPixelTranslation_ = 0;
    //Negative Standard Deviations indicate a non-useful value
    //private double avgRelXPixelTranslateStdDev_ = -1;
    //private double avgRelYPixelTranslateStdDev_ = -1;
    
    //Drift Models for use with tracking how far the Fiducial Model has traveled
    private int validStatisticalDriftParticles_ = 0; //Used to store the number of samples used in Drift (excluding virtual placeholders)
    private iDriftModel absoluteDriftInfo_;
    private iDriftModel relativeToLastModelDriftInfo_;
    
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
        
        acquisitionTrackNumber_ = 0;
        acquisitionTitle_ = acquisitionTitle;
        absoluteDriftInfo_ = new LinearDriftModel2D(acquisitionTrackNumber_,0,0,0,0, iDriftModel.DriftUnits.nm);
        relativeToLastModelDriftInfo_ = new LinearDriftModel2D(acquisitionTrackNumber_,0,0,0,0, iDriftModel.DriftUnits.nm);
        
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
        this(imgWin.getImagePlus(), fAreaProcessor, acquisitionTitle);
       /* acquisitionTrackNumber_ = 0;
        acquisitionTitle_ = acquisitionTitle;
        
        ip_ = imgWin.getImagePlus();
        
        try {
            fAreaProcessor_ = CopyUtils.abstractCopy( fAreaProcessor );
        } catch ( Exception ex ) {
            IJMMReportingUtils.logError(ex, "Failure to Copy Processor in New FiducialLocationModel");
            throw new RuntimeException( ex );  //Allow Micromanager to respond Appropriately
        }*/
    }
    
    //Copy Constructor, with new ImagePlus

    /**
     *  Modifed Copy Constructor 
     *   (FiducialModel characteristics as basis)
     * <p>
     * This will apply an imagePlus as the reference imagePlus to the resulting Object. All non-tracked information will
     * be populated according to the fiducialLocationModel.  This means that FiducialAreas will be copied and applied to the current 
     * ImagePlus processor.  The Order of application for FiducialArea region is 1. {@link FiducialArea#getTrackSearchArea()} if &gt; 0 2. {@link FiducialArea#getSelectionArea()} otherwise.
     * <p>
     * Note, the selected spot in the "copied" FiducialArea is not guaranteed to be the same since the ability for
     * change across images guarantees nothing is permanent.  This means this constructor is NOT a track operation.
     * Please use {@link #createTrackedFiducialLocationModel(ij.ImagePlus, edu.hope.superresolution.models.FiducialLocationModel, java.lang.String, int)} if
     * looking for a track. 
     *
     * 
     * @param ip - New ImagePlus
     * @param fLocationModel - Location Model to copy and track from on new ImagePlus
     * @param acquisitionTitle - The title of the LocationAcquisitionModel that is producing a track
     * @param aquisitionTrackNumber - The number of the track relative to the "commissioning" locationAcquistion
     *                                  Note:  Depending on how the LocationAcquisition handles non-tracked FiducialLocationModels,
     *                                          it may discard the location model, so this is only meant as a sequential reference to 
     *                                          when a FiducialLocationModel was commissioned.
     */
    private FiducialLocationModel( ImagePlus ip, FiducialLocationModel fLocationModel, 
                                        String acquisitionTitle, int aquisitionTrackNumber ) {
        
        acquisitionTitle_ = acquisitionTitle;
        acquisitionTrackNumber_ = aquisitionTrackNumber;
        //Since Drift is indeterminate, reset it
        absoluteDriftInfo_ = new LinearDriftModel2D(acquisitionTrackNumber_,0,0,0,0, iDriftModel.DriftUnits.nm);
        relativeToLastModelDriftInfo_ = new LinearDriftModel2D(acquisitionTrackNumber_,0,0,0,0, iDriftModel.DriftUnits.nm);
        
        ip_ = ip;
        minNumFiducialsForTrack_ = fLocationModel.minNumFiducialsForTrack_;
        //Copy subClass of fiducialAreaProcessor for thread-safety
        try {
            fAreaProcessor_ = CopyUtils.abstractCopy( fLocationModel.fAreaProcessor_ );
        } catch ( Exception ex ) {
            IJMMReportingUtils.logError(ex, "Failure to Copy Processor in New FiducialLocationModel");
            throw new RuntimeException( ex );  //Allow Micromanager to respond Appropriately
        }
        
        //Copy the Fiducial Area Rois and Apply them to the ImagePlus for this Instance
        for( FiducialArea fArea : fLocationModel.fiducialAreaList_ ) {
            //This a check for a loss of threshold or spot
           //if ( fArea.getRawSelectedSpot() != null ) {
                //ensure the fAreas aren't asynchronously processing so we wait
                boolean wasAsync = fArea.getFiducialAreaProcessor().isAsyncProcessEnabled();
                try {
                if( wasAsync ) {
                    fArea.getFiducialAreaProcessor().enableAsyncProcessing(false);
                }
                Roi trackArea = fArea.getTrackSearchArea();
                //This can produce NoFiducial Results, however, the spotMatchThreads account for this
                fiducialAreaList_.add( FiducialArea.createLinkedFiducialArea(ip_, trackArea, fArea));
                }
                finally {
                    if( wasAsync ) {
                        fArea.getFiducialAreaProcessor().enableAsyncProcessing(true);
                    }
                }
            /*} else {
                ReportingUtils.showError( "There was a null selected Spot");
            }*/
        }
        
        /*int maxMissingFrames = 15;
        fMoveFinder_ = new FiducialMoveFinder( maxMissingFrames, minNumFiducialsForTrack_,
                                                acquisitionTitle_ );
        TravelMatchCase mCase = fMoveFinder_.CorrelateTrackSelectedParticles(fLocationModel.fiducialAreaList_, ip);
        if( mCase == null ) {
            throw new NoFiducialException();
        }
        
        ApplySelectedMatchCase( mCase );
        //Since avg translation Values Calculated by Selecteed MatchCase, use source model for absolute
        avgAbsoluteXPixelTranslation_ = fLocationModel.avgAbsoluteXPixelTranslation_ + avgRelXPixelTranslate_;
        avgAbsoluteYPixelTranslation_ = fLocationModel.avgAbsoluteYPixelTranslation_ + avgRelYPixelTranslate_;*/
        
        //For consistency, copy currentSelected FiducialArea too
        selectedAreaIndex_ = fLocationModel.selectedAreaIndex_;
        selectedFiducialArea_ = fiducialAreaList_.get( selectedAreaIndex_ );

    }
    
    /**
     * Static method for creating a FiducialLocation Model that results from tracking Fiducials from another Fiducial Location Model onto
     * the current imageProcessor of the ImagePlus.
     * <p>
     * This requires the explicit specification of the locationAcquisitionTitle that is calling the the model.
     * 
     * @param ip The ImageProcessor to which the resulting FiducialLocationModel will refer
     * @param fLocationModel The FiducialLocationModel that will have its processor copied, settings, and selectedFiducialArea {@link #FiducialLocationModel(ij.ImagePlus, edu.hope.superresolution.models.FiducialLocationModel, java.lang.String, int) }
     * @param acquisitionTitle The title of the LocationAcquisition that this track is being created for
     * @param acquisitionTrackNumber  The number of the track relative to the "commissioning" locationAcquistion
     *                                  Note:  Depending on how the LocationAcquisition handles non-tracked FiducialLocationModels,
     *                                          it may discard the location model, so this is only meant as a sequential reference to 
     *                                          when a FiducialLocationModel was commissioned.
     * @return The FiducialLocationModel Reference that resulted from a good track
     * @throws NoTrackException If there was no correlatable track.  Any FiducialAreas will be reported back as NoFiducialExceptions
     *                          if they had no potential spots in the original search area for each FiducialArea (tracksearchArea)
     *                          there will be no information about expanded expanded search areas that the fiducialmove finder may 
     *                          have also iterated through other than the fact that those iterations yielded no track as well.
     */
    public static FiducialLocationModel createTrackedFiducialLocationModel( ImagePlus ip, FiducialLocationModel fLocationModel,
                                        String acquisitionTitle, int acquisitionTrackNumber ) throws NoTrackException {
        
        FiducialLocationModel baseFModel = new FiducialLocationModel( ip, fLocationModel, acquisitionTitle, acquisitionTrackNumber );
        int maxMissingFrames = 15;
        FiducialMoveFinder fMoveFinder = new FiducialMoveFinder( maxMissingFrames, fLocationModel.minNumFiducialsForTrack_,
                                                acquisitionTitle );
        TravelMatchCase mCase = fMoveFinder.CorrelateTrackSelectedParticles(fLocationModel.fiducialAreaList_, baseFModel.fiducialAreaList_);
        
        //If there is no match case, then we should alert that track is not available
        if( mCase == null ) {
            int max = baseFModel.fiducialAreaList_.size();
            List<NoFiducialException> noFids = new ArrayList<NoFiducialException>(max);
            for( int i = 0; i < max; ++i) {
                FiducialArea fArea = baseFModel.fiducialAreaList_.get(i);
                if(fArea.getRawSelectedSpot() == null) {
                    noFids.add( new NoFiducialException("Failed to Find a fiducial", fArea.getSelectionArea(), i) );
                }
            }
            throw new NoTrackException( "Unable to Track", noFids.toArray(new NoFiducialException[noFids.size()]));
        }
        
        baseFModel.ApplySelectedMatchCase( mCase, fLocationModel );
        
        return baseFModel;
    }
    
    
    
    
    /**
     * 
     * Static method for creating a FiducialLocation Model that results from tracking Fiducials from another Fiducial Location Model onto
     * the current imageProcessor.
     * <p>
     * This requires the explicit specification of the locationAcquisitionTitle that is calling the the model.
     * 
     * @param iProc The ImageProcessor to which the resulting FiducialLocationModel will refer
     * @param fLocationModel The FiducialLocationModel that will have its processor copied, settings, and selectedFiducialArea {@link #FiducialLocationModel(ij.ImagePlus, edu.hope.superresolution.models.FiducialLocationModel, java.lang.String, int) }
     * @param acquisitionTitle The title of the LocationAcquisition that this track is being created for
     * @param acquisitionTrackNumber  The number of the track relative to the "commissioning" locationAcquistion
     *                                  Note:  Depending on how the LocationAcquisition handles non-tracked FiducialLocationModels,
     *                                          it may discard the location model, so this is only meant as a sequential reference to 
     *                                          when a FiducialLocationModel was commissioned.
     * @return The FiducialLocationModel Reference that resulted from a good track
     * @throws NoTrackException If there was no correlatable track.  Any FiducialAreas will be reported back as NoFiducialExceptions
     *                          if they had no potential spots in the original search area for each FiducialArea (tracksearchArea)
     *                          there will be no information about expanded expanded search areas that the fiducialmove finder may 
     *                          have also iterated through other than the fact that those iterations yielded no track as well.
     */
    public static FiducialLocationModel createTrackedFiducialLocationModel( ImageProcessor iProc, FiducialLocationModel fLocationModel,
                                            String acquisitionTitle, int acquisitionTrackNumber ) throws NoTrackException {
        return createTrackedFiducialLocationModel( new ImagePlus("Track Processed Num:" + (acquisitionTrackNumber + 1), iProc), fLocationModel,
                                                    acquisitionTitle, acquisitionTrackNumber);
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
    /*public FiducialLocationModel( ImageProcessor iProc, FiducialLocationModel fLocationModel,
                                            String acquisitionTitle, int acquisitionTrackNumber ) throws NoFiducialException {
        this( new ImagePlus("Track Processed Num:" + (acquisitionTrackNumber + 1), iProc), fLocationModel, acquisitionTitle, acquisitionTrackNumber );
    }*/
    

    /**
     * Apply a Given Match Case (Returned from fMoveFinder_) to FiducialAreaList_
     *     Adds Listeners and calculates Average Translation of the Match Case.
     * 
     * @param mCase - The Match case for all Fiducial Areas With Best matches above
     *                the minimum requirements
     * @param basis The FiducialLocationModel that was used for such a travelmatch case.
     *              This basis is specifically used for its computation of absolute drift
     *              since the basis FiducialLocationModel has been tracking from the genesis by its
     *              absolutedrift model.
     */
    private void ApplySelectedMatchCase( TravelMatchCase mCase, FiducialLocationModel basis) {
            
        List< FiducialArea > tempList = new ArrayList< FiducialArea >();
        
        double avgXTranslate = 0;
        double avgYTranslate = 0;
        double avgXUncertainty = 0;
        double avgYUncertainty = 0;
        int numReliable = 0;
        FiducialArea fArea;
        //Apply Selected Spots to the FiducialAreas and Fiducial Areas to the List
        for( FiducialTravelDiff2D diff : mCase.getFiducialTranslationSpots() ) {
            fArea = diff.areaOwnerRef_;
            //Only add differences with non-virtual members
            if( !diff.toVirtual_ && !diff.fromVirtual_) {
                ++numReliable;
                avgXTranslate += diff.xDiffs_;
                avgYTranslate += diff.yDiffs_;
                avgXUncertainty += Math.pow( diff.xUncertainty_,2);
                avgYUncertainty += Math.pow( diff.yUncertainty_, 2);
            }
            //Add Change Observer so that the area will register
            //This is way too soon for this.  since we haven't even add the fArea...
            fArea.RegisterToStateEventBus(fAreaCallBack_);
            //Actually set the result's selected spot
            try {
                //fArea.setSelectedSpotRaw( diff.spotRef_ );
                fArea.applyTravelDifferenceAsDrift(diff, acquisitionTrackNumber_);
            }
            catch ( Exception ex ) {
                throw new RuntimeException( ex );
            }
            tempList.add( fArea );
        }
        
        //Calculate the average Translation from real to real points
        avgXTranslate /= numReliable;
        avgYTranslate /= numReliable;
        avgXUncertainty = Math.sqrt(avgXUncertainty)/numReliable;
        avgYUncertainty = Math.sqrt(avgYUncertainty)/numReliable;
        
        //Calculate Standard Deviation of those translations (corrected)
        double avgRelXPixelTranslateStdDev = 0;
        double avgRelYPixelTranslateStdDev = 0;
        //Apply Selected Spots to the FiducialAreas and Fiducial Areas to the List
        for( FiducialTravelDiff2D diff : mCase.getFiducialTranslationSpots() ) {
            //We Can Really only Calculate Standard Deviation from the set of real to real reliably
            if( !diff.toVirtual_ && !diff.fromVirtual_ ) {
                avgRelXPixelTranslateStdDev += Math.pow((diff.xDiffs_ - avgXTranslate), 2);
                avgRelYPixelTranslateStdDev += Math.pow((diff.yDiffs_ - avgYTranslate), 2);
            }              
        }
        
        double totalXUncertainty = 0;
        double totalYUncertainty = 0;
        
        if( numReliable > 1 ) {
            avgRelXPixelTranslateStdDev = Math.sqrt(avgRelXPixelTranslateStdDev/(numReliable - 1));
            avgRelYPixelTranslateStdDev = Math.sqrt(avgRelYPixelTranslateStdDev/(numReliable - 1));
            totalXUncertainty = computeCIUncertainty(avgRelXPixelTranslateStdDev, numReliable);
            totalYUncertainty = computeCIUncertainty(avgRelYPixelTranslateStdDev, numReliable);
        } else{
            //We'll use zero for the sake of adding other uncertainties
            avgRelXPixelTranslateStdDev = 0;
            avgRelYPixelTranslateStdDev = 0;
        }
        
        //Store the number of particles that were used
        validStatisticalDriftParticles_ = numReliable;
        
        //add the uncertainty to the CI from above
        totalXUncertainty += avgXUncertainty;
        totalYUncertainty += avgYUncertainty;
        
        //Create Relative and absolute Drift Info
        relativeToLastModelDriftInfo_ = new LinearDriftModel2D(acquisitionTrackNumber_, avgXTranslate, totalXUncertainty, avgYTranslate, totalYUncertainty, iDriftModel.DriftUnits.nm);
        absoluteDriftInfo_ = new LinearDriftModel2D( acquisitionTrackNumber_, avgXTranslate + basis.absoluteDriftInfo_.getXFromStartTranslation(),
                                                       Math.sqrt(Math.pow(totalXUncertainty, 2) + Math.pow(basis.absoluteDriftInfo_.getXTranslationUncertainty(), 2)),
                                                        avgYTranslate + basis.absoluteDriftInfo_.getYFromStartTranslation(),
                                                        Math.sqrt(Math.pow(totalYUncertainty, 2) + Math.pow(basis.absoluteDriftInfo_.getYTranslationUncertainty(), 2)),
                                                        basis.absoluteDriftInfo_.getDriftUnits() );
        
        //Update the List in case of references
        fiducialAreaList_.clear();
        fiducialAreaList_.addAll(tempList);
        
        //TODO: We need to dispatch an event that is more meaningful
        
    }
    
    /**
     * This function should be used to compute the resulting uncertainty as dictated by the specified confidence interval for a
     * set of data that behaves with an anticipated normal distribution with a given standard deviation.
     * 
     * 
     * @return 
     */
    private double computeCIUncertainty( double stdDev, int numSamples ) {
        
        if( numSamples < 2 ) {
            throw new OutOfRangeException( numSamples, 2, Double.POSITIVE_INFINITY);
        }
        
        //Currently HardCoded
        return AdditionalGaussianUtils.produceZScoreFromCI(.95)*stdDev/Math.sqrt(numSamples);
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
     * @param iproc - the new imageProcessor
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
     */
    public int getMinNumFiducialTracks( ) {
        return minNumFiducialsForTrack_;
    }
    
    /**
     * Sets All the Current Fiducials to refSigmaNaut_ so that they are now the relative measure
     * for z score in changes in the psf.
     */
    public void setFocusPlaneToCurrentFiducials() {
        
        double avgDiffs = 0, avgUncertaintyDiffs = 0;
        int numReal = 0;
        //Store the Real Spots Widths First
        for( int i = 0; i< fiducialAreaList_.size(); ++i ) {
            FiducialArea fArea = fiducialAreaList_.get(i);
            BoundedSpotData spot = fArea.getRawSelectedSpot();
            if( spot!= null && !spot.isVirtual() ) {
                avgDiffs += spot.getWidth()/4 - fArea.getFocusPlaneSigmaRef();
                avgUncertaintyDiffs = spot.getSigma() - fArea.getFocusPlaneSigmaRefUncertainty();
                fArea.setRefSigmaNaut( spot.getWidth()/4, spot.getSigma() );
                numReal++;
            }
        }
        
        avgDiffs /= numReal;
        
        //For Virtuals, reiterate with the average change applied to their current refSigmaNauts
        for( int i = 0; i< fiducialAreaList_.size(); ++i ) {
            FiducialArea fArea = fiducialAreaList_.get(i);
            BoundedSpotData spot = fArea.getRawSelectedSpot();
            if( spot!= null && spot.isVirtual() ) {
                //Guard against some strange uncertainty being produce 
                assert(avgUncertaintyDiffs + fArea.getFocusPlaneSigmaRefUncertainty() >= 0);
                fArea.setRefSigmaNaut( fArea.getFocusPlaneSigmaRef()+ avgDiffs, fArea.getFocusPlaneSigmaRefUncertainty() + avgUncertaintyDiffs );
            }
        }
        
        return;
    }
    
    /**
     * Gets the Drift Information for this FiducialLocationModel instance relative to the 
     * fiducialLocationModel used to track it.
     * <p>
     * Note:  The relativity of this model is context dependent, based off of the FiducialLocationModel used to create the 
     * track and thus the drift associate with it {@link #createTrackedFiducialLocationModel(ij.ImagePlus, edu.hope.superresolution.models.FiducialLocationModel, java.lang.String, int) }
     * <p>
     * Note2: The way the FiducialLocationModel determines drift is due to the average of tracked translation between its fiducials
     * and a previous set.  Unfortunately, due to the ability for some tracks to be the result of approximate values (i.e. virtually 
     * assigned spots) that may have blinked, fiducials that are currently translating from virtual or to a virtual are not statistically significant, 
     * since they are assigned the average of the set.  Thus, there may be an outcome, in which there is no standard deviation
     * due to the set of only 1 real to real fiducial particle.  {@link #getNumDriftContributingParticles()} can be used to indicate
     * the number of fiducial particles that were available in this calculation.
     * 
     * @return 
     */
    public iDriftModel getDriftRelativeToBaseFiducialModel() {
        return relativeToLastModelDriftInfo_;
    }
    
    /**
     * Gets the sample size used in calculated the Drift of this FiducialArea relative to the last area.
     * <p>
     * This accounts for the ability for a blinking fiducial particle to be assumed inside of the average
     * of the rest of the fiducial set translation, but not to be statistically signficant because it is
     * the result of a virtual operation.
     * <p>
     * Should be used in conjunction with {@link #getDriftAbsoluteFromFirstModel()} and {@link #getDriftRelativeToBaseFiducialModel() } 
     * if statistical relevance is to be confirmed.  There are some edge case in which statistical relevance is not nearly as important,
     * (e.g. when aligning slices approximately based off of nearest pixels).
     * 
     * @return   
     */
    public int getNumDriftContributingParticles() {
        return validStatisticalDriftParticles_;
    }
    
    /**
     * Gets the Drift Information for this FiducialLocationModel instance relative to the 
     * first instance that started tracking fiducialLocation Models.
     * <p>
     * Note:  This Model may generate different uncertainties in difference contexts.
     * For Instance, if we are tracking relative to the previous FiducialLocationModel, then
     * the absolute track will have a high uncertainty due to the combination of uncertainties for each
     * FiducialLocationModel tracked previous to the current one, being added up.  This is a possibility for the sake of
     * not finding multiple tracks between frames in a real-time situation.
     * <p>
     * Note2: The way the FiducialLocationModel determines drift is due to the average of tracked translation between its fiducials
     * and a previous set.  Unfortunately, due to the ability for some tracks to be the result of approximate values (i.e. virtually 
     * assigned spots that may have blinked), fiducials that are currently translating from virtual or to a virtual are not statistically significant, 
     * since they are assigned the average of the set.  Thus, there may be an outcome, in which there is no standard deviation
     * due to the set of only 1 real to real fiducial particle.  {@link #getNumDriftContributingParticles()} can be used to indicate
     * the number of fiducial particles that were available in this calculation.
     * 
     * @hope.superresolution.todo Create a way to more reliably produce an absolute Value internally, or correct such
     * 
     * @return The DriftModel containing translation information from the first FiducialLocationModel to the current location Model
     */
    public iDriftModel getDriftAbsoluteFromFirstModel() {
        return absoluteDriftInfo_;
    }
    
    /**
     * Gets a Absolute Drift Model Object {@link #getDriftAbsoluteFromFirstModel() } with units of pixels according to the FiducialProcessor.
     * <p>
     * Note, the drift units are still fractional pixel units
     * 
     * @return 
     */
    public iDriftModel getPixelSizeAbsoluteDriftModel() {
        return absoluteDriftInfo_.generatePixelConversion( (int) fAreaProcessor_.getPixelSize() , iDriftModel.DriftUnits.nm);
    }
    
     /**
     * Gets a Relative Drift Model Object {@link #getDriftRelativeToBaseFiducialModel() } with units of pixels according to the FiducialProcessor.
     * <p>
     * Note, the drift units are still fractional pixel units
     * 
     * @return 
     */
    public iDriftModel getPixelSizeRelativeDriftModel() {
        return relativeToLastModelDriftInfo_.generatePixelConversion( (int) fAreaProcessor_.getPixelSize() , iDriftModel.DriftUnits.nm);
    }
    
    /**
     * Get the Average X Translation from the last Fiducial Model to this one

     * @return 
     */
    /*public double getAvgRelXPixelTranslation() {
        return avgRelXPixelTranslate_;
    }*/

    /**
     * Get the Standard Deviation of the translations used to produce the Average Relative X Pixel Translation
     * 
     * This deviation accounts for datum that are not calculated from Virtual spots or to Virtual Spots
     * 
     * @return 
     */
    /*public double getAvgRelXPixelTranslationStdDev() {
        return avgRelXPixelTranslateStdDev_;
    }*/
    
    /**
     *  Get the Average X Translation from the last Fiducial Model to this one 
     * @return 
     */
    /*public double getAvgRelYPixelTranslation() {
        return avgRelYPixelTranslate_;
    }*/
    
    /**
     * Get the Standard Deviation of the translations used to produce the Average Relative Y Pixel Translation
     * 
     * This deviation accounts for datum that are not calculated from Virtual spots or to Virtual Spots
     * 
     * @return 
     */
    /*public double getAvgRelYPixelTranslationStdDev() {
        return avgRelYPixelTranslateStdDev_;
    }*/
    
    /**
     *  Gets the Absolute X Translation Relative to the First Fiducial Location Model
     *   used to start Track Copy Constructors.
     *   This Value will only be non-zero when a FiducialLocationModel is the result 
     *   of a Track Copy Constructor.
     * 
     * @return the Value in Image coordinates (Pixels) of X Translation
     */
    /*public double getAvgAbsoluteXPixelTranslation() {
        return avgAbsoluteXPixelTranslation_;
    }*/
    
        /**
     *  Gets the Absolute Y Translation Relative to the First Fiducial Location Model
     *   used to start Track Copy Constructors.
     *   This Value will only be non-zero when a FiducialLocationModel is the result 
     *   of a Track Copy Constructor.
     * 
     * @return the Value in Image coordinates (Pixels) of Y Translation
     */
    /*public double getAvgAbsoluteYPixelTranslation() {
        return avgAbsoluteYPixelTranslation_;
    }*/
    
    /**
     * FiducialArea State Listener Callback Class
     * <p>
     * Implemented as inner class to avoid constructor initialization issues that may arise with registration
     */
    private class FiducialAreaCallback {

        /**
         * Callback for handling when a FiducialArea's Selected Fiducial Has Changed
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
            dispatch(EVENT_FIDUCIAL_SELECTION_CHANGED);
        }
        
        /**
         * Callback for handling when a FiducialArea's Search Region Has
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
            dispatch(EVENT_FIDUCIAL_REGION_CHANGED);
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
        public void onPossibleFiducialsChanged(FiducialArea.SpotSearchRepopulatedEvent evt) {
            //Should be reworked, but currently just call update for listeners
            dispatch(EVENT_FIDUCIAL_AREA_DATA_CHANGED);
        }
    }
}
