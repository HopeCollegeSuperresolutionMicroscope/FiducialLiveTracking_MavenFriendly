/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import java.util.concurrent.locks.ReentrantLock;

/**
 *
 *  Used to Store Various Attributes of a Microscope Physical System 
 * 
 * @author Justin Hanselman
 */
public class MicroscopeModel extends ModelUpdateDispatcher {
    
    //boolean to determine if this is an unmodified nullary constructor
    private boolean isNull_;
    
    private double numericalAperture_;  //The Numerical aperture of an Objective
    private double objectiveRefractiveIdx_; //The index of refraction for a given idx
    private double objectiveMagnification_; //The magnification of the objective
    private double eyePieceMagnification_; //The Magnification of the eye piece past the intermediary plane
    private double refTubeLength_;  //the tubelength for the intermediate Image
    //Infinity Corrected Objectives
    private boolean isInfinityObjective_;  //Indicates whether the objective is Inifinity Corrected, 
    private double opticalTubeFocalLength_; //This is the actual focal Length for Infinity Corrected Systems that Places the intermediate Image
    
    //Lock for copying
    private final ReentrantLock copyLock_ = new ReentrantLock();
    
    /**
     * Nullary Constructor 
     */
    MicroscopeModel() {
        numericalAperture_ = 0;
        objectiveRefractiveIdx_ = 0;
        objectiveMagnification_ = 0;
        refTubeLength_ = 0;
        isInfinityObjective_ = false; 
        opticalTubeFocalLength_ = 0;
        eyePieceMagnification_ = 1;
        isNull_ = true;
    }
    
    /**
     * General Constructor
     * 
     * @param numericalAperture - the numericalAperture of the device
     * @param objectiveRefractiveIdx - the refractive index between objective and slide (important for immersion objectives)
     * @param objectiveMagnification - The Magnification as specified by the objective manufacturer
     * @param refTubeLength - The Manufacturer reference tube Length (optical or Mechanical) for the intermediate image
     * @param isInfinityObjective - Whether of not the Objective is Infinity Corrected
     * @param opticalTubeFocalLength - Specified Optical Tube Focal Length (only relevant if infinityCorrected)
     * @param eyePieceMagnification - The Magnification of any eyePiece Between the Camera and Intermediary Plane (1 indicates none)
     */
    public MicroscopeModel( double numericalAperture, double objectiveRefractiveIdx, 
                      double objectiveMagnification, double refTubeLength, boolean isInfinityObjective,
                      double opticalTubeFocalLength, double eyePieceMagnification ) {
        numericalAperture_ = numericalAperture;
        objectiveRefractiveIdx_ = objectiveRefractiveIdx;
        objectiveMagnification_ = objectiveMagnification;
        refTubeLength_ = refTubeLength;
        isInfinityObjective_ = isInfinityObjective;
        //Infinity Corrected Objectives have opticalTubeLengths that can vary and affect magnification
        if( isInfinityObjective ) {
            opticalTubeFocalLength_ = opticalTubeFocalLength;
        }
        //If there is a negative or 0 magnification, revert to 1
        if( eyePieceMagnification <= 0 ) {
            eyePieceMagnification = 1;
        }
        eyePieceMagnification_ = eyePieceMagnification;
        isNull_ = false;
    }
    
    /**
     * General Constructor - Whether Optical Tube Focal Length is the same as the RefTube Length if the system is Infinity Corrected
     * (Implements General Constructor where opticalTubeFocalLength = refTubeLength)
     * 
     * @param numericalAperture - the numericalAperture of the device
     * @param objectiveRefractiveIdx - the refractive index between objective and slide (important for immersion objectives)
     * @param objectiveMagnification - The Magnification as specified by the objective manufacturer
     * @param refTubeLength - The Manufacturer reference tube Length (optical or Mechanical) for the intermediate image
     * @param isInfinityObjective - Whether of not the Objective is Infinity Corrected
     * @param eyePieceMagnification - The Magnification of any eyePiece Between the Camera and Intermediary Plane (1 indicates none)
     * 
     * @see #MicroscopeModel(double, double, double, double, boolean, double) 
     */
    public MicroscopeModel( double numericalAperture, double objectiveRefractiveIdx, 
                                double objectiveMagnification, double refTubeLength, 
                                boolean isInfinityObjective, double eyePieceMagnification ) {
        this( numericalAperture, objectiveRefractiveIdx, objectiveMagnification, refTubeLength,
                            isInfinityObjective, refTubeLength, eyePieceMagnification );
    }
    
