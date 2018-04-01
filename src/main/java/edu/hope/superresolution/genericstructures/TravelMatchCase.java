/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import java.util.ArrayList;
import java.util.List;

/**
 *   Structure Used to Characterize A Travel Match
     spotMatches_ contains the FiducialTravelDiff2D's that matched properties
     realMatchRatio_  the percentage( out of 1 ) of matches to the Fiducial Area Set
     maxSpotMatches_ the max number (denominator for percentage calculation)
 *
 * @author Justin Hanselman
 */
public class TravelMatchCase {
    
    //Holds The FiducialTravelDiff2Ds that would correspond to a match
    // Can Have Real Matches and Matches that assume where a blink might be moved
    private final List< FiducialTravelDiff2D > matchSpots_;
    //Members correlating to matches that were found before and after
    private double fromRealMatches_;
    private double fromRealMatchRatio_;
    private final double maxFromRealMatches_;
    //Members correlating to matches from a virtual spot (blink across frames)
    private double fromVirtualMatches_;
    private double fromVirtualMatchRatio_;
    private final double maxFromVirtualMatches_;

    public TravelMatchCase(int numRealMatches, int numVirtMatches) {
        matchSpots_ = new ArrayList< FiducialTravelDiff2D>( numRealMatches + numVirtMatches );
        maxFromRealMatches_ = numRealMatches;
        maxFromVirtualMatches_ = numVirtMatches;
        if( maxFromRealMatches_ == 0 ) {
            //zero-division ratio set to 1
            fromRealMatchRatio_ = 1;
        } else {
            fromRealMatchRatio_ = 0;
        }
        if( maxFromVirtualMatches_ == 0 ) {
            //zero-division ratio set to 1
            fromVirtualMatchRatio_ = 1;
        } else {
            fromVirtualMatchRatio_ = 0;
        }
        fromRealMatches_ = 0;
        fromVirtualMatches_ = 0;
    }

    /*
    *   Adds a given FiducialTravelDiff2D to the spotMatch List
    *    The diff may be a real match when compared to either a virtual or real 
    *    start Fiducial.  It also may be a virtual recreation for where
    *    a Fiducial would be expected but wasn't found (blinked)
    */
    public void addMatch( FiducialTravelDiff2D diff ) {
        if( diff.toVirtual_ ) {
            //The diff is an assumed fiducial movement and is symbolic
            matchSpots_.add( diff );
        }
        else {
            if( diff.fromVirtual_ ) {
                //This is a Blink Object
                matchSpots_.add( diff );
                ++fromVirtualMatches_;
                //divide by zero guard
                if( maxFromVirtualMatches_ != 0 ) {
                    fromVirtualMatchRatio_ = fromVirtualMatches_/maxFromVirtualMatches_;
                } else {
                    //Set Infinite to 1
                    fromVirtualMatchRatio_ = 1;
                }
            }
            else {
                //This is a Static Fiducial
                matchSpots_.add( diff );
                ++fromRealMatches_;
                //divide by zero guard
                if( maxFromRealMatches_ != 0 ) {
                    fromRealMatchRatio_ = fromRealMatches_/maxFromRealMatches_;
                } else {
                    //Set Infinite to 1
                    fromRealMatchRatio_ = 1;
                }
            }
        }
    }
    
    /*
     *  Get the match Ratio of physically detected fiducials
     */
    public double getFromRealMatchRatio() {
        return fromRealMatchRatio_;
    }

    /*
     *  Get the match Ratio of virtually created (blink interval typically) Fiducials 
     */
    public double getFromVirtualMatchRatio() {
        return fromVirtualMatchRatio_;
    }

    public List< FiducialTravelDiff2D> getFiducialTranslationSpots() {
        return matchSpots_;
    }
    
    /**
     *  Gets the maximum number of Real Matches Possible
     * 
     * @return 
     */
    public double getMaxPossibleRealMatches() {
        return maxFromRealMatches_;
    }
    
    /**
     *  Get the maximum number of From Virtual Matches Returned
     * 
     * @return 
     */
    public double getMaxPossibleVirtualMatches() {
        return maxFromVirtualMatches_;
    }

}
