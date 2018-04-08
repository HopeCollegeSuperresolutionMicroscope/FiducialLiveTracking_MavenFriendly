/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.imagetrack;

import com.google.common.util.concurrent.AtomicDouble;
import edu.hope.superresolution.genericstructures.FiducialTravelDiff2D;
import edu.hope.superresolution.genericstructures.TravelMatchCase;
import edu.hope.superresolution.models.FiducialArea;
import edu.hope.superresolution.processors.FiducialAreaProcessor;
import edu.valelab.gaussianfit.utils.ReportingUtils;
import ij.ImagePlus;
import ij.gui.Roi;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  Class Designed for Finding the movement of 1 Fiducial Set to another Fiducial Set. 
 * <p>
 * This class is intended to do iterative walking through potential move areas and coordinating
 * the maximum likely hood.  That code is currently commented out, however, due to an overselection of 
 * the wrong type.  
 * <p>
 * TODO:  rework this class to expand search areas for results if the central expectation
 * is not found.
 * 
 * @author Justin Hans
 */
public class FiducialMoveFinder {
           
    private final String acquisitionTitle_; //Used For Displays Correspondings to the owning Acquisition
    
    //private final List<FiducialArea> prevFAreas_;  //Previous Image and fiducial Areas
    //private final List< FiducialArea > curFAreas_;  //This is the current Fiducial Areas and Image
    
    //Globals Used for Shifting Searches
    //private final ImagePlus curIP_;
    //private final FiducialAreaProcessor curFAreaProcessor_;
    
    //private final BlockingQueue< List<FiducialArea> > shiftedFiducialAreas_ = new LinkedBlockingQueue< List<FiducialArea> >();
    //matches_ allows for access to any multiple matches found of max number
    //private final List< TravelMatchCase > matches_ = Collections.synchronizedList( new ArrayList< TravelMatchCase >() );
    //private TravelMatchCase selectedMatch_ = null;
    //private final AtomicDouble curMaxFromRealMatchRatio_ = new AtomicDouble( 0.0f );
    //private final AtomicDouble curMaxFromVirtualMatchRatio_ = new AtomicDouble( 0.0f );
    
    //The Minimum Number of Fiducials to only find before checking other areas
    private int minNumFiducialsForTrack_ = 1;
    
    private double intensityDiminishRatio_ = .8;
    //private double failLimitRatio_ = .5;
    private int maxMissingFrames_;
    //Atomic Variable to indicate a failure to place the "poison" or endkey result
    //private AtomicBoolean endKeyPlacedIndicator_ = new AtomicBoolean();
    
    /**
     * Constructor
     * 
     * @param maxMissingFrames - The maximum Number of Missing Frames allowed for tracks before not searching in a FiducialArea 
     * @param minNumFiducialsForTrack - The minimum Number of Track Correlated 
     *                                  Fiducials (less than or equal to Number of Fiducial Areas)
     *                                  for it to count as a viable possible track.
     * @param acquisitionTitle - The Title Used For Graphical Displays that pertains to the Acquisition calling this class
     */
    public FiducialMoveFinder( int maxMissingFrames, int minNumFiducialsForTrack,
                                String acquisitionTitle ) {
        
        /*assert( prevFAreas.size() == curFAreas.size());
        assert( prevFAreas.size() > 0 );*/
        
        acquisitionTitle_ = acquisitionTitle;
        /*prevFAreas_ = prevFAreas;
        curFAreas_ = curFAreas;
        numAreas_ = prevFAreas.size();*/
        minNumFiducialsForTrack_ = minNumFiducialsForTrack;
        /*failLimitRatio_ = ((double) minNumFiduciailsForTrack_)/numAreas_;
        
        //Shift Traits
        originalSearchAreas_ = new ArrayList< Roi >( curFAreas_.size() );
        for (FiducialArea fArea : curFAreas_) {
            originalSearchAreas_.add(fArea.getTrackSearchArea());           
        }
        curIP_ = curFAreas_.get(0).getImagePlus();
        curFAreaProcessor_ = curFAreas.get(0).getFiducialAreaProcessor();
        */
        //endKeyPlacedIndicator_.set(false);
        maxMissingFrames_ = maxMissingFrames;

        
    }
    
