/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.imagetrack;

import com.google.common.util.concurrent.AtomicDouble;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.genericstructures.FiducialTravelDiff2D;
import edu.hope.superresolution.genericstructures.TravelMatchCase;
import edu.hope.superresolution.models.FiducialArea;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Color;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  Thread For Matching spots in a corresponding set of Given Areas.  Accesses a blocking Queue 
 *  of Fiducial Area Sets that is meant to allow for multiple shifted FiducialArea scans
 *   and compares each FiducialArea's spots to the previous SelectedSpot from the 
 *   previous FiducialArea.  Attempts to create a match with the most number of matches
 *   and monitors the ratio of matches across other threads through an atomic ratio.
 *   Emphasis is put on real matches and their ratios, over matches that may have 
 *    come from vitrually anticipated spots.  Those spots are seen as extra data.
 * <p> 
 *  TODO: Evaluate More Correlative Methods For weeding out Fits.
 * 
 * @author Microscope
 */
public class SpotMatchThread implements Runnable {

    private String acquisitionTitle_; //Used For Displays Correspondings to the owning Acquisition
    
    static int runNumber = 0;
    int curRun = 0;
    
    //Storage Class for FAreas without Data, to create "symbolic" spots
    private class NoSpotFAreaData {
        
        public BoundedSpotData prevSpot_;
        public FiducialArea curFArea_;
        
        public NoSpotFAreaData( BoundedSpotData prevSpot, FiducialArea curFArea ) {
            prevSpot_ = prevSpot;
            curFArea_ = curFArea;
        }
        
    }
    
    
    private final List< FiducialArea > prevFAreas_;   
    private final BlockingQueue< List< FiducialArea > > shiftedFAreasList_;
    private final double diminishFraction_;
    private final double failLimitRatio_;
    private final int maxMissingFrames_;
    private final int totalNumAreas_;
    private List< NoSpotFAreaData > noSpotFAreas_ = new ArrayList< NoSpotFAreaData >();  //Used to store a Fiducial Area with no more correlations
    private final List< NoSpotFAreaData > noSpotFAreasBuffer_ = new ArrayList<NoSpotFAreaData>(); //Used to store Buffered No Spots
    private final AtomicBoolean endKeyPlacedIndicator_;
    
    private Thread thread_ = null;
    private boolean isRunning_ = false;
    private final Object runCheckLock_ = new Object();
    private boolean stopLoop_ = false;
    private final Object stopLoopLock_ = new Object();
    
    //AtomicInteger for comparison and determination of match validity
    private final AtomicDouble curFromRealMatchCaseRatio_;
    private final AtomicDouble curFromVirtualMatchCaseRatio_;
    private final List< TravelMatchCase > matchResults_;  //Synchronized List Reference

    /**
     *  General Constructor
     * 
     * @param prevFAreas - The Previous Fiducial Areas
     * @param shiftedFAreasList - The BlockingQueue of Fiducial Areas (shifted 
     *                             or same region) to compare for translational similarities
     * @param matchResults - A synchronized List to added TravelMatchCases to
     * @param percent - The Percentage of anticipated Intensity Diminish in the 
     *                   Selected Spot to establish a filtering threshold
     * @param failLimitRatio - The minimum ratio of actual match Areas/overall 
     *                          Areas for a match to be registered
     * @param maxMissingFrames - The maximum number of frames a Fiducial Area has 
     *                             a virtual Spot before being disregarded in tracks
     * @param endKeyPlacedIndicator - An Atomic Boolean that indicates is the 
     *                                  endKey was successfully replaced at the end of the Queue.
     *                                  (Determines if Thread Interruptions are necessary)
     * @param curMatchCaseRatio - An Atomic Double That keeps track of the current Match Case Ratio
     *                              ( A Real Spot to a Real Spot track) so that Matches with less may 
     *                              be discarded in search for maximal match ratio.
     * @param curFromVirtualMatchCaseRatio - An Atomic Double that keeps track of the current match case ratio
     *                                         (from a Virtual Spot to a Real Spot track).  This is used
     *                                          as secondary validation in competing real match cases currently.
     * @param acquisitionTitle  - The Title Used For Graphical Displays that pertains to the Acquisition calling this class
     * @param acquisitionSaveSpace - The Space in which Images Can Be showed
     */
    SpotMatchThread( List< FiducialArea > prevFAreas, BlockingQueue< List<FiducialArea> > shiftedFAreasList,
                    List< TravelMatchCase > matchResults, double percent, double failLimitRatio, int maxMissingFrames,
                    AtomicBoolean endKeyPlacedIndicator, AtomicDouble curMatchCaseRatio, 
                    AtomicDouble curFromVirtualMatchCaseRatio, String acquisitionTitle ) {
        
        acquisitionTitle_ = acquisitionTitle;
        prevFAreas_ = prevFAreas;
        diminishFraction_ = percent;
        failLimitRatio_ = failLimitRatio;
        shiftedFAreasList_ = shiftedFAreasList;
        maxMissingFrames_ = maxMissingFrames;
        
        endKeyPlacedIndicator_ = endKeyPlacedIndicator;
        
        //Result Storage Possibilities
        curFromRealMatchCaseRatio_ = curMatchCaseRatio;
        curFromVirtualMatchCaseRatio_ = curFromVirtualMatchCaseRatio;
        matchResults_ = matchResults;
        
        //Assumes The CurFAreas is the same size
        totalNumAreas_ = prevFAreas_.size();
    }
    
