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
 *
 * @author Justin Hanselman
 */
public class FiducialMoveFinder {
           
    private final String acquisitionTitle_; //Used For Displays Correspondings to the owning Acquisition
    
    private final List<FiducialArea> prevFAreas_;  //Previous Image and fiducial Areas
    private final List< FiducialArea > curFAreas_;  //This is the current Fiducial Areas and Image
    
    //Globals Used for Shifting Searches
    private final ImagePlus curIP_;
    private final FiducialAreaProcessor curFAreaProcessor_;
    private final List<Roi> originalSearchAreas_;  //Used For Roi 
    
    private final BlockingQueue< List<FiducialArea> > shiftedFiducialAreas_ = new LinkedBlockingQueue< List<FiducialArea> >();
    //matches_ allows for access to any multiple matches found of max number
    private final List< TravelMatchCase > matches_ = Collections.synchronizedList( new ArrayList< TravelMatchCase >() );
    private TravelMatchCase selectedMatch_ = null;
    private final AtomicDouble curMaxFromRealMatchRatio_ = new AtomicDouble( 0.0f );
    private final AtomicDouble curMaxFromVirtualMatchRatio_ = new AtomicDouble( 0.0f );
    //Central Fiducial Area Storage (Temporary Fix)
    private int centralMatches_ = 0;
    private double centralfromRealMatchRatio_ = 0;
    private double centralfromVirtualMatchRatio_ = 0;
    
    //The Minimum Number of Fiducials to only find before checking other areas
    private int numCentralMatchesThreshold_ = 1;
    
    //Storage to avoid calling size() everytime
    private final int numAreas_;
    
    private double intensityDiminishRatio_ = .8;
    private double failLimitRatio_ = .5;
    private int maxMissingFrames_;
    //Atomic Variable to indicate a failure to place the "poison" or endkey result
    private AtomicBoolean endKeyPlacedIndicator_ = new AtomicBoolean();
    
    //Recorrective?
    public FiducialMoveFinder( List<FiducialArea> prevFAreas, List<FiducialArea> curFAreas, 
                                int maxMissingFrames, int minNumFiducialsForTrack,
                                String acquisitionTitle ) {
        
        assert( prevFAreas.size() == curFAreas.size());
        assert( prevFAreas.size() > 0 );
        
        acquisitionTitle_ = acquisitionTitle;
        prevFAreas_ = prevFAreas;
        curFAreas_ = curFAreas;
        numAreas_ = prevFAreas.size();
        numCentralMatchesThreshold_ = minNumFiducialsForTrack;
        failLimitRatio_ = ((double) numCentralMatchesThreshold_)/numAreas_;
        
        //Shift Traits
        originalSearchAreas_ = new ArrayList< Roi >( curFAreas_.size() );
        for (FiducialArea fArea : curFAreas_) {
            originalSearchAreas_.add(fArea.getTrackSearchArea());           
        }
        curIP_ = curFAreas_.get(0).getImagePlus();
        curFAreaProcessor_ = curFAreas.get(0).getFiducialAreaProcessor();
        
        endKeyPlacedIndicator_.set(false);
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
    public FiducialMoveFinder( List<FiducialArea> prevFAreas, ImagePlus curIP, 
                                int maxMissingFrames, int minNumFiducialsForTrack,
                                String acquisitionTitle ) {
        
        acquisitionTitle_ = acquisitionTitle;
        prevFAreas_ = prevFAreas;
        numAreas_ = prevFAreas.size();
        numCentralMatchesThreshold_ = minNumFiducialsForTrack;
        failLimitRatio_ = ((double) numCentralMatchesThreshold_)/numAreas_;
        
        //Shift Traits
        curIP_ = curIP;
        curFAreaProcessor_ = prevFAreas_.get(0).getFiducialAreaProcessor();
        curFAreaProcessor_.enableAsyncProcessing(false);  //Make Sure that We Are not Async Processing in Constructor
        
        originalSearchAreas_ = new ArrayList< Roi >( prevFAreas_.size() );
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
                curFAreas_.add(new FiducialArea(curIP_, trackArea, fArea));
            /*} else {
                ReportingUtils.showError( "There was a null selected Spot");
            }*/
        }
        
        endKeyPlacedIndicator_.set(false);
        maxMissingFrames_ = maxMissingFrames;
        
    }
    
    /*
    *   Returns the matches_ list, a synchronized list
    *   Note, modifying operations on this list should be carefully synchronized
    *    if correlateDifferences() is running in another thread
    */
    public List< TravelMatchCase > getMatchList() {
        return matches_;
    }
    
    public TravelMatchCase getSelectedMatch() {
        return selectedMatch_;
    }
    
