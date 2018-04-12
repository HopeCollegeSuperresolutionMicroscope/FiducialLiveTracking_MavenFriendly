/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.models.FiducialArea;
import edu.valelab.gaussianfit.data.SpotData;

/**
 *   Data Structure for storing the change in a Fiducial Travel (possible)
 *    xDiff_ and yDiff_ - delta in X and Y translation from previous Fiducial to spotRef_
 *    x and y Uncertainty_ - uncertainty in the translation
 *    spotRef_  -  the spot compared for translation
 *    areaOwnerRef_  - the Fiducial Area to which this spot belongs 
 * 
 * @author Microscope
 */
public class FiducialTravelDiff2D {
    
    
        /**
         * The Current spot that is being compared against the track reference spot
         */
        public BoundedSpotData spotRef_;
        /**
         * The FiducialArea that the Current spot belongs to.  (Useful for setting or reevaluating proximity of other spots)
         */
        public FiducialArea areaOwnerRef_;
        /**
         * The track reference spot against from which this travel seems to have occurred
         */
        public BoundedSpotData prevSpot_;
        
        /**
         * Difference in X Coordinates (nm)
         */
        public double xDiffs_;  //Difference In X coordinates (nm)
        /**
         * Difference in Y Coordinates (nm)
         */
        public double yDiffs_;  //Difference in Y Coordinates (nm)
        /**
         * The ratio of current spot intensity/track reference spot intensity
         */
        public double intRatio_;  
        /**
         * The Uncertainty in the X Travel (TODO: Change from simple linear combination of uncertainties for tighter tolerances)
         */
        public double xUncertainty_;
        /**
         * The Uncertainty in the Y Travel (TODO: Change from simple linear combination of uncertainties for tighter tolerances)
         */
        public double yUncertainty_;
        //Uncertainty to account for the fact that Gaussian Fits are constrainted to Rayleigh Criteria
        //These currently are AbbeLimit of reference focus plane spot + sigma in each direction for two spots
        /**
         * The X Visual Resolving uncertainty (Abbe Limit) + any mathematical uncertainties in X coordinates
         * This is again, the uncerainties combined of the difference operation between reference and current spot
         */
        public double xDiffractionUncertainty_;  
        /**
         * The Y Visual Resolving uncertainty (Abbe Limit) + any mathematical uncertainties in X coordinates
         * This is again, the uncerainties combined of the difference operation between reference and current spot
         */
        public double yDiffractionUncertainty_;
        /**
         * The ratio of the current spots location uncertainty/track reference spot location uncertainty
         */
        public double avgUncertaintyRatio_;
        
        /**
         * Focus distance as scored relative to a reference plane.
         * @see edu.hope.superresolution.models.FiducialArea#getRefSigmaNaut()
         */
        public double zDistance_;
        
        /**
         * The uncertainty of the ZDistance relative to the Z distance of the reference
         */
        public double zDistanceUncertainty_;
        /**
         * If the travel reference spot was virtually created and was not the result of a physical detection
         */
        public boolean fromVirtual_;
        /**
         * If the current spot provided was virtually created and is not the result of a physical detection
         */
        public boolean toVirtual_;
        