    public boolean init() {
        synchronized( runCheckLock_ ) {
            if( isRunning_ ) {
                return false;
            }
            thread_ = new Thread( this );
            isRunning_ = true;
            setStop( false );
            thread_.start();
        }
        
        return true;
    }
        
    /**
     * Stops the processes, without waiting on BlockingQueues
     * 
     * @return <code>true</code>If the thread was running and then was stopped
     */
    public boolean stopImmediate() {
        
        synchronized( runCheckLock_ ) {
            if( !isRunning_ ) {
                return false;
            }
            
            setStop(true);
            //To interrupt Queue Waiting
            thread_.interrupt();

        }
        
        return true;
    }
    
    /**
     * Stops the Loop While Allowing the Thread to Wait for the EndKey/Next Entry 
     * to process on the BlockingQueue.
     * 
     * @return <code>true</code> if the thread was running when this was called
     */
    public boolean stopLoop() {
        
        synchronized( runCheckLock_ ) {
            if( !isRunning_ ) {
                return false;
            }
            
            setStop(true);
            
        }
        
        return true;
    }
    
    public boolean join() {
        if (thread_ == null) {
            return false;
        }
        
        try {
            thread_.join();
            
        } catch ( InterruptedException ex ) {
            ij.IJ.log("Thread join interruped and stop called on " + Thread.currentThread().getName());
            stopLoop();
        }
        return true;
    }
    
    private void setStop( boolean enableStop ) {
        synchronized( stopLoopLock_ ) {
            stopLoop_ = enableStop;
        }
    }
    
    private boolean getStop( ) {
        synchronized( stopLoopLock_ ) {
            return stopLoop_;
        }
    }
    
    @Override
    public void run() {
       
        //Find All Spots in curFArea_
        try {
          runNumber++;
          curRun = runNumber;
          correlateDifference();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            ij.IJ.log( ex.getMessage() );
        }
        
        synchronized( runCheckLock_ ) {
            isRunning_ = false;
        }
        
    }    
    
    /**
     *  Attempts to search out Fiducials based on an assumed frame decay from the 
     *   previous fiducial to the current frame.  The PrevFiducial is the fiducial 
     *   that was tracked from the last frame, the curFArea is the current Fiducial Area 
     *   in which potential translations are found, and the normalization ratio is in case
     *   an overall reduction or increase in the currentFiducial Area is detected.  DiminishFraction_
     *   is used to indicate the expected fractional decay from the previous number of photons.
     * <p>
     *   The Intensity Threshold is: prevFiducial Intensity * diminishFraction_ 
                                            * normalization Ratio - prevFiducial Intensity Uncertainty
     *   
     * @param prevFiducial - the previous fiducial from the previous correlated FiducialArea
     * @param curFArea - The current Fiducial Area in which potential travel differences will be evaluated
     * @param normalizationRatio - the ratio of curFiducialArea values to previous in case of whole area decrease
     * @return Returns a list of FiducialTravelDiff2D objects correlating to potential translation from the previous Fiducial
     * 
     * @see FiducialTravelDiff2D
     * @see #diminishFraction_
     */
    public List< FiducialTravelDiff2D > intensityPrimaryCorrelation( BoundedSpotData prevFiducial, FiducialArea curFArea, double normalizationRatio ) {
                
        List<BoundedSpotData> newOptions = curFArea.getAllRawPossibleSpots();
        
        List< FiducialTravelDiff2D > differences = new ArrayList< FiducialTravelDiff2D >();
        
        double dimIntensity = diminishFraction_ * normalizationRatio * prevFiducial.getIntensity() - prevFiducial.getNumPhotonUncertainty();
        
        for( BoundedSpotData spot : newOptions ) {
            
            if( spot.getIntensity() >= dimIntensity ) {
                differences.add( new FiducialTravelDiff2D( prevFiducial, spot, curFArea ));
            } else {
                //ij.IJ.log("Spot Exempted " + spot.getIntensity() + ":" + dimIntensity );                      
            }
            
        }
        
        return differences;
        
    }
    
