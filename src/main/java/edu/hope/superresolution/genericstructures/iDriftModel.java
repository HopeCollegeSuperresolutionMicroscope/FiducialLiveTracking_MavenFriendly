/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import java.awt.Point;
import java.awt.geom.Point2D;
import javafx.geometry.Point3D;
import org.apache.commons.math.geometry.Vector3D;

/**
 *  Interface for retrieving Drift Information about a given Image Frame relative to some reference frame.
 *  It is not the concern of this interface model, what that reference frame is, since drift is always
 *  context pertinent, DriftModels are not expected to be stored or accessed without an understanding 
 *  of their context.
 * 
 *  For the sake of Generality, this drift model assumes 3-axis drift and rotation for 
 *  a given frame.  Simplified extending classes such as 2DLinearDriftModel, reports 0 for all but X and Y translational elements.
 * 
 * @author Justin Hanselman
 */
public interface iDriftModel {
    
    /**
     * Enumerated Values for Translational Drift units for an image.  Used to indicate how this DriftModel has stored its data.
     */
    public enum DriftUnits {
        nm,
        um,
        mm,
        m,
        km,
        Mm,
        Gm,
        pixels,        
    }
    
    
    /**
     * Gets the Frame Number of the acquisition that this model applies to
     * 
     * Note: Frame is originally intended to mean each step in an ImageStack, however, extending classes can make
     * explicit rules as long as they have a means of ensuring them
     * 
     * @return Frame Number (1-Base)
     */
    public int getFrameNumber();
    
    /**
     * Gets the X Translation that this frame has drifted from the start Frame in the corresponding unit.
     * 
     * @return Distance that this current frame has translated (in iDriftUnits)
     * @see DriftUnits
     * @see iDriftModel#getDriftUnits()
     */
    public double getXFromStartTranslation();
     
     /**
     * Gets the Uncertainty associated with the X Translation of the frame
     * @return +/- Uncertainty in (DriftUnits)
     */
    public double getXTranslationUncertainty();
    
     /**
     * Gets the Y Translation that this frame has drifted from the start Frame in the corresponding unit.
     * 
     * @return Distance that this current frame has translated (in DriftUnits)
     * @see DriftUnits
     * @see iDriftModel#getDriftUnits()
     */
    public double getYFromStartTranslation();
    
    /**
     * Gets the Uncertainty associated with the Y Translation of the frame
     * @return +/- Uncertainty in (DriftUnits)
     */
    public double getYTranslationUncertainty();
    
    /**
     * Gets the Z Translation that this frame has drifted from the start Frame in the corresponding unit.
     * 
     * @return Distance that this current frame has translated (in iDriftUnits)
     * @see DriftUnits
     * @see iDriftModel#getDriftUnits()
     */
    public double getZFromStartTranslation();
    
    /**
     * Gets the Uncertainty associated with the Z Translation of the frame
     * @return +/- Uncertainty in (DriftUnits)
     */
    public double getZTranslationUncertainty();
    
    /**
     * Gets the Drift Units for the Translation
     * 
     * @return 
     */
    public DriftUnits getDriftUnits();
    
    /**
     * Gets the point of rotation (in Drift Units) for the frame's rotation.
     * <br>
     * Rules About Rotation Point:
     * <br>
     * The Axis Point assumes 3 orthogonal axes along X and Y rows and columns in the pixel array for the frame.
     * Z is thus perpendicular and should obey Right-Hand Rule Mechanics.  This is best explained and 
     * measured against getZFromStartTranslation(), since a negative value for the frame Z drift with a positive axis point,
     * should mean that the axis is closer to the original reference frame.
     * <br>
     * The point 0,0,0 is located at the upper left corner of the image, and the Z used assumes 0 is the current frame
     * @return 
     */
    public Point3D getRotationAxisPoint();
    
    /**
     * Gets the Rotation Vector (XYZ) necessary for such drift (in degrees) in Euler Angles
     * 
     * @return A 3D Point representative of (X Euler Angle, Y Euler Angle, Z Euler Angle) from the axis established in getRotationAxisPoint
     */
    public Point3D getEulerRotations();
    
    /**
     * Gets the Uncerainty (sigmaX, sigmaY, sigmaZ) associated with each Rotation (in degrees)
     * @return 
     */
    public Point3D getEulerRotationUncertainty();
    
}