    /**
     *  Tracking Constructor - Given A Set of Previous Fiducial Areas and a new ImagePlus, 
     *   create a set of searches across them
     * 
     * @param prevFAreas - The Previous Fiducial Areas with selectedSpots to search for in the new ImagePlus
     * @param curIP - The new ImagePlus to Search 
     * @param maxMissingFrames - The maximum Number of Missing Frames allowed for tracks before not searching it
     * @param minNumFiducialsForTrack - The minimum Number of Track Correlated 
     *                                  Fiducials (less than or equal to Number of Fiducial Areas)
     *                                  for it to count as a viable possible track.
     * @param acquisitionTitle - The Title Used For Graphical Displays that pertains to the Acquisition calling this class
     */
/*    public FiducialMoveFinder( List<FiducialArea> prevFAreas, ImagePlus curIP, 
                                int maxMissingFrames, int minNumFiducialsForTrack,
                                String acquisitionTitle ) {
        
        acquisitionTitle_ = acquisitionTitle;
        //prevFAreas_ = prevFAreas;
        numAreas_ = prevFAreas.size();
        minNumFiduciailsForTrack_ = minNumFiducialsForTrack;*/
        //failLimitRatio_ = ((double) minNumFiduciailsForTrack_)/numAreas_;
        
        //Shift Traits
        //curIP_ = curIP;
        //curFAreaProcessor_ = prevFAreas_.get(0).getFiducialAreaProcessor();
        //curFAreaProcessor_.enableAsyncProcessing(false);  //Make Sure that We Are not Async Processing in Constructor
        
        /*originalSearchAreas_ = new ArrayList< Roi >( prevFAreas_.size() );
        curFAreas_ = new ArrayList< FiducialArea >( prevFAreas_.size() );
        //Copy FiducialAreas into curFAreas (First Assumes Same Track Area)
        int numRealAreas = 0;
        for( FiducialArea fArea : prevFAreas ) {
            //This a check for a loss of threshold or spot
           //if ( fArea.getRawSelectedSpot() != null ) {
                Roi trackArea = fArea.getTrackSearchArea();
                originalSearchAreas_.add(trackArea);
                if ( !fArea.getRawSelectedSpot().isVirtual() ) {
                    //If Not Virtual, let it affect FailRatio
                    ++numRealAreas;
                }
                curFAreas_.add(new FiducialArea(curIP_, trackArea, fArea));*/
            /*} else {
                ReportingUtils.showError( "There was a null selected Spot");
            }*/
        //}
       /* 
        //endKeyPlacedIndicator_.set(false);
        maxMissingFrames_ = maxMissingFrames;
        
    }*/
    