    /**
     * Copy Constructor
     * 
     * @param sourceObj - The source MicroscopeModel to copy
     */
    public MicroscopeModel( MicroscopeModel sourceObj ) {
        sourceObj.copyLock_.lock();
        try {
            eyePieceMagnification_ = sourceObj.eyePieceMagnification_;
            isInfinityObjective_ = sourceObj.isInfinityObjective_;
            numericalAperture_ = sourceObj.numericalAperture_;
            objectiveMagnification_ = sourceObj.objectiveMagnification_;
            objectiveRefractiveIdx_ = sourceObj.objectiveRefractiveIdx_;
            opticalTubeFocalLength_ = sourceObj.opticalTubeFocalLength_;
            refTubeLength_ = sourceObj.refTubeLength_;
            isNull_ = sourceObj.isNull_;
        } finally {
            sourceObj.copyLock_.unlock();
        }
    }
    
    /**
     *  Set the numerical Aperture of the MicroscopeModel Instance
     * 
     * @param numericalAperture - the numericalAperture of the microscope this corresponds to
     */
    public void setNumericalAperture( double numericalAperture ) {
        copyLock_.lock();
        try {
            isNull_ = false;
            numericalAperture_ = numericalAperture;
        } finally {
            copyLock_.unlock();
        }
    }
    
    /**
     *  Get the numerical Aperture of the MicrscopeModel Instance
     * 
     * @return - numerical Aperture
     */
    public double getNumericalAperture( ) {
        return numericalAperture_;
    }
    
    /**
     *  Set the objective Refractive Index for the medium between slide and objective for the MicroscopeModel Instance
     * 
     * @param objectiveRefractiveIdx - the refractive index between objective and slide (important for immersion objectives)
     */
    public void setObjectiveRefractiveIdx( double objectiveRefractiveIdx ) {
        copyLock_.lock();
        try {
            isNull_ = false;
            objectiveRefractiveIdx_ = objectiveRefractiveIdx;
        } finally {
            copyLock_.unlock();
        }
    }
    
    /**
     *  Get the objective Refractive Index of the medium between slide and objective for the MicroscopeModel Instance
     * 
     * @return - the refractive Index of the medium between the slide and objective
     */
    public double getObjectiveRefractiveIdx( ) {
        return objectiveRefractiveIdx_;
    }
    
    /**
     * Set the magnification of the objective of the MicroscopeModel Instance
     * 
     * @param objectiveMagnification - the magnification of the objective as specificed by the objective manufacturer
     */
    public void setObjectiveMagnification( double objectiveMagnification ) {
        copyLock_.lock();
        try {
            isNull_ = false;
            objectiveMagnification_ = objectiveMagnification;
        } finally {
            copyLock_.unlock();
        }
    }
    
    /**
     * Get the magnification of the objective for the MicroscopeModel Instance
     * 
     * @return - the magnification of the objective as specified by the objective manufacturer
     */
    public double getObjectiveMagnification() {
        return objectiveMagnification_;
    }
    
    /**
     *  Set the reference Tube Length (As Specified by the Manufacturer) for the Microscope Model Instance
     * <p>
     * This Tube Length is used dually as the mechanical Tube Length for traditional objectives
     * and as the optical tube length for infinity corrected systems.
     * 
     * @param refTubeLength - the reference Tube Length for the Intermediate Image as Specified by the Manufacturer 
     */
    public void setRefTubeLength( double refTubeLength ) {
        copyLock_.lock();
        try {
            isNull_ = false;
            refTubeLength_ = refTubeLength;
        } finally {
            copyLock_.unlock();
        }        
    }
    
    /**
     * Get the reference Tube Length (As Specified by the Manufacturer) for the Microscope Model Instance
     * <p>
     * This Tube Length is used dually as the mechanical Tube Length for traditional objectives
     * and as the optical tube length for infinity corrected systems.
     * 
     * @return - the reference Tube Length for the Intermediate Image as Specified by the Manufacturer 
     */
    public double getRefTubeLength( ) {
        return refTubeLength_;
    }
   