    //Cue up Threads
    public TravelMatchCase CorrelateDifferences() {
        //ij.IJ.log( "Starting New Match");
        //For the sake of resources, don't create extra threads just yet
        try {
            shiftedFiducialAreas_.put(curFAreas_);
            //Add poison for first evaluation
            shiftedFiducialAreas_.put( new ArrayList<FiducialArea>() );
        }  catch ( InterruptedException ex ) {
            ij.IJ.log("No Correlation Performed, Thread Queue Put Interrupted " + Thread.currentThread().getName());
            Thread.currentThread().interrupt(); //Reinterrupt
            return null;  //return without any processing if there is an error in Queueing
        }
        SpotMatchThread[] matchThreads = new SpotMatchThread[8];
        matchThreads[0] = new SpotMatchThread( prevFAreas_, shiftedFiducialAreas_, matches_, intensityDiminishRatio_,
                                failLimitRatio_, maxMissingFrames_, endKeyPlacedIndicator_,
                                curMaxFromRealMatchRatio_, curMaxFromVirtualMatchRatio_, acquisitionTitle_ );
        
        matchThreads[0].init();
        matchThreads[0].join();
        
        //TEMPORARY FIX UNTIL BETTER EVALUATION DETERMINED FOR EXTRA TRACK WINDOWS
        // Store Central Ratio and Central Indices
        centralMatches_ = matches_.size();
        centralfromRealMatchRatio_ = curMaxFromRealMatchRatio_.get();
        centralfromVirtualMatchRatio_ = curMaxFromVirtualMatchRatio_.get();
  
        if( matches_.size() > 0 ) {
        //100% match is assumed to be the best currently
        //  Currently, we don't have a tolerance for searching a blink
        /*ij.IJ.log( "Central Match = " + centralfromRealMatchRatio_ + " and Virt = " + centralfromVirtualMatchRatio_ + "\n For number of: " + numAreas_ );
        for( TravelMatchCase cas : matches_ ) {
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
            numRealMatches = (int) (centralfromRealMatchRatio_ * matches_.get(0).getMaxPossibleRealMatches());
            numVirtualMatches = (int) (centralfromVirtualMatchRatio_ * matches_.get(0).getMaxPossibleVirtualMatches());
        
        //Remove Poison Key
        shiftedFiducialAreas_.remove();
        endKeyPlacedIndicator_.set(true);  //Reset in the event of a starting thread hanging        
        
        // 3 Conditions based on uncertainties - 1 spot left as a total ( only use central spot)
        //   Both from real and from virtual spots are ideal
        //   The sum of matches (from virtual and reals is above an assumed minimum matches threshold)
        if( (centralfromRealMatchRatio_ == 1 && centralfromVirtualMatchRatio_ == 1) 
                ||  numRealMatches + numVirtualMatches >= numCentralMatchesThreshold_ ) {
            //If there are multipled Values, choose the one with the most even intensity change ratios
            try {
                //ij.IJ.log( "The Current Match Ratio is " + centralfromRealMatch_ + "\n" );
                selectedMatch_ = SelectMatch();
                ApplySelectedMatchCase(selectedMatch_);
                matchThreads[0].stopImmediate();
                return selectedMatch_;
            } catch ( EmptyStackException ex ) {
                ex.printStackTrace();
                //Continue on, and try to recover with other Fiducials with less ridiculous intensities
                ReportingUtils.showError("Somehow Mishandled Selection of MatchCase");
            }
        }
        }
        else {
            ij.IJ.log( "No Matches Found!");
        }
        
        //Current Multiple Area Result Does not have enough metrics to warrant throwing out false positives
        /*
        ij.IJ.log( "Spawning New Threads");
        
        //If Not, Initialize Threads for Shifted Area search
        //They will block on shiftedFiducialAreas_
        for( int i = 1; i < matchThreads.length; ++i ) {
            matchThreads[i] = new SpotMatchThread( prevFAreas_, shiftedFiducialAreas_, matches_, intensityDiminishRatio_,
                                failLimitRatio_, maxMissingFrames_, endKeyPlacedIndicator_,
                                curMaxFromRealMatchRatio_, curMaxFromVirtualMatchRatio_, acquisitionTitle_ );
            matchThreads[i].init();
        }
        
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
                        shiftedFiducialAreas_.put(createShiftedList(-x, y));
                        //North
                        shiftedFiducialAreas_.put(createShiftedList(0, y));
                        //NorthEast
                        shiftedFiducialAreas_.put(createShiftedList(x, y));
                        //East
                        shiftedFiducialAreas_.put(createShiftedList(x, 0));
                        //SouthEast
                        shiftedFiducialAreas_.put(createShiftedList(x, -y));
                        //South
                        shiftedFiducialAreas_.put(createShiftedList(0, -y));
                        //SouthWest
                        shiftedFiducialAreas_.put(createShiftedList(-x, -y));
                        //West
                        shiftedFiducialAreas_.put(createShiftedList(-x, 0));
                    }
                }
            }

            //Empty List As Indicator to finish (poison)
            shiftedFiducialAreas_.put(new ArrayList<FiducialArea>());

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();  //Pass Interrupt Along
        }
        
        //Block 
        for( int i = 0; i < matchThreads.length; ++i ) {
            matchThreads[i].join();
            if( !endKeyPlacedIndicator_.get() ) {
                //Somehow Blocking Queue was full or misbehaving
                //Manually Stop All Threads
                for( int stopIdx = i; stopIdx < matchThreads.length; ++stopIdx ) {
                    //Calls interrupt on Processes, early enough that join can wait
                    matchThreads[ stopIdx ].stopImmediate();
                }
                //We have effectively placed each endkey for the threads
                endKeyPlacedIndicator_.set(true);
            }
            matchThreads[i] = null;
        }
        
        //curMatchRatio should be 100% but in the event it's not,
        //Pick The highest match with the most uniform intensity ratios
        try {
            selectedMatch_ = SelectMatch();
            ApplySelectedMatchCase(selectedMatch_);
            return selectedMatch_;
        } catch (EmptyStackException ex) {
            ex.printStackTrace();
            ReportingUtils.showError("There were no matches with adequate Intensity Distributions");
        }*/
        
        //Need to decide what to do with a null result
        return null;
    }
    
    private List< FiducialArea > createShiftedList( int xWidthShifts, int yWidthShifts ) {
        
        List< FiducialArea > shiftedFAreaList = new ArrayList<FiducialArea>();
        Roi copyRoi;
        Roi origArea;
        for( FiducialArea fArea : prevFAreas_ ) {
            origArea = fArea.getTrackSearchArea();
            copyRoi = new Roi( origArea.getXBase() + ( xWidthShifts * origArea.getFloatWidth() ),
                                origArea.getYBase() + ( yWidthShifts * origArea.getFloatHeight() ),
                                origArea.getFloatWidth(), origArea.getFloatHeight() );
            shiftedFAreaList.add( new FiducialArea( curIP_, copyRoi, fArea ) );
        }
        
        return shiftedFAreaList;
        
    }
    
    //Searches Match Set For Most Appropriate Match
    //Note:  Should only be called after threads have finished processing
    private TravelMatchCase SelectMatch( ) throws EmptyStackException {
        if( matches_.size() == 1 ) {
            return matches_.get(0);
        }
        
        //Temporary workaround for any central pieces that remain to only count them
        //May be changed when other metric is proven
        int numMatches;
        if( centralfromRealMatchRatio_ == curMaxFromRealMatchRatio_.get() &&
                centralfromVirtualMatchRatio_ == curMaxFromVirtualMatchRatio_.get() ) {
            numMatches = centralMatches_;
        } else {
            numMatches = matches_.size();
        }
        
        // Get the expected range from the previous Fiducial Areas (linear difference)
        double expectedZRange = calculatePreviousZRangeDifference();
       
        TravelMatchCase mCase;
        TravelMatchCase bestCase = null;
        double minIntRatioSigma = Double.MAX_VALUE, minZPlaneDefocus = Double.MAX_VALUE;
        double curIntRatioSigma;
        double curZPlaneStdDev;
        for( int i = 0 ; i < numMatches; ++i ) {
            mCase = matches_.get(i);
                 
            //In the Case of reentry permission misordering, filter any matches that are below criteria
            if (mCase.getFromRealMatchRatio() == curMaxFromRealMatchRatio_.get()
                    && mCase.getFromVirtualMatchRatio() == curMaxFromVirtualMatchRatio_.get()) {
                
                //If any case is within the diffraction limit, select it immediately
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
                matches_.remove(i);
                numMatches--;
            }
          
        }
        
        //If there is still no match, choose the minimum intensityStandardDeviation
        if( bestCase == null ) {
            for (int i = 0; i < numMatches; ++i) {
                mCase = matches_.get(i);
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
    
    //Takes All Real Values in the Match Case and produces standard deviation in the intensity Change
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
     *  and the max to produce a maximum difference expected for any matches.
     * 
     * @return the difference: (maxZi1 + uncertaintyi1) - (minZi2 - uncertaintyi2) 
     *                      (Can Be a zero value if there's one fiducial and no uncertainty)
     */
    private double calculatePreviousZRangeDifference( ) {
        //Calculate Z Focus Range from prevFiducial Areas
        double minZ = Double.MAX_VALUE;
        double maxZ = Double.MIN_VALUE;
        for( FiducialArea fArea : prevFAreas_ ) {
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
