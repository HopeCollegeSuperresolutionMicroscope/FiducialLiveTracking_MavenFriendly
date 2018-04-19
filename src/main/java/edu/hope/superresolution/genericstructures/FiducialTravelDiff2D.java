/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.models.FiducialArea;

/**
 *   Data Structure for storing the change in a Fiducial Travel (possible)
 *    xDiff_ and yDiff_ - delta in X and Y translation from previous Fiducial to spotRef_
 *    x and y Uncertainty_ - uncertainty in the translation
 *    spotRef_  -  the spot compared for translation
 *    areaOwnerRef_  - the Fiducial Area to which this spot belongs 
 * 
 * @author Justin Hanselman
 */
public class FiducialTravelDiff2D {
    
    
        public BoundedSpotData spotRef_;
        public FiducialArea areaOwnerRef_;
        public BoundedSpotData prevSpot_;
        
        public double xDiffs_;  //Difference In X coordinates (nm)
        public double yDiffs_;  //Difference in Y Coordinates (nm)
        public double intRatio_;  
        public double xUncertainty_;
        public double yUncertainty_;
        //Uncertainty to account for the fact that Gaussian Fits are constrainted to Rayleigh Criteria
        //These currently are AbbeLimit of reference focus plane spot + sigma in each direction for two spots
        public double xDiffractionUncertainty_;  
        public double yDiffractionUncertainty_;
        public double avgUncertaintyRatio_;
        
        //focus relative to starting "focus plane" (first Fiducial Area)
        public double zDistance_;
        public double zDistanceUncertainty_;
        
        public boolean fromVirtual_;
        public boolean toVirtual_;
        
        public FiducialTravelDiff2D( BoundedSpotData prevSpot, BoundedSpotData spotRef, FiducialArea areaOwnerRef ) {

            xDiffs_ = spotRef.getXCenter() - prevSpot.getXCenter();
            yDiffs_ = spotRef.getYCenter() - prevSpot.getYCenter();
            intRatio_ = spotRef.getIntensity() / prevSpot.getIntensity();
            //Use Linear Combination For Now of actual spots
            xUncertainty_ = spotRef.getSigma() + prevSpot.getSigma();
            yUncertainty_ = spotRef.getSigma() + prevSpot.getSigma();
            avgUncertaintyRatio_ = spotRef.getSigma() / prevSpot.getSigma();
            spotRef_ = spotRef;
            prevSpot_ = prevSpot;
            areaOwnerRef_ = areaOwnerRef;
            
            fromVirtual_ = prevSpot.isVirtual();
            toVirtual_ = spotRef.isVirtual();
            
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
            
            //Selected Spot Divided By 4 must be changed later for other geometries
            double dSigma = spotRef_.getWidth() / 4 - areaOwnerRef.getFocusPlaneSigmaRef();
            double refIdx = areaOwnerRef.getFiducialAreaProcessor().getMicroscopeModel().getObjectiveRefractiveIdx();
            double numAp = areaOwnerRef.getFiducialAreaProcessor().getMicroscopeModel().getNumericalAperture();
            zDistance_ = dSigma * refIdx / numAp;
            zDistanceUncertainty_ = Math.sqrt(Math.pow(areaOwnerRef.getFocusPlaneSigmaRefUncertainty(), 2)
                    + Math.pow(spotRef_.getSigma(), 2)) * refIdx / numAp;

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
        
        /*Static Function that provides Function-hinted Access to private Modified Copy constructor
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
                
}
