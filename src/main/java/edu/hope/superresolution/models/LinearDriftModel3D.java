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
 * @author HanseltimeIndustries
 */
public class LinearDriftModel3D implements iDriftModel {
       
    private int frameNum_;
    private double xFromStartTranslation_;
    private double xTranslationUncertainty_;
    private double yFromStartTranslation_;
    private double yTranslationUncertainty_;
    private double zFromRefTranslation_;
    private double zTranslationUncertainty_;
    private iDriftModel.DriftUnits unit_;
    
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
    public LinearDriftModel3D( int frameNum, double xFromStartTranslation, double xTranslationUncertainty,
                                double yFromStartTranslation, double yTranslationUncertainty, double zFromRefTranslation,
                                double zTranslationUncertainty, iDriftModel.DriftUnits unit ) {
        frameNum_ = frameNum;
        xFromStartTranslation_ = xFromStartTranslation;
        xTranslationUncertainty_ = xTranslationUncertainty;
        yFromStartTranslation_ = yFromStartTranslation;
        yTranslationUncertainty_ = yTranslationUncertainty;
        zFromRefTranslation_ = zFromRefTranslation;
        zTranslationUncertainty_ = zTranslationUncertainty;
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
        return zFromRefTranslation_;
    }

    @Override
    public double getZTranslationUncertainty() {
        return zTranslationUncertainty_;
    }

    @Override
    public iDriftModel.DriftUnits getDriftUnits() {
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
