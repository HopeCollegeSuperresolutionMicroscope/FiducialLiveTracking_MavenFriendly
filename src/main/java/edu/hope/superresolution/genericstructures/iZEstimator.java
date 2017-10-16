/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.models.MicroscopeModel;

/**
 *  Interface for implementing Z estimates.  This Class assumes two forms of input.
 *   A comparative reference point passed in the form of SpotData, and a current 
 *   point for calculation.  Note, that the reference point may be a fixed point and as such, 
 *   may disregard any references passed in. 
 *  <p>
 *  Intended Application: A FiducialLocationModel Represents a selection of multiple 
 *  FiducialAreas, in which, each area tries to maintain the same singular selected
 *  fiducial based on lateral tracking.  Since these fiducials may be situated at different 
 *  Z-planes for the same image, each FiducialArea maintains its own reference plane 
 *  (the in focus value for the Fiducial) and compares a current fiducial in a later frame
 *  to the reference to calculate a z-estimate.  As stated above, a fixed reference point 
 *  may be assumed, and therefore the reference discarded, but this should only be 
 *  done if the model can somehow safely assume the tolerances and variations among 
 *  measurement and calculation.
 * 
 * @author Desig
 */
public interface iZEstimator {
    
    /**
     * Inner PassBackClass For iZEstimator
     */
    public class ZPassBack {
        
        private final double zValue_;
        private final double zUncertainty_;
        
        public ZPassBack( double zValue, double zUncertainty ) {
            zValue_ = zValue;
            zUncertainty_ = zUncertainty;
        } 
        
        public double getZValue() {
            return zValue_;
        }
        
        public double getZUncertainty() {
            return zUncertainty_;
        }
        
    }
    
    /**
     * Sets any microscope Properties in the extending Class that might be relevant to the estimate.
     * 
     * @param microscope - the current microscopeModel to use
     */
    public void setMicroscopeProperties( MicroscopeModel microscope );
    
    /**
     * 
     * This should calculate the ZEstimate given a referenceSpot and a currentSpot.  If the reference is
     * non-external in nature, calculations may neglect its use.  It is highly recommended 
     * that estimators take the reference value into account, however, as a measured point
     * of reference.
     * <p>
     * Note: a reference may be passed in that is null.  If that is the case, treat that 
     * reference as being undetermined and/or not available.  The CurrentSpot is its own reference.
     * 
     * @param referenceSpot
     * @param currentSpot 
     * @return The Current ZEstimate and its uncertainty wrapped in the ZPassBack object.
     * 
     * @see #ZPassBack
     */
    public ZPassBack calculateZEstimate( BoundedSpotData referenceSpot, BoundedSpotData currentSpot );
    
}