        public FiducialTravelDiff2D( BoundedSpotData prevSpot, BoundedSpotData spotRef, FiducialArea areaOwnerRef ) {

            xDiffs_ = computeXTravelDiff(prevSpot, spotRef);//spotRef.getXCenter() - prevSpot.getXCenter();
            yDiffs_ = computeYTravelDiff(prevSpot, spotRef);//spotRef.getYCenter() - prevSpot.getYCenter();
            intRatio_ = FiducialTravelDiff2D.computIntensityRatio(prevSpot, spotRef);//spotRef.getIntensity() / prevSpot.getIntensity();
            //Use Linear Combination For Now of actual spots
            xUncertainty_ = FiducialTravelDiff2D.computeXTravelDiffUncertainty(prevSpot, spotRef);//spotRef.getSigma() + prevSpot.getSigma();
            yUncertainty_ = FiducialTravelDiff2D.computeYTravelDiffUncertainty(prevSpot, spotRef);//spotRef.getSigma() + prevSpot.getSigma();
            avgUncertaintyRatio_ = FiducialTravelDiff2D.computeAverageUncertaintyRatio(prevSpot, spotRef);//spotRef.getSigma() / prevSpot.getSigma();
            spotRef_ = spotRef;
            prevSpot_ = prevSpot;
            areaOwnerRef_ = areaOwnerRef;
            
            fromVirtual_ = prevSpot.isVirtual();
            toVirtual_ = spotRef.isVirtual();
            
            double assumedAbbeSpot;
            //To Generalize for Non-Copy (non-existent) case
            if( areaOwnerRef.getFocusPlaneSigmaRef() <=0 ) {
                //Sigma of Gaussian * 2
                assumedAbbeSpot = prevSpot_.getWidth()/2;
            } else {
                //Sigma * 2
                assumedAbbeSpot = areaOwnerRef.getFocusPlaneSigmaRef() * 2;
            }
            
            //Abbe Limit Disks (2 * sigma of a Gaussian Fit at focus plane)*2 + plus uncertainties
            xDiffractionUncertainty_ = xUncertainty_ + assumedAbbeSpot*2;
            yDiffractionUncertainty_ = yUncertainty_ + assumedAbbeSpot*2;
            
            //Selected Spot Divided By 4 must be changed later for other geometries
            double dSigma = spotRef_.getWidth() / 4 - areaOwnerRef.getFocusPlaneSigmaRef();
            double refIdx = areaOwnerRef.getFiducialAreaProcessor().getMicroscopeModel().getObjectiveRefractiveIdx();
            double numAp = areaOwnerRef.getFiducialAreaProcessor().getMicroscopeModel().getNumericalAperture();
            zDistance_ = FiducialTravelDiff2D.computeZLocationOfSpot(areaOwnerRef.getFocusPlaneSigmaRef(), refIdx, numAp, spotRef);//dSigma * refIdx / numAp;
            zDistanceUncertainty_ = FiducialTravelDiff2D.computeZLocationOfSpot(areaOwnerRef.getFocusPlaneSigmaRefUncertainty(), refIdx, numAp, spotRef);// Math.sqrt(Math.pow(areaOwnerRef.getFocusPlaneSigmaRefUncertainty(), 2)
                    //+ Math.pow(spotRef_.getSigma(), 2)) * refIdx / numAp;

        }
        
        //Modified Copy For Virtual Spots
        private FiducialTravelDiff2D( FiducialTravelDiff2D diffModel, BoundedSpotData refSpot, BoundedSpotData virtualSpot, 
                                        FiducialArea areaOwnerRef ) {
            
            prevSpot_ = refSpot;
            //Difference Model Parameters that are used (less arithmetic operators)
            xDiffs_ = diffModel.xDiffs_; //This was the model parameter used
            yDiffs_ = diffModel.yDiffs_;
            intRatio_ = diffModel.intRatio_;
            avgUncertaintyRatio_ = diffModel.avgUncertaintyRatio_;
            
            //Construct A Virtual Spot by Updating It to Match the diffModel
            spotRef_ = virtualSpot;
            toVirtual_ = virtualSpot.isVirtual();
            fromVirtual_ = refSpot.isVirtual();
            areaOwnerRef_ = areaOwnerRef;
            
            double assumedAbbeSpot;
            //To Generalize for Non-Copy (non-existent) case
            if( areaOwnerRef.getFocusPlaneSigmaRef() <=0 ) {
                assumedAbbeSpot = prevSpot_.getWidth()/2;
            } else {
                assumedAbbeSpot = areaOwnerRef.getFocusPlaneSigmaRef() * 2;
            }
            
            //Abbe Limit Disks (2 * sigma of a Gaussian Fit at focus plane)*2 + plus uncertainties
            xDiffractionUncertainty_ = xUncertainty_ + assumedAbbeSpot*2;
            yDiffractionUncertainty_ = yUncertainty_ + assumedAbbeSpot*2;
            
            //Copy the zDistance as from the other diff expected (this should not be used without aliasing)
            zDistance_ = diffModel.zDistance_;
            zDistanceUncertainty_ = diffModel.zDistanceUncertainty_;
        }
        