    /**
     *  Attempts to search out Fiducials based on the assumed frame decay from the 
     *   maximum intensity spot in the the current frame.  The PrevFiducial is the fiducial 
     *   that was tracked from the last frame, the curFArea is the current Fiducial Area 
     *   in which potential translations are found, and the normalization ratio is in case
     *   an overall reduction or increase in the currentFiducial Area is detected.  DiminishFraction_
     *   is used to indicate the expected fractional decay from the previous number of photons.
     * <p>
     * Note: Different from IntensityPrimary Correlate, this guarantees correlated selection within
     * the diminishFraction_ of the maximum spot.  If there are not detected spots in the Fiducial Area
     * is the only time this method will return an empty list.
     * <p>
     *   The Intensity Threshold is: current Fiducial Area Max Intensity * diminishFraction_ 
                                            * normalization Ratio - prevFiducial Intensity Uncertainty
     *   
     * @param prevFiducial - the previous fiducial from the previous correlated FiducialArea (for differences)
     * @param curFArea - The current Fiducial Area in which potential travel differences will be evaluated
     * @param normalizationRatio - the ratio of curFiducialArea values to previous in case of whole area decrease
     * @return Returns a list of FiducialTravelDiff2D objects correlating to potential translation from the previous Fiducial
     * 
     * @see FiducialTravelDiff2D
     * @see #diminishFraction_
     */
    public List<FiducialTravelDiff2D> intensitySecondaryCorrelation( BoundedSpotData prevFiducial, FiducialArea curFArea, double normalizationRatio ) {
        List<BoundedSpotData> newOptions = curFArea.getAllRawPossibleSpots();
        
        List< FiducialTravelDiff2D > differences = new ArrayList< FiducialTravelDiff2D >();
        
        double dimIntensity = diminishFraction_ * normalizationRatio * curFArea.getMaxIntensity() - prevFiducial.getNumPhotonUncertainty();
        
        for( BoundedSpotData spot : newOptions ) {
            
            if( spot.getIntensity() >= dimIntensity ) {
                differences.add( new FiducialTravelDiff2D( prevFiducial, spot, curFArea ));
            }
            
        }
        
        return differences;
    }
    
    //Get all the differences
    //For Each Fiducial Area, Calculate the Differences, 
    //Take an arrayList of differences
    