    /**
     * Set Whether or not the Objective is an infinity corrected Objective.
     * 
     * @param isInfinityObjective - if the objective is infinity corrected
     * @param refTubeLength - The Reference Tube Length of The Objective
     * @param manufacturerMagnification - The Magnification of the Objective
     * @param opticalTubeFocalLength - The Optical Tube Length if the Objective is Infinity Corrected
     */
    public void setIsInfinityCorrectedObjective( boolean isInfinityObjective, double refTubeLength,
                                            double manufacturerMagnification, double opticalTubeFocalLength ) {
        copyLock_.lock();
        try {
            isNull_ = false;
            isInfinityObjective_ = isInfinityObjective;
            refTubeLength_ = refTubeLength;
            objectiveMagnification_ = manufacturerMagnification;
            if (isInfinityObjective_) {
                opticalTubeFocalLength_ = opticalTubeFocalLength;
            }
        } finally {
            copyLock_.unlock();
        }       
    }
    
    /**
     *  Get Whether or not the Objective is Infinity Corrected
     * 
     * @return - <code>true</code> if the objective is infinity corrected
     */
    public boolean getIsInfinityCorrectedObjective( ) {
        return isInfinityObjective_;
    }
    
    /**
     * Set the OpticalTubeFocal Length if the Microscope is Infinity Corrected otherwise 
     *  non-effective
     * 
     * @param opticalTubeFocalLength - the Actual Optical Tube Focal Length
     */
    public void setOpticalTubeFocalLength( double opticalTubeFocalLength ) {
        copyLock_.lock();
        try {
            if (isInfinityObjective_) {
                isNull_ = false;
                opticalTubeFocalLength_ = opticalTubeFocalLength;
            }
        } finally {
            copyLock_.unlock();
        }    
    }
    
    /**
     * Get The Optical Tube Focal Length (Only Non-zero, if the objective is infinity corrected)
     * 
     * @return - returns the optical tube focal length for an infinity corrected objective or 0 otherwise
     */
    public double getOpticalTubeFocalLength( ) {
        if( isInfinityObjective_ ) {            
            return opticalTubeFocalLength_;
        }
        
        return 0;
    }
    
    /**
     * Gets the total Magnification of the microscope to the imaging Plane.
     * <p>
     * This Considers any change in opticalTubeFocalLength for infinity corrected 
     *  objectives, as well as any eyePiece Magnification placed between the 
     *  intermediate image and the image plane.
     * 
     * @return 
     */    
    public double GetTotalMagnification( ) {
        double magnification = 1;
        copyLock_.lock();
        try {
            if (isInfinityObjective_) {
                magnification = objectiveMagnification_ * opticalTubeFocalLength_ / refTubeLength_;
            } else {
                magnification = objectiveMagnification_;
            }

            magnification *= eyePieceMagnification_;
        } finally {
            copyLock_.unlock();
        }      
        
        return magnification;
    }

    /**
     *  Sets the Magnification of an eyePiece Placed between the Camera and the intermediate image.
     * <p>
     *  Passively sets magnification to 1 if the given number is less than or equal to 0.
     * 
     * @param eyePieceMagnification - The Magnification of any eyePiece Between the Camera and Intermediate Plane (1 indicates none)
     */
    public void setEyePieceMagnification( double eyePieceMagnification ) {
        copyLock_.lock();
        try {
            if (eyePieceMagnification <= 0) {
                isNull_ = false;
                eyePieceMagnification_ = eyePieceMagnification;
            }
        } finally {
            copyLock_.unlock();
        }
    }
    
    /**
     *  Get the EyePiece Magnification of any eyePiece between the camera and intermediate Image Plane
     * 
     * @return 
     */
    public double getEyePieceMagnification( ) {
        return eyePieceMagnification_;
    } 
    
    /**
     * Determines Whether or not the object was null initialized and has yet to have its member 
     * values altered.
     * <p>
     * Note: Any code using this, should set ALL attributes in the non-null constructor as 
     * a response.  This is here to avoid potential issues of passing nullary constructed 
     * code that needs some type of defaults added later, and passing a meaningful object
     * that should be used as is.
     * 
     * @return 
     */
    public boolean isNullEquivalent() {
        return isNull_;
    }
    
}