        /**Static Function that provides Function-hinted Access to private Modified Copy constructor
        *  Creates a FiducialTravelDiff2D with an average difference to a Virtual Spot
        *  @param diffModel - The FiducialTravelDiff2D whose average Values we want to replicate
        *  @param toShiftSpotModel - BoundedSpot that will be copied and modified to simulate it having moved according to diffModel
        *  @param areaOwnerRef - Fiducial Area that should apply this as it's selectedSpot 
        */
        public static FiducialTravelDiff2D createToVirtualTravelDiff2D( FiducialTravelDiff2D diffModel, BoundedSpotData toShiftSpotModel, 
                                        FiducialArea areaOwnerRef ) {
            //As A workaround, while we assume the same intensity fluxs, we will only use an intensity ratio to reduce the intensity
            double intensityRatio = ( diffModel.intRatio_ < 1 ) ? diffModel.intRatio_ : 1;
            BoundedSpotData virtualSpot = BoundedSpotData.createVirtualSpot(toShiftSpotModel, diffModel.xDiffs_, diffModel.yDiffs_, intensityRatio, diffModel.avgUncertaintyRatio_ );
            return new FiducialTravelDiff2D( diffModel, toShiftSpotModel, virtualSpot, areaOwnerRef );
        }
        
        
        //Currently, exposing all computation methods as static variables to allow for recalculation
        //This will need to be reworked if more data structures are introduced (TEMPORAY SOLUTION)
        public static double computeXTravelDiffUncertainty( SpotData prevSpot, SpotData curSpot ) {
            return curSpot.getSigma() + prevSpot.getSigma();
        }
        
        public static double computeYTravelDiffUncertainty( SpotData prevSpot, SpotData curSpot ) {
            return curSpot.getSigma() + prevSpot.getSigma();
        }
        
        public static double computeXTravelDiff( SpotData prevSpot, SpotData curSpot ) {
            return curSpot.getXCenter() - prevSpot.getXCenter();
        }
        
        public static double computeYTravelDiff( SpotData prevSpot, SpotData curSpot ) {
            return curSpot.getYCenter() - prevSpot.getYCenter();
        }
        
        public static double computIntensityRatio( SpotData prevSpot, SpotData curSpot ) {
            return curSpot.getIntensity() / prevSpot.getIntensity();
        }
        
        public static double computeAverageUncertaintyRatio( SpotData prevSpot, SpotData curSpot ) {
            return curSpot.getSigma() / prevSpot.getSigma();
        }
        
        /**
         * Used to calculate the approximate Z Distance of a spot relative to given InFocus sigma of a Gaussian PSF
         * <p>
         * TODO: This type of logic needs to be the result of an interface for expansion of methods
         */
        public static double computeZLocationOfSpot( double inFocusRefSigma, double refIdx, double numAp, SpotData curSpot ) { 
            //Selected Spot Divided By 4 must be changed later for other geometries
            double dSigma = curSpot.getWidth() / 4 - inFocusRefSigma;
            return dSigma * refIdx / numAp;
        }
        
        /**
         * Simple combination of uncertainties of a given Focus Reference Sigma of a Gaussian PSF and 
         * the currentSpot's uncertainties for a Z Distance
         * <p>
         * TODO: This type of logic needs to be the result on an interface for expansion of methods
         * <p>
         * TODO2: This should be a same operation from ComputeZLocation of Spot for Efficiency
         * @param inFocusRefSigmaUncertainty
         * @param refIdx
         * @param numAp
         * @param curSpot
         * @return 
         */
        public static double computeZLocationUncertainty( double inFocusRefSigmaUncertainty, double refIdx, double numAp, SpotData curSpot ) {
            return Math.sqrt(Math.pow(inFocusRefSigmaUncertainty, 2)
                    + Math.pow(curSpot.getSigma(), 2)) * refIdx / numAp;
        }
}