    //Current Strategy for Correlation is that we expect existing (from Real) differences
    //  to persist, while blinked (from virtual elements are suspect to more chaotic appearances
    //  Therefore, if The Ability to Produce a Same FromRealMatch Ratio is compromised, 
    //  the current list is considered final, otherwise the list is modified for the largest fromVirtualMatch Ratio as well
    public void correlateDifference( ) {
        
        while ( !getStop() ) {

            //List of Diffs from a Real Starting Spot
            List< List< FiducialTravelDiff2D > > potentialRealDiffTravelSpots = 
                                                    new ArrayList< List< FiducialTravelDiff2D > >();
            //List of Diffs from a Virtual Starting Spot
            List< List< FiducialTravelDiff2D > > potentialVirtualDiffTravelSpots = 
                                                    new ArrayList< List< FiducialTravelDiff2D > >();
            List< FiducialArea> curFAreas;

            try {
                //Make sure that that an interrupt wasn't called between getStop() and now
                if( Thread.currentThread().isInterrupted() ) {
                    throw new InterruptedException();
                }
                curFAreas = shiftedFAreasList_.take();

                //emptySet is the indicator for thread to end
                if (curFAreas.isEmpty()) {
                    //Set This Atomic Variable For The Calling Thread to add/cancel other threads
                    endKeyPlacedIndicator_.set(shiftedFAreasList_.add(curFAreas));
                    return;
                }

            } catch (InterruptedException ex) {
                ij.IJ.log("Thread Queue Blocking interruped " + Thread.currentThread().getName());
                Thread.currentThread().interrupt(); //Pass Along for any Joins
                return;
            }

            assert (curFAreas.size() != prevFAreas_.size());

            //Generate A List of translations from possible Fiducials to last Fiducial
            //Could Be Changed to a Thread...?
            noSpotFAreas_.clear(); //Clear NoSpotFAreas List
            FiducialArea curFArea, prevFArea;
            double maxNormalizationRatio;
            int missingFromReal = 0, missingFromVirtual = 0;
            for (int i = 0; i < curFAreas.size(); i++) {
                prevFArea = prevFAreas_.get(i);
                curFArea = curFAreas.get(i);
                BoundedSpotData prevFiducial = prevFArea.getRawSelectedSpot();
                //Establish a cutoff for number of missing frames
                if ( prevFArea.getVirtualFrameTrackNumber() <= maxMissingFrames_ ) {
                    if( prevFArea.getMaxIntensity() == 0 ) {
                        //divide-by-zero guard
                        maxNormalizationRatio = 1;
                    } else {
                        maxNormalizationRatio = curFArea.getMaxIntensity()/prevFArea.getMaxIntensity();
                        //Correct to 1 for caution sake
                        maxNormalizationRatio = ( maxNormalizationRatio > 1 ) ? 1 : maxNormalizationRatio;
                    }
                    List< FiducialTravelDiff2D > spots = intensityPrimaryCorrelation(prevFiducial, curFArea, maxNormalizationRatio );
                    //Try to normalize to maximum Fiducial Area if there was no result (assuemd regional depletion)
                    if( spots.isEmpty() ) {
                        spots = intensitySecondaryCorrelation( prevFiducial, curFArea, maxNormalizationRatio );
                    }
                    //assume there are no suitable translation candidates
                    if (spots.isEmpty()) {
                        //For missing Fiducial Areas, determine if that counts toward fromReal
                        if( prevFiducial.isVirtual() ) {
                            ++missingFromVirtual;
                        }  else {
                           ++missingFromReal;
                        }
                        //add Fiducial Area to List for Virtual Spot Creation Later
                        noSpotFAreas_.add( new NoSpotFAreaData(prevFiducial, curFArea) );
                    } else {
                        if (prevFiducial.isVirtual()) {
                            potentialVirtualDiffTravelSpots.add(spots);
                        } else {
                            potentialRealDiffTravelSpots.add(spots);
                        }
                    }
                } else {
                    //In the event of max missing frames, establish Virtual Pattern to avoid extra searches
                    //add Fiducial Area to List for Virtual Spot Creation Later
                    noSpotFAreas_.add( new NoSpotFAreaData(prevFiducial, curFArea) );
                }
            }     
            
            //Display The Spots Found
            //Create New ImageWindow
            List<Roi> paintRois = new ArrayList();
            for( FiducialArea fArea : curFAreas ) {
                if (fArea.getTrackSearchArea() != null) {
                    paintRois.add((Roi) fArea.getTrackSearchArea().clone());
                    for (BoundedSpotData spot : fArea.getAllRawPossibleSpots()) {
                        if (spot.getBoundingBox() != null) {
                            paintRois.add((Roi) spot.getBoundingBox().clone());
                        }
                    }
                }
            }
            createWindow( curFAreas.get(0).getImagePlus().getProcessor(), paintRois );
                        
            //Store the starting number of FiducialAreas with Correlatable intensities
            int totalRealAreas = potentialRealDiffTravelSpots.size() + missingFromReal;
            int totalVirtualAreas = potentialVirtualDiffTravelSpots.size() + missingFromVirtual;
           
            //Check in case the potential Spots can match any current match ratios
            //Update Number of empty FiducialAreas
            double remainingRealAreasRatio;
            double remainingVirtualAreasRatio;
            if (totalRealAreas == 0) {
                remainingRealAreasRatio = 0;
            } else {
                remainingRealAreasRatio = ((double) (potentialRealDiffTravelSpots.size() )) / totalRealAreas;
            }
            if (totalVirtualAreas == 0) {
                //To Account For Virtual Areas Being Fulfilled and the way their stored
                remainingVirtualAreasRatio = 1;
            } else {
                remainingVirtualAreasRatio = ((double) potentialVirtualDiffTravelSpots.size()) / totalVirtualAreas;
            }

            //This Gaurds Against iterating over a zero set or any set that may already be 
            // disqualified due to its lack of possibilities in comparison to the current ratio
            if (remainingRealAreasRatio > 0 && remainingRealAreasRatio >= curFromRealMatchCaseRatio_.get()
                    && remainingVirtualAreasRatio >= curFromVirtualMatchCaseRatio_.get()) {
                //match Variables
                //List for References
                TravelMatchCase curMatch = null;
                //List of maxMatches
                List<TravelMatchCase> maxMatches = new ArrayList<TravelMatchCase>();
                boolean maxSet = false;
                double maxFromRealMatchRatio = 0, maxFromVirtualMatchRatio = 0, curFromRealMatchRatio = 0, curFromVirtualMatchRatio = 0;
                int idx = 0;
                boolean fractionalEvaluate = false;

                do {
                    
                    //Store SpotFAreaData for later idx++ iterations as a virtual Spot
                    NoSpotFAreaData curTravelCompFArea = null;
                    try {
                        for (FiducialTravelDiff2D diff : potentialRealDiffTravelSpots.get(idx)) {
                            if (curTravelCompFArea == null) {
                                curTravelCompFArea = new NoSpotFAreaData(diff.prevSpot_, diff.areaOwnerRef_);
                            }
                            curMatch = findSpotShiftMatch(diff, potentialRealDiffTravelSpots.subList(idx + 1, potentialRealDiffTravelSpots.size()), potentialVirtualDiffTravelSpots, totalRealAreas, totalVirtualAreas);
                            if (curMatch != null) {
                                //Automatic Flag Case
                                curFromRealMatchRatio = curMatch.getFromRealMatchRatio();
                                curFromVirtualMatchRatio = curMatch.getFromVirtualMatchRatio();
                                if (curFromRealMatchRatio == 1
                                        && curFromVirtualMatchRatio == 1) {
                                    if (!maxSet) {
                                        //Immediate Set For Perfect Match
                                        curFromRealMatchCaseRatio_.set(1.0f);
                                        curFromVirtualMatchCaseRatio_.set(1.0f);
                                        maxFromRealMatchRatio = 1;
                                        maxFromVirtualMatchRatio = 1;
                                        maxMatches.clear(); //Clear any previous matches
                                        maxSet = true;
                                    }
                                    //add max Match automatically
                                    maxMatches.add(curMatch);
                                } else if (curFromRealMatchRatio >= maxFromRealMatchRatio
                                        && curFromVirtualMatchRatio >= maxFromVirtualMatchRatio
                                        && curFromRealMatchRatio >= curFromRealMatchCaseRatio_.get()
                                        && curFromVirtualMatchRatio >= curFromVirtualMatchCaseRatio_.get()) {
                                    //Other Threads Haven't overshadowed this result
                                    if (!maxMatches.isEmpty()
                                            || (maxFromRealMatchRatio == curFromRealMatchRatio && maxFromVirtualMatchRatio == curFromVirtualMatchRatio)) {
                                        maxMatches.add(curMatch);
                                    } else {
                                        maxMatches.clear(); //There's a new standard
                                        maxMatches.add(curMatch);
                                        maxFromRealMatchRatio = curFromRealMatchRatio;
                                        maxFromVirtualMatchRatio = curFromVirtualMatchRatio;
                                    }
                                }

                            }
                        }
                    } catch (IndexOutOfBoundsException ex) {
                        throw ex;
                    }

                    //Update the number ratio of remaining Real Areas based on list depletion
                    // from findSpotShiftMatch()
                    if (totalRealAreas == 0) {
                        //Divide by zero guard
                        remainingRealAreasRatio = 0;
                    } else {
                        remainingRealAreasRatio = ((double) (potentialRealDiffTravelSpots.size() - 1.0 - idx)) / totalRealAreas;
                    }
                    if (totalVirtualAreas == 0) {
                        //To Account For Virtual Areas Being Fulfilled and the way their stored
                        remainingVirtualAreasRatio = 1;
                    } else {
                        remainingVirtualAreasRatio = ((double) potentialVirtualDiffTravelSpots.size()) / totalVirtualAreas;
                    }

                    //Check if remaining matches/startingNumber could provide a better ratio
                    if ( remainingRealAreasRatio >= maxFromRealMatchRatio
                            && remainingVirtualAreasRatio >= maxFromVirtualMatchRatio
                            && remainingRealAreasRatio >= curFromRealMatchCaseRatio_.get()
                            && remainingVirtualAreasRatio >= curFromVirtualMatchCaseRatio_.get()) {
                        //instead of performing removal on potentialRealDiffTravelSpots, just cheat and add it to NoSpotFarea 
                        noSpotFAreas_.add(curTravelCompFArea);
                        idx++;
                        //In case the index is the end index (which will not allow sublists)
                        if( idx >= potentialRealDiffTravelSpots.size() - 1 ) {
                            fractionalEvaluate = false;
                        }
                        else {
                            fractionalEvaluate = true;
                        }
                    } else {
                        fractionalEvaluate = false;
                    }

                } while (fractionalEvaluate);

                // Multiple Results is more costly, but allows for Comparison of intensity decreases for repeat match cases
                //Current Logic is FromRealMatches Are Better than From VirtualMatches
                //Modification at end, allows for reChecking of atomicDouble ratios (should be adequate)
                if (!maxMatches.isEmpty()) {                   
  
                    //Display any Matches
                    paintRois.clear();
                    List<Color> colorOpts = new ArrayList<Color>();
                    {
                        colorOpts.add(Color.BLUE);
                        colorOpts.add(Color.CYAN);
                        colorOpts.add(Color.GREEN);
                        colorOpts.add(Color.MAGENTA);
                        colorOpts.add(Color.ORANGE);
                    }
                    int i = 0;
                    for (TravelMatchCase match : maxMatches) {
                        if (i >= colorOpts.size()) {
                            i = 0;
                        }
                        Color lineCol = colorOpts.get(i);
                        for (FiducialTravelDiff2D diff : match.getFiducialTranslationSpots()) {
                            if (diff.toVirtual_) {
                                diff.spotRef_.getBoundingBox().setStrokeColor(Color.blue);
                                paintRois.add((Roi) diff.spotRef_.getBoundingBox().clone());
                            } else {
                                diff.spotRef_.getBoundingBox().setStrokeColor(Color.red);
                                paintRois.add((Roi) diff.spotRef_.getBoundingBox().clone());
                            }
                            Line line = new Line(diff.spotRef_.getXPixelLocation(), diff.spotRef_.getYPixelLocation(),
                                    diff.prevSpot_.getXPixelLocation(), diff.prevSpot_.getYPixelLocation());
                            line.setStrokeWidth(3);
                            line.setStrokeColor(lineCol);
                            paintRois.add(line);
                        }
                        i++;
                    }
                    createWindow(prevFAreas_.get(0).getImagePlus().getProcessor(), paintRois);

                    //This synchronization allows for multiple operations on atomic cureFromVirtualMatchCaseRatio as well
                    synchronized (matchResults_) {

                        TravelMatchCase sample = maxMatches.get(0); //They should all have same ratios
                        if (sample.getFromRealMatchRatio() == curFromRealMatchCaseRatio_.get()) {
                            if (sample.getFromVirtualMatchRatio() == curFromVirtualMatchCaseRatio_.get()) {
                                matchResults_.addAll(maxMatches);
                            } else if (sample.getFromVirtualMatchRatio() > curFromVirtualMatchCaseRatio_.get()) {
                                curFromVirtualMatchCaseRatio_.set(sample.getFromVirtualMatchRatio());
                                matchResults_.clear();
                                matchResults_.addAll(maxMatches);
                            }
                        } else if (sample.getFromRealMatchRatio() > curFromRealMatchCaseRatio_.get()) {
                            curFromRealMatchCaseRatio_.set(sample.getFromRealMatchRatio());
                            curFromVirtualMatchCaseRatio_.set(sample.getFromVirtualMatchRatio());
                            matchResults_.clear();
                            matchResults_.addAll(maxMatches);
                        }
                    }
                }
            }
        }
    }

