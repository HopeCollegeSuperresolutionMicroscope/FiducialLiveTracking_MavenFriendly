/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import edu.hope.superresolution.genericstructures.AbstractDriftModel;
import edu.hope.superresolution.genericstructures.iDriftModel;
import javafx.geometry.Point3D;

/**
 *
 * Basic Class that holds data for Linear 2 Dimensional Drift of an Image Relative to a reference Image.
 * This is a final class in function, as the values are only set via initialization since drift is something that should change with images,
 * rather than multiple times for the same image.  Extending this class may deal with edge cases, such as
 * changing a reference frame, if the context requires such a changeable model.
 * 
 * @author Justin Hanselman
 */
public class LinearDriftModel2D extends AbstractDriftModel {

    /**
     * One-time initialization of members.  Final in nature, but class remains non-final for extensions.
     * 
     * @param frameNum
     * @param xFromStartTranslation
     * @param xTranslationUncertainty
     * @param yFromStartTranslation
     * @param yTranslationUncertainty
     * @param unit 
     */
    public LinearDriftModel2D( int frameNum, double xFromStartTranslation, double xTranslationUncertainty, double yFromStartTranslation, double yTranslationUncertainty, DriftUnits unit ) {
        super( frameNum, xFromStartTranslation, xTranslationUncertainty, yFromStartTranslation, yTranslationUncertainty,
                0, 0, new Point3D(0,0,0), new Point3D(0,0,0), new Point3D(0,0,0), unit );
    }

    /**
     * Generates a Pixel conversion of this driftModel.  If this DriftModel is already in units
     * {@link DriftUnits#pixels}, then the object is returned with nothing done to it.
     * 
     * @param pixelSize
     * @param unit
     * @return 
     */
    @Override
    public iDriftModel generatePixelConversion(int pixelSize, DriftUnits unit) {
        
        DriftUnits instanceUnits = getDriftUnits();
        
        if( instanceUnits == iDriftModel.DriftUnits.pixels || unit == iDriftModel.DriftUnits.pixels ) {
            return this;
        }
        
        double pixel = (unit.getUnitScaleFactor(instanceUnits) * pixelSize);
        
        return new LinearDriftModel2D( getFrameNumber(), getXTranslation() / pixel, getXUncertainty() / pixel,
                                            getYTranslation() / pixel,  getYUncertainty() / pixel,  iDriftModel.DriftUnits.pixels );
        
    }
    
    
    
}
