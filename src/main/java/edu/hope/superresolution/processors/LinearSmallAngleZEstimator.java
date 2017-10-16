/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.processors;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.genericstructures.iZEstimator;
import edu.hope.superresolution.models.MicroscopeModel;

/**
 * As Detailed in a supplement (TO BE PROVIDED) this estimator determines the variation in
 * the Gaussian sigma from an "in focus" minimum to the current and attributes it to 
 * expansion of the beam cone.  This Estimator assumes the small angle approximation of 
 * sin(theta)~tan(theta).  The Estimator itself will still produce comparable behavior
 * when comparing z estimates to others made with the same estimate, given its linear
 * behavior.
 * <p>
 * For non-small angle assumption, the same comparative linear behavior occurs but
 * may be estimated with a higher degree of conceptual accuracy with the LinearTanZEstimator.
 * 
 * @see LinearTanZEstimator
 * @author Desig
 */
public class LinearSmallAngleZEstimator implements iZEstimator {

    private double numericalAperture_;
    private double refracIdx_;
    private final Object settingsLock_ = new Object();
    
    LinearSmallAngleZEstimator( MicroscopeModel microscope ) {
        numericalAperture_ = microscope.getNumericalAperture();
        refracIdx_ = microscope.getObjectiveRefractiveIdx();
    }

    /**
     * Really only sets NumericalAperture and Refractive Index due to the estimate
     * @param microscope 
     */
    @Override
    public void setMicroscopeProperties(MicroscopeModel microscope) {
        synchronized (settingsLock_) {
            numericalAperture_ = microscope.getNumericalAperture();
            refracIdx_ = microscope.getObjectiveRefractiveIdx();
        }
    }

    /**
     * Approximates sigma to 1/4 the Airy Disk Diameter, and uses the change in that dimension
     * as a qualifier for a zEstimate.
     * 
     * @param referenceSpot
     * @param currentSpot
     * @return 
     */
    @Override
    public ZPassBack calculateZEstimate(BoundedSpotData referenceSpot, BoundedSpotData currentSpot) {
        synchronized (settingsLock_) {
            double refSig = referenceSpot.getWidth() / 4;
            double curSig = currentSpot.getWidth() / 4;
            double refSigUncertainty = referenceSpot.getSigma();
            double curSigUncertainty = currentSpot.getSigma();

            double relativeZ = (curSig - refSig) * refracIdx_ / numericalAperture_;

            double relativeZUncertainty = Math.sqrt(Math.pow(refSigUncertainty, 2)
                    + Math.pow(curSigUncertainty, 2)) * refracIdx_ / numericalAperture_;
            return new ZPassBack(relativeZ, relativeZUncertainty);
        }
    }

    
}