    /**
     * Track the Selected Particles for each FArea over to the next ImagePlus. This will assume the anticipated travel area
     * of the previous selected particles and will apply them to the current imagePlus.
     * <p>
     * Note, This will require a minimum number of fiducials areas for a non-erroneous result, since the fiducial sets will be 
     * correlated together to produce the correct track.
     * 
     * @param prevFAreas - The List of previous Fiducial Area that are to be tracked
     * @param curIP - The current ImagePlus to apply Tracking for
     * @return - The best case match case, or null if nothing could be tracked.  Please note, as tracking is still subjective, this may return a bad track.
     * @see FiducialMoveFinder#SelectMatch(java.util.List, double) for better understanding of the selection process when multiple track possibilities are detected
     */
    public TravelMatchCase CorrelateTrackSelectedParticles( List<FiducialArea> prevFAreas, ImagePlus curIP ) {
        
        //Create a the current FAreas centered on the selectFiducial and the trackAreas
        List<FiducialArea> curFAreas = new ArrayList< FiducialArea >( prevFAreas.size() );
        //Copy FiducialAreas into curFAreas (First Assumes Same Track Area)
        int numRealAreas = 0;
        for( FiducialArea fArea : prevFAreas ) {
            //This a check for a loss of threshold or spot
           //if ( fArea.getRawSelectedSpot() != null ) {
                //ensure the fAreas aren't asynchronously processing so we wait
                boolean wasAsync = fArea.getFiducialAreaProcessor().isAsyncProcessEnabled();
                try {
                if( wasAsync ) {
                    fArea.getFiducialAreaProcessor().enableAsyncProcessing(false);
                }
                Roi trackArea = fArea.getTrackSearchArea();
                if ( !fArea.getRawSelectedSpot().isVirtual() ) {
                    //If Not Virtual, let it affect FailRatio
                    ++numRealAreas;
                }
                curFAreas.add(new FiducialArea(curIP, trackArea, fArea));
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
        
        return CorrelateDifferences( prevFAreas, curFAreas );
        
    }
    
    /**
     * Track the Selected Particles for each FArea from one Image over to the other.
     * <p>
     * Note, This will require a minimum number of fiducials areas for a non-erroneous result, since the fiducial sets will be 
     * correlated together to produce the correct track.
     * <p>
     * Additional note,expansionary searching has been commented out, but would like to 
     * be separated out and applied again as edge cases arise that cannot have a meaningful anticipated travel zone.
     * 
     * @param prevFAreas - The previous FiducialAreas whose selected particles will be used for the track comparison
     * @param curFAreas - The current FiducialAreas on a new Image Area that will be used mainly for its geometry and the fiducial processor.  If a selected FiducialParticle is
     *                    already selected on this FiducialArea, it will not be altered, but the MatchCase returned will provide the particles that should be selected based off of track.
     * @return - The best case match case, or null if nothing could be tracked.  Please note, as tracking is still subjective, this may return a bad track.
     * @see FiducialMoveFinder#SelectMatch(java.util.List, double) for better understanding of the selection process when multiple track possibilities are detected
     */
    public TravelMatchCase CorrelateDifferences(List<FiducialArea> prevFAreas, List<FiducialArea> curFAreas) {

        assert(prevFAreas.size() > 0 );
        assert( prevFAreas.size() == curFAreas.size());
        
        final BlockingQueue< List<FiducialArea> > shiftedFiducialAreas = new LinkedBlockingQueue< List<FiducialArea> >();
        //matches_ allows for access to any multiple matches found of max number
        final List< TravelMatchCase > matches = Collections.synchronizedList( new ArrayList< TravelMatchCase >() );
        //Atomic Variable to indicate a failure to place the "poison" or endkey result
        AtomicBoolean endKeyPlacedIndicator = new AtomicBoolean(false);
        //Initialize Atomic Ratios for comparison across SpotMatchThreads
        final AtomicDouble curMaxFromRealMatchRatio = new AtomicDouble( 0.0f );
        final AtomicDouble curMaxFromVirtualMatchRatio = new AtomicDouble( 0.0f );
        
        double failLimitRatio = ((double) minNumFiducialsForTrack_)/prevFAreas.size();    
        
        //Central Fiducial Area Storage (Temporary Fix)
        int centralMatches = 0;
        double centralfromRealMatchRatio = 0;
        double centralfromVirtualMatchRatio = 0; 
       
        //For the sake of resources, don't create extra threads just yet
        try {
            shiftedFiducialAreas.put(curFAreas);
            //Add poison for first evaluation
            shiftedFiducialAreas.put( new ArrayList<FiducialArea>() );
            endKeyPlacedIndicator.set(true);
        }  catch ( InterruptedException ex ) {
            ij.IJ.log("No Correlation Performed, Thread Queue Put Interrupted " + Thread.currentThread().getName());
            Thread.currentThread().interrupt(); //Reinterrupt
            return null;  //return without any processing if there is an error in Queueing
        }
        //This needs to change to ExecutorServices for the sake of central resources
        SpotMatchThread[] matchThreads = new SpotMatchThread[8];
        matchThreads[0] = new SpotMatchThread( prevFAreas, shiftedFiducialAreas, matches, intensityDiminishRatio_,
                                failLimitRatio, maxMissingFrames_, endKeyPlacedIndicator,
                                curMaxFromRealMatchRatio, curMaxFromVirtualMatchRatio, acquisitionTitle_ );
        
        matchThreads[0].init();
        matchThreads[0].join();
        
        //TEMPORARY FIX UNTIL BETTER EVALUATION DETERMINED FOR EXTRA TRACK WINDOWS ?? What was i saying..
        // Store Central Ratio and Central Indices
        centralMatches = matches.size();
        centralfromRealMatchRatio = curMaxFromRealMatchRatio.get();
        centralfromVirtualMatchRatio = curMaxFromVirtualMatchRatio.get();
  
        if( matches.size() > 0 ) {
        //100% match is assumed to be the best currently
        //  Currently, we don't have a tolerance for searching a blink
        /*ij.IJ.log( "Central Match = " + centralfromRealMatchRatio_ + " and Virt = " + centralfromVirtualMatchRatio_ + "\n For number of: " + numAreas_ );
        for( TravelMatchCase cas : matches ) {
            //ij.IJ.log("For Diff Real = " + cas.getFromRealMatchRatio() + " and Virt = " + cas.getFromVirtualMatchRatio() );
            String msg = "Spot ";
            for( FiducialTravelDiff2D diff : cas.getFiducialTranslationSpots() ) {
                msg += (diff.toVirtual_) ? " Virt, " : " Real, ";
            }
            ij.IJ.log(msg);
        }*/
        int numRealMatches = 0;
        int numVirtualMatches = 0;
              //Calculate The Overall Potential Real + Blink Matches
            numRealMatches = (int) (centralfromRealMatchRatio * matches.get(0).getMaxPossibleRealMatches());
            numVirtualMatches = (int) (centralfromVirtualMatchRatio * matches.get(0).getMaxPossibleVirtualMatches());
        
        //Remove Poison Key
        shiftedFiducialAreas.remove();
        endKeyPlacedIndicator.set(true);  //Reset in the event of a starting thread hanging        
        
        //Calculated the ZRange of the expected set
        double expectedZRange = calculatePreviousZRangeDifference( prevFAreas);
        // 3 Conditions based on uncertainties - 1 spot left as a total ( only use central spot)
        //   Both from real and from virtual spots are ideal
        //   The sum of matches (from virtual and reals is above an assumed minimum matches threshold)
        if( (centralfromRealMatchRatio == 1 && centralfromVirtualMatchRatio == 1) 
                ||  numRealMatches + numVirtualMatches >= minNumFiducialsForTrack_ ) {
            //If there are multipled Values, choose the one with the most even intensity change ratios
            try {
                //ij.IJ.log( "The Current Match Ratio is " + centralfromRealMatch_ + "\n" );
                TravelMatchCase selectedMatch = SelectMatch( matches, curMaxFromRealMatchRatio, curMaxFromVirtualMatchRatio, expectedZRange );
                //ApplySelectedMatchCase(selectedMatch_);
                matchThreads[0].stopImmediate();
                return selectedMatch;
            } catch ( EmptyStackException ex ) {
                ex.printStackTrace();
                //Continue on, and try to recover with other Fiducials with less ridiculous intensities
                ReportingUtils.showError("Somehow Mishandled Selection of MatchCase");
            }
        }
        }
        else {
            ij.IJ.log( "No Central Matches Found!");
        }
        
        //Current Multiple Area Result Does not have enough metrics to warrant throwing out false positives
        /*
        ij.IJ.log( "Spawning New Threads");
        
        //If Not, Initialize Threads for Shifted Area search
        //They will block on shiftedFiducialAreas
        for( int i = 1; i < matchThreads.length; ++i ) {
            matchThreads[i] = new SpotMatchThread( prevFAreas_, shiftedFiducialAreas, matches, intensityDiminishRatio_,
                                failLimitRatio_, maxMissingFrames_, endKeyPlacedIndicator,
                                curMaxFromRealMatchRatio_, curMaxFromVirtualMatchRatio_, acquisitionTitle_ );
            matchThreads[i].init();
        }
        
        ImagePlus curIp = curFAreas.get(0).getImagePlus();
        //Shift The originalRois and create Fiducial Area Copy Lists for evaluation
        //maximum multiple Width Extensions (1 is the square around the original)
        int maxSearchExtensions = 1;
        try {
            for (int x = 1; x <= maxSearchExtensions; ++x) {
                for (int y = 1; y <= maxSearchExtensions; ++y) {
                    //This can be Optimized with Exceptions, 
                    // but Currently, just block on an iteration if there is already a 100% match
                    // Iterates Through
                    if ( curMaxFromRealMatchRatio_.get() < 1.0f && curMaxFromVirtualMatchRatio_.get() <= 1.0f ) {
                        //NorthWest
                        shiftedFiducialAreas.put(createShiftedList(prevFAreas, curIp, -x, y));
                        //North
                        shiftedFiducialAreas.put(createShiftedList(prevFAreas, curIp, 0, y));
                        //NorthEast
                        shiftedFiducialAreas.put(createShiftedList(prevFAreas, curIp, x, y));
                        //East
                        shiftedFiducialAreas.put(createShiftedList(prevFAreas, curIp, x, 0));
                        //SouthEast
                        shiftedFiducialAreas.put(createShiftedList(prevFAreas, curIp, x, -y));
                        //South
                        shiftedFiducialAreas.put(createShiftedList(prevFAreas, curIp, 0, -y));
                        //SouthWest
                        shiftedFiducialAreas.put(createShiftedList(prevFAreas, curIp, -x, -y));
                        //West
                        shiftedFiducialAreas.put(createShiftedList(prevFAreas, curIp, -x, 0));
                    }
                }
            }

            //Empty List As Indicator to finish (poison)
            shiftedFiducialAreas.put(new ArrayList<FiducialArea>());

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();  //Pass Interrupt Along
        }
        
        //Block 
        for( int i = 0; i < matchThreads.length; ++i ) {
            matchThreads[i].join();
            if( !endKeyPlacedIndicator.get() ) {
                //Somehow Blocking Queue was full or misbehaving
                //Manually Stop All Threads
                for( int stopIdx = i; stopIdx < matchThreads.length; ++stopIdx ) {
                    //Calls interrupt on Processes, early enough that join can wait
                    matchThreads[ stopIdx ].stopImmediate();
                }
                //We have effectively placed each endkey for the threads
                endKeyPlacedIndicator.set(true);
            }
            matchThreads[i] = null;
        }
        
        //Currently, we will default to the central matches only if the ratio never changed
        //May be changed when other metric is proven
        int numMatches;
        if( centralfromRealMatchRatio_ == curMaxFromRealMatchRatio_.get() &&
                centralfromVirtualMatchRatio_ == curMaxFromVirtualMatchRatio_.get() ) {
            matches = matches.subList(0, centralMatches);
        } else {
            numMatches = matches.size();
        }
        
        //curMatchRatio should be 100% but in the event it's not,
        //Pick The highest match with the most uniform intensity ratios
        try {
            TravelMatchCase selectedMatch  = SelectMatch(matches, expectedZRange );
            //ApplySelectedMatchCase(selectedMatch_);
            return selectedMatch;
        } catch (EmptyStackException ex) {
            ex.printStackTrace();
            ReportingUtils.showError("There were no matches with adequate Intensity Distributions");
        }*/
        
        //Need to decide what to do with a null result
        return null;
    }
    
    /**
     * Creates a list of Fiducial Areas that is the original prevFAreas_ in which track motion was checked, 
     * but shifted by the x and y width shifts. 
     * <p>
     * Note, the arguments are given in terms of Ratios of the whole width of fiducial area.
     * i.e. an XWidthShift of -1 will move the search areas -XWidth (to the left in the coordinate system).  
     * an XWidthShift of .5 will push the fiducial area halfway to the right.
     * 
     * @param origFAreas - The List of Fiducial Areas that is being shifted
     * @param curIP - The current ImagePlus on which the shifted FiducialAreas will be placed
     * @param xWidthShifts - The multiple of the Xwidth of the area 
     * @param yWidthShifts - The multiple of the yWidth of the area
     * @return 
     */
    private List< FiducialArea > createShiftedList( List<FiducialArea> origFAreas, ImagePlus curIP, int xWidthShifts, int yWidthShifts ) {
        
        List< FiducialArea > shiftedFAreaList = new ArrayList<FiducialArea>();
        Roi copyRoi;
        Roi origArea;
        for( FiducialArea fArea : origFAreas ) {
            origArea = fArea.getTrackSearchArea();
            copyRoi = new Roi( origArea.getXBase() + ( xWidthShifts * origArea.getFloatWidth() ),
                                origArea.getYBase() + ( yWidthShifts * origArea.getFloatHeight() ),
                                origArea.getFloatWidth(), origArea.getFloatHeight() );
            shiftedFAreaList.add( new FiducialArea( curIP, copyRoi, fArea ) );
        }
        
        return shiftedFAreaList;
        
    }
    
    //Searches Match Set For Most Appropriate Match
    //Note:  Should only be called after threads have finished processing
    /**
     * Selects a Match out of all potentially possible MatchCases that were returned.  The function looks for the 
     * least deviated Z values between the set of Fiducials for each match case and compares them against
     * the minimally expected Z Range.  
     * <p>
     * This is simplistic in nature, assuming effectively that any extraneous matches will be the result of larger uncertainty fits
     * and therefore, larger Z scores, while the expected difference of the original track set will remain within itself.
     * <p>
     * Additionally, in the event of slight defocus, if nothing is within the expectedZRange due to increased uncertainties in a defocused fit,
     * the fiducial set with the least standard deviation in its intensity ratios from the traack reference to the assumed translation is chosen.
     * <p>
     * If there is only 1 match, that match is returned immediately.
     * 
     * @param expectedZRange - The positive Z range within which a matching set it expected to be constrained (in a good focus event)
     * @return - The selected TravelMatchCase based off of the above criteria
     * @throws EmptyStackException 
     */
    private TravelMatchCase SelectMatch( List<TravelMatchCase> matches, AtomicDouble curMaxFromRealMatchRatio,
                                            AtomicDouble curMaxFromVirtualMatchRatio, double expectedZRange ) throws EmptyStackException {
        
        int numMatches = matches.size();
        
        if( numMatches == 1 ) {
            return matches.get(0);
        }
       
        TravelMatchCase mCase;
        TravelMatchCase bestCase = null;
        double minIntRatioSigma = Double.MAX_VALUE, minZPlaneDefocus = Double.MAX_VALUE;
        double curIntRatioSigma;
        double curZPlaneStdDev;
        for( int i = 0 ; i < numMatches; ++i ) {
            mCase = matches.get(i);
                 
            //In the Case of reentry permission misordering, filter any matches that are below criteria
            if (mCase.getFromRealMatchRatio() == curMaxFromRealMatchRatio.get()
                    && mCase.getFromVirtualMatchRatio() == curMaxFromVirtualMatchRatio.get()) {
                
                //If any case is within the diffraction limit, select it immediately since we assume stationary is still the normal mode
                FiducialTravelDiff2D diff = mCase.getFiducialTranslationSpots().get(0);
                if( diff.xDiffs_ < diff.xDiffractionUncertainty_ 
                        && diff.yDiffs_ < diff.yDiffractionUncertainty_ ) {
                    bestCase = mCase;
                    break;
                }
                
                curZPlaneStdDev = CalculateZRangeStandardDeviation(mCase, true);
                //As an expectation that may or may not hold, based on fitting and data (and for zero plane assumption)
                // First check to see if any match z standardDeviation is within expectedZRange of previous
                //  This biases toward selection currently due to lower standardDeviation vs top to bottom range
                if (curZPlaneStdDev < expectedZRange) {
                    //Calculate Standard Deviation of previous to new intensity ratios
                    curIntRatioSigma = calculateIntensityShiftStandardDeviation(mCase);
                    if (curIntRatioSigma < minIntRatioSigma) {
                        minIntRatioSigma = curIntRatioSigma;
                        bestCase = mCase;
                    }
                }
            } else {
                //Remove the misordered case
                matches.remove(i);
                numMatches--;
            }
          
        }
        
        //If there is still no match, choose the minimum intensityStandardDeviation
        if( bestCase == null ) {
            for (int i = 0; i < numMatches; ++i) {
                mCase = matches.get(i);
                //Calculate Standard Deviation of previous to new intensity ratios
                curIntRatioSigma = calculateIntensityShiftStandardDeviation(mCase);
                if (curIntRatioSigma < minIntRatioSigma) {
                    minIntRatioSigma = curIntRatioSigma;
                    bestCase = mCase;
                }
            }
        }
        
        /*
        if( bestCase == null ) {
            throw new EmptyStackException();
        }*/
        
        return bestCase;
        
    }
    
    /**
     * Given a Travel Match Case, calculate the Standard Deviation in the intensity ratios from (current particle intensity/previous particle intensity)
     * 
     * @param mCase 
     * @return 
     */
    private double calculateIntensityShiftStandardDeviation( TravelMatchCase mCase ) {
        double sigma = 0;
        //We assume Intensity Ratio for Fiducials should decrease With the same ratio
        double avgIntRatio = 0;
        
        //Since We know the TravelMatchCase options_ is ordered by: real, virtual
        //Average Ratio
        List<FiducialTravelDiff2D> diffList = mCase.getFiducialTranslationSpots();
        FiducialTravelDiff2D diff;
        int realSize = diffList.size();
        for( int i = 0; i < realSize; ++i ) {
            diff = diffList.get(i);
            if( diff.toVirtual_ ) {
                realSize = i;
                break;
            }
            avgIntRatio += diff.intRatio_;
        }
        
        if( realSize == 0 ) {
            return Double.MAX_VALUE;
        }
        
        avgIntRatio = avgIntRatio / realSize;
        
        for( int i = 0; i < realSize; ++i ) {
            diff = diffList.get(i);
            sigma += Math.pow( diff.intRatio_ - avgIntRatio, 2); 
        }
        sigma = Math.sqrt(sigma/realSize);
        
        return sigma;
        
    }
    
    /**
     * Get the minimumNumberofFiducials (and FiducialAreas) that must be provided for a track to be performed.
     * <p>
     * NOTE: For Now, this is thread-safe since it's a getter with constructor initialization.
     * However, setting will need to be reviewed since it is an option to the user.
     * @return 
     */
    public int getMinNumberOfFiducialsForTrack() {
        return minNumFiducialsForTrack_;
    }
    
    //
    /**
     * Calculate the standard Deviation in Z Values Across the match Case +/- their uncertainties.
     *  If you want the minimum standard Deviation, the uncertainty will be subtracted
     *  from the standardDeviation and if the maximum is wanted, the uncertainty is added.
     * 
     * @param mCase The TravelMatchCase From which to compare zDistance_ values of the translationSpots
     * @param min <code>true</code> if the minimum is returned or <code>false</code> if the maximum is returned
     * @return The result (min or max) of standard deviation ( minus or plus) uncertainty
     */
    private double CalculateZRangeStandardDeviation( TravelMatchCase mCase, boolean min ) {
        double stdDev = 0;
        //We assume Intensity Ratio for Fiducials should decrease With the same ratio
        double avgZPlane = 0;
        double sumUncertaintySquares = 0;
        
        //Since We know the TravelMatchCase options_ is ordered by: real, virtual
        //Average Ratio
        List<FiducialTravelDiff2D> diffList = mCase.getFiducialTranslationSpots();
        FiducialTravelDiff2D diff;
        int realSize = diffList.size();
        for( int i = 0; i < realSize; ++i ) {
            diff = diffList.get(i);
            if( diff.toVirtual_ ) {
                realSize = i;
                break;
            }
            avgZPlane += diff.zDistance_;
        }
        
        if( realSize == 0 ) {
            return Double.MAX_VALUE;
        }
                
        avgZPlane = avgZPlane / realSize;
        
        for( int i = 0; i < realSize; ++i ) {
            diff = diffList.get(i);
            stdDev += Math.pow( diff.zDistance_ - avgZPlane, 2); 
            sumUncertaintySquares += Math.pow( diff.zDistanceUncertainty_, 2);
        }
        //Uncertainty Combined Using Formula: 1/(2*N)*sqrt((1 + 1/N)*sum(uncertainties^2))
        if( min ) {
            stdDev = Math.sqrt(stdDev/realSize) 
                    - 1.0f/(2*realSize)*Math.sqrt( (1 + 1.0f/realSize) * sumUncertaintySquares );
        } else {
            stdDev = Math.sqrt(stdDev/realSize) 
                    + 1.0f/(2*realSize)*Math.sqrt( (1 + 1.0f/realSize) * sumUncertaintySquares );
        }
        
        
        return stdDev;
    }
            
    /**
     *  Calculates the difference between the smallest dZ value and its uncertainty
     *  and the max to produce a maximum difference for the set of given FiducialAreas
     * 
     * @return the difference: (maxZi1 + uncertaintyi1) - (minZi2 - uncertaintyi2) 
     *                      (Can Be a zero value if there's one fiducial and no uncertainty)
     */
    private double calculatePreviousZRangeDifference( List<FiducialArea> fAreas ) {
        //Calculate Z Focus Range from prevFiducial Areas
        double minZ = Double.MAX_VALUE;
        double maxZ = Double.MIN_VALUE;
        for( FiducialArea fArea : fAreas ) {
            //We ARe only interested in non-virtuals
            if( !fArea.getRawSelectedSpot().isVirtual() ) {
                double tempMin = fArea.getFocusPlaneSigmaRef() - fArea.getFocusPlaneSigmaRefUncertainty();
                double tempMax = fArea.getFocusPlaneSigmaRef() + fArea.getFocusPlaneSigmaRefUncertainty();
                if( tempMin < minZ ) {
                    minZ = tempMin;
                }
                if( tempMax > maxZ ) {
                    maxZ = tempMax;
                }
            }
        }
                
        return maxZ - minZ;
    }
    
    //This only accounts for 100% case currently
    private void ApplySelectedMatchCase( TravelMatchCase mCase) {
        
        for( FiducialTravelDiff2D diff : mCase.getFiducialTranslationSpots() ) {
            try {
                diff.areaOwnerRef_.setSelectedSpotRaw( diff.spotRef_ );
            }
            catch ( Exception ex ) {
                ReportingUtils.showError( "We Did not Fully Set A Selected Spot ");
                throw new RuntimeException( ex );
            }
        }
        
    }
    
}
