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
 * @author HanseltimeIndustries
 */
public class LinearDriftModel3D extends AbstractDriftModel {
      

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
    public LinearDriftModel3D( int frameNum, double xFromStartTranslation, double xTranslationUncertainty,
                                double yFromStartTranslation, double yTranslationUncertainty, double zFromRefTranslation,
                                double zTranslationUncertainty, iDriftModel.DriftUnits unit ) {
        super( frameNum, xFromStartTranslation, xTranslationUncertainty, yFromStartTranslation, yTranslationUncertainty,
                zFromRefTranslation, zTranslationUncertainty, new Point3D(0,0,0), new Point3D(0,0,0), new Point3D(0,0,0), unit );
    }

    /**
     * Generates a Pixel conversion of this driftModel.  If this DriftModel is already in units
     * {@link DriftUnits#pixels}, then the object is returned with nothing done to it.  Note, this means the Z distance is also
     * taken in terms of pixels as well.
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
        
        return new LinearDriftModel3D( getFrameNumber(), getXTranslation() / pixel, getXUncertainty() / pixel,
                                            getYTranslation() / pixel,  getYUncertainty() / pixel, 
                                            getZTranslation() / pixel, getZUncertainty()/pixel, iDriftModel.DriftUnits.pixels );
    }
    
}