    /**
    *  Takes A FiducialTravel Difference Object as a comparison object and a list of other complementary Travel Differences
    *   Using the Differences that were found as results of shifts, the this builds a TravelMatchCaseObject
    *   @param compShift - Comparison Travel Difference for a given FiducialArea to shifted Fiducial Area Travel
    *        Note:  For Each Candidate list, inner Lists are potential differences for a given shifted Fiducial Area
    *   @param realSpotCandidates - List of Each Real Detected Fiducial Areas Travel Differences
    *   @param virtualSpotCandidates - List of Each Virtually Simulated (in case of blinking frames) Fiducial Areas Travel Differences
    *   @param totalPossibleFromRealMatches - The Total number of "From Real" Matches that
    *               can be found in an ideal situation (this is in the case that the spot 
    *               candidate set is smaller for the sake of faster search)
    *   @param totalPossibleFromVirtualMatches - The Total number of "From Virtual" Matches that
    *               can be found in an ideal situation (this is in the case that the spot 
    *               candidate set is smaller for the sake of faster search)
    *   @return - A TravelMatchCase consisting of Actual Matches 
    *            and filler virtual matches for Fiducial Areas without (anticipating blinking)
    */
    private TravelMatchCase findSpotShiftMatch( FiducialTravelDiff2D compShift, List<List<FiducialTravelDiff2D>> realSpotCandidates, List< List< FiducialTravelDiff2D > > virtualSpotCandidates,
            int totalPossibleFromRealMatches, int totalPossibleFromVirtualMatches) {

        noSpotFAreasBuffer_.clear();  //Clear Any Buffer Leftovers
        
        int particularFails = noSpotFAreas_.size();
        int failLimit = (int) Math.ceil(totalPossibleFromRealMatches * failLimitRatio_);
        TravelMatchCase mCase = new TravelMatchCase( totalPossibleFromRealMatches, totalPossibleFromVirtualMatches ); 
        mCase.addMatch(compShift); //Add the comparison ShiftValue
        //Safe Iteration Variables
        Iterator< List<FiducialTravelDiff2D> > it = realSpotCandidates.iterator();
        List<FiducialTravelDiff2D> shiftCandidates;
        while( it.hasNext() ) {/*for( List<FiducialTravelDiff2D> shiftCandidates : realSpotCandidates ) {*/
            shiftCandidates = it.next();
            FiducialTravelDiff2D match = match(compShift, shiftCandidates);
            if (match != null) {
                mCase.addMatch(match);
                //Clean Up the List if there is a null value
                if( shiftCandidates.isEmpty() ) {
                    //Add the previous value to the noSpotFAreas List for virtuals
                    noSpotFAreasBuffer_.add( new NoSpotFAreaData( match.prevSpot_, match.areaOwnerRef_ ) );
                    //realSpotCandidates.remove( shiftCandidates );
                    it.remove();
                }
            }
            else {
                particularFails++;
                if( particularFails >= failLimit ) {
                    //Add Buffer Spots
                    addNoSpotBuffer();
                    return null;
                }
                //append virtual Difference
                mCase.addMatch( FiducialTravelDiff2D.createToVirtualTravelDiff2D( compShift, shiftCandidates.get(0).prevSpot_, shiftCandidates.get(0).areaOwnerRef_ ) );
            }
        }
        
        //Check For Virtuals  
        //Readjust failLimit to account for all Possibilities
        failLimit = (int) Math.ceil ((realSpotCandidates.size() + virtualSpotCandidates.size()) * failLimitRatio_ );
        it = virtualSpotCandidates.iterator();
        while( it.hasNext() ) /*for( List<FiducialTravelDiff2D> shiftCandidates : virtualSpotCandidates )*/ {
            shiftCandidates = it.next();
            FiducialTravelDiff2D match = match(compShift, shiftCandidates);
            if( match != null ) {
                mCase.addMatch( match );
                ij.IJ.log( "A From Virtual Was Registered" );
                if( shiftCandidates.isEmpty() ) {
                    noSpotFAreasBuffer_.add( new NoSpotFAreaData( match.prevSpot_, match.areaOwnerRef_ ) );
                    //virtualSpotCandidates.remove( shiftCandidates );
                    it.remove();
                }
            }
            else {
                particularFails++;
                if( particularFails >= failLimit ) {
                    //Add Buffer Spots
                    addNoSpotBuffer();
                    return null;
                }
                ij.IJ.log( "A Virtual From Virtual Was Registered ");
                
                //append virtual Difference
                mCase.addMatch( FiducialTravelDiff2D.createToVirtualTravelDiff2D( compShift, shiftCandidates.get(0).prevSpot_, shiftCandidates.get(0).areaOwnerRef_ ) );
            }
        }
        
        //For Each numFail Case, we will implement a virtual Case, in case of blinking
        for( NoSpotFAreaData noSpotData : noSpotFAreas_ ) {
            //Create A  virtual copy
            mCase.addMatch( FiducialTravelDiff2D.createToVirtualTravelDiff2D( compShift, noSpotData.prevSpot_, noSpotData.curFArea_ ) );
        }
        addNoSpotBuffer();
        
        return mCase;
    }
    
