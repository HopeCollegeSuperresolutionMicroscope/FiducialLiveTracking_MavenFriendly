/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

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
public class LinearDriftModel2D implements iDriftModel {
    
    private int frameNum_;
    private double xFromStartTranslation_;
    private double xTranslationUncertainty_;
    private double yFromStartTranslation_;
    private double yTranslationUncertainty_;
    private DriftUnits unit_;
    
    //final variables for the sake of filler
    private final Point3D nonRotations_ = new Point3D(0,0,0);

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
        frameNum_ = frameNum;
        xFromStartTranslation_ = xFromStartTranslation;
        xTranslationUncertainty_ = xTranslationUncertainty;
        yFromStartTranslation_ = yFromStartTranslation;
        yTranslationUncertainty_ = yTranslationUncertainty;
        unit_ = unit;
    }
    
    @Override
    public int getFrameNumber() {
        return frameNum_;
    }

    @Override
    public double getXFromStartTranslation() {
        return xFromStartTranslation_;
    }

    @Override
    public double getXTranslationUncertainty() {
        return xTranslationUncertainty_;
    }

    @Override
    public double getYFromStartTranslation() {
        return yFromStartTranslation_;
    }

    @Override
    public double getYTranslationUncertainty() {
        return yTranslationUncertainty_;
    }

    @Override
    public double getZFromStartTranslation() {
        return 0;
    }

    @Override
    public double getZTranslationUncertainty() {
        return 0;
    }

    @Override
    public DriftUnits getDriftUnits() {
        return unit_;
    }

    @Override
    public Point3D getRotationAxisPoint() {
        return nonRotations_;
    }

    @Override
    public Point3D getEulerRotations() {
        return nonRotations_;
    }

    @Override
    public Point3D getEulerRotationUncertainty() {
        return nonRotations_;
    }
    
    
    
}