    /*
    *   Adds Objects in noSpotFAreasBuffer_ to noSpotFAreas_
    *   This is a FIFO operation ( noSpotFAreasBuffer_ clears after adding its contents
    */
    private void addNoSpotBuffer() {
        noSpotFAreas_.addAll(noSpotFAreasBuffer_);
        noSpotFAreasBuffer_.clear();
    }
 
    private FiducialTravelDiff2D match(FiducialTravelDiff2D ref, List<FiducialTravelDiff2D> compList) {

        FiducialTravelDiff2D diff;
        for (int i = 0; i < compList.size(); ++i ) {
            diff = compList.get(i);

            //Checks Are As Such ( pass if difference is within the AbbeLimit Uncertainty,
            //of it the difference is in the same direction (positivity) x first then y
            if ( (Math.abs(ref.xDiffs_) < ref.xDiffractionUncertainty_
                    && Math.abs(diff.xDiffs_) < diff.xDiffractionUncertainty_)
                    || (diff.xDiffs_ < 0 && ref.xDiffs_ < 0) || (diff.xDiffs_ >= 0 && ref.xDiffs_ >= 0)) {
                if ((Math.abs(ref.yDiffs_) < ref.yDiffractionUncertainty_
                    && Math.abs(diff.yDiffs_) < diff.yDiffractionUncertainty_)
                        || (diff.yDiffs_ < 0 && ref.yDiffs_ < 0) || (diff.yDiffs_ >= 0 && ref.yDiffs_ >= 0)) {
                    //While Redundant for non-moving case, this makes sure that the distance difference is within uncertainties
                    if (Math.abs(diff.xDiffs_ - ref.xDiffs_) < (diff.xDiffractionUncertainty_ + ref.xDiffractionUncertainty_)
                            && Math.abs(diff.yDiffs_ - ref.yDiffs_) < (diff.yDiffractionUncertainty_ + ref.yDiffractionUncertainty_)) {
                        //Secondary Filter of Remaining Spots in the case of defocusing and overlapping Gaussians
                        //return compList.get(i);
                        return compList.remove(i);  //May Be optimized Later (but large data is not expected)
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     *  Populates an ImageWindow with the ImageProcessor as a new Slice with corresponding 
     *   Overlays populated by the Paint Rois.  It is the implementations burden to
     *   decide whether the Rois are copies or references.
     * 
     * @param iProc - Image Processor for the next slice to be added
     * @param PaintRois - Rois to be painted on the slice as an overlay
     */
    private void createWindow(ImageProcessor iProc, List<Roi> PaintRois) {
        //Display The Spots Found
        //Create New ImageWindow
        ImageProcessor copyProc = new ShortProcessor(iProc.getWidth(), iProc.getHeight());
        copyProc.setPixels(iProc.getPixelsCopy());
        ImageStack iStack;
        Window imWin1 = WindowManager.getWindow(acquisitionTitle_);
        StackWindow sWin;
        if (imWin1 == null || (imWin1 instanceof StackWindow) == false) {
            if( imWin1 != null ) {
                acquisitionTitle_ = WindowManager.getUniqueName(acquisitionTitle_ );
            }
            iStack = new ImageStack(copyProc.getWidth(), copyProc.getHeight() );
            iStack.addSlice(copyProc);
            ImagePlus ip = new ImagePlus(acquisitionTitle_, iStack);
            sWin = new StackWindow(ip);
            //TODO: Merge with WindowManager
            //WindowListener
            //sWin.addWindowListener( new WindowListner() {);
            //WindowManager.addWindow(sWin);
        } else {
            sWin = (StackWindow) imWin1;
            sWin.getImagePlus().getStack().addSlice(copyProc);
        }
        ImagePlus ip = sWin.getImagePlus();
        
        
        Overlay ov = ip.getOverlay();
        if (ov == null) {
            ov = new Overlay();
        }

        int sliceNum1 = ip.getStackSize();
        for (Roi roi : PaintRois) {
            roi.setPosition(sliceNum1);
            ov.add(roi);
        }
        ip.setOverlay(ov);
        sWin.updateImage(ip);
        sWin.showSlice(sliceNum1);
        sWin.setVisible(true);
    }
    
}
