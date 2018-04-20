/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import com.hanseltime.loosebeans.annotations.BeanGetter;
import com.hanseltime.loosebeans.annotations.BeanProperty;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import javafx.geometry.Point3D;

/**
 * Base Abstract Class for Drift that exposes Basic Bean Behavior.
 * <p>
 * Carries with it opencsv annotations for writing to csv.
 * 
 * @author Justin Hanselman
 */
public abstract class AbstractDriftModel implements iDriftModel {
    
    /**
     * Static Container for LooseBean Keys
     * <p>
     * This is provided for specification of Setters in extending contexts since this
     * abstract layer only provides getter with protected setters that may be wrapped
     * by the extending class and declared a BeanSetter
     */
    public static class LooseBeanKeys {
        
        public static final String FrameNumber = "FrameNum";
        public static final String XTranslation = "xTranslation";
        public static final String XUncertainty = "SigmaX";
        public static final String YTranslation = "yTranslation";
        public static final String YUncertainty = "SigmaY";
        public static final String ZTranslation = "zTranslation";
        public static final String ZUncertainty = "SigmaZ";
        public static final String RotationAxisPoint = "rotAxisPoint";
        public static final String EulerRotation = "EulRotPoint";
        public static final String EulerUncertainty = "SigmaEuler";
        public static final String Units = "units";
        
        
    }

    @CsvBindByName( column = "Frame" )
    @BeanProperty( name = LooseBeanKeys.FrameNumber )
    protected int frameNum_;
    
    @CsvBindByName( column = "X Translation" )
    @BeanProperty( name = LooseBeanKeys.XTranslation )
    protected double xTranslation_;
    
    @CsvBindByName( column = "Sigma_X" )
    @BeanProperty( name = LooseBeanKeys.XUncertainty )
    protected double xUncertainty_;
    
    @CsvBindByName( column = "Y Translation" )
    @BeanProperty( name = LooseBeanKeys.YTranslation )
    protected double yTranslation_;
    
    @CsvBindByName( column = "Sigma_Y" )
    @BeanProperty( name = LooseBeanKeys.YUncertainty )
    protected double yUncertainty_;
    
    @CsvBindByName( column = "Z Translation" )
    @BeanProperty( name = LooseBeanKeys.ZTranslation )
    protected double zTranslation_;
    
    @CsvBindByName( column = "Sigma_Z" )
    @BeanProperty( name = LooseBeanKeys.ZUncertainty )
    protected double zUncertainty_;
    
    @CsvCustomBindByName( column = "Units", converter = DriftUnitsCSVConverter.class )
    @BeanProperty( name = LooseBeanKeys.Units )
    protected iDriftModel.DriftUnits units_;
    
    @CsvCustomBindByName(column = "Rotation Axis Point", converter = Point3DCSVConverter.class)
    @BeanProperty( name = LooseBeanKeys.RotationAxisPoint )
    protected Point3D rotationAxis_;
    
    @CsvCustomBindByName(column = "Rotations", converter = Point3DCSVConverter.class)
    @BeanProperty( name = LooseBeanKeys.EulerRotation )
    protected Point3D eulerRotations_;
    
    @CsvCustomBindByName(column = "Sigma_Rotations", converter = Point3DCSVConverter.class)
    @BeanProperty( name = LooseBeanKeys.EulerUncertainty )
    protected Point3D eulerUncertainty_;
    
    /**
     * Nullary constructor 
     * <p>
     * Initializes all values to 0 and the standard unit {@link DriftUnits#m}
     */
    public AbstractDriftModel() {
        frameNum_ = 0;
        xTranslation_ = 0;
        xUncertainty_ = 0;
        yTranslation_ = 0;
        yUncertainty_ = 0;
        zTranslation_ = 0;
        zUncertainty_ = 0;
        rotationAxis_ = new Point3D(0,0,0);
        eulerRotations_ = new Point3D(0,0,0);
        eulerUncertainty_ = new Point3D(0,0,0);
        units_ = DriftUnits.m;
    }
    
    
    /**
     * General constructor for ease of use
     * <p>
     * Note:  protected final setters are provided for finer tuned control
     * 
     * @param frameNum The frame number relative to the start of the acquisition associate with this drift
     * @param xTranslation The linear x translation of the drift
     * @param xUncertainty The uncertainty in the linear x translation
     * @param yTranslation The linear y translation of the drift
     * @param yUncertainty The uncertainty in the linear y translation
     * @param zTranslation The linear z translation of the drift
     * @param zUncertaintyThe uncertainty in the linear z translation
     * @param rotAxis The rotational axis (a single point for orthogonal axes) around which the whole drift is rotated
     * @param eulerRot The euler rotations ( x, y, z) around the rotation axis (degrees)
     * @param eulerUncertainty The uncertainty in each of euler rotations (degrees)
     * @param units The units for the linear translations (only 1 unit is applied to all)
     */
    public AbstractDriftModel( int frameNum, double xTranslation, double xUncertainty, double yTranslation,
                                double yUncertainty, double zTranslation, double zUncertainty, Point3D rotAxis, 
                                Point3D eulerRot, Point3D eulerUncertainty, DriftUnits units ) {
        frameNum_ = frameNum;
        xTranslation_ = xTranslation;
        xUncertainty_ = xUncertainty;
        yTranslation_ = yTranslation;
        yUncertainty_ = yUncertainty;
        zTranslation_ = zTranslation;
        zUncertainty_ = zUncertainty;
        rotationAxis_ = rotAxis;
        eulerRotations_ = eulerRot;
        eulerUncertainty_ = eulerUncertainty;
        units_ = units;
    }
    
    @BeanGetter( name = LooseBeanKeys.FrameNumber )
    @Override
    public int getFrameNumber() {
        return frameNum_;
    }

    final protected void setFrameNumber( int fNum) {
        frameNum_ = fNum;
    }
    
    @BeanGetter( name = LooseBeanKeys.XTranslation )
    @Override
    public double getXTranslation() {
        return xTranslation_;
    }
    
    final protected void setXTranslation( double xTranslation ) {
        xTranslation_ = xTranslation;
    }

    @BeanGetter( name = LooseBeanKeys.XUncertainty )
    @Override
    public double getXUncertainty() {
        return xUncertainty_;
    }
    
    final protected void setXUncertainty( double xUncertainty) {
        xUncertainty_ = xUncertainty;
    }

    @BeanGetter(name = LooseBeanKeys.YTranslation )
    @Override
    public double getYTranslation() {
        return yTranslation_;
    }

    final protected void setYTranslation( double yTranslation ) {
        yTranslation_ = yTranslation;
    }
    
    @BeanGetter( name = LooseBeanKeys.YUncertainty )
    @Override
    public double getYUncertainty() {
        return yUncertainty_;
    }
    
    final protected void setYUncertainty( double yUncertainty ) {
        yUncertainty_ = yUncertainty;
    }

    @BeanGetter( name = LooseBeanKeys.ZTranslation )
    @Override
    public double getZTranslation() {
        return zTranslation_;
    }
    
    final protected void setZTranslation( double zTranslation) {
        zTranslation_ = zTranslation;
    }

    @BeanGetter( name = LooseBeanKeys.ZUncertainty )
    @Override
    public double getZUncertainty() {
        return zUncertainty_;
    }
    
    final protected void setZUncertainty( double zUncertainty ) {
        zUncertainty_ = zUncertainty;
    }

    @BeanGetter( name = LooseBeanKeys.Units )
    @Override
    public DriftUnits getDriftUnits() {
        return units_;
    }
    
    final protected void setDriftUnits( DriftUnits units ){
        units_ = units;
    }
    
    @BeanGetter( name = LooseBeanKeys.RotationAxisPoint )    
    @Override
    public Point3D getRotationAxisPoint() {
        return rotationAxis_;
    }

    final protected void setRotationAxisPoint( double x, double y, double z ) {
        rotationAxis_ = new Point3D( x, y, z);
    }
    
    @BeanGetter( name = LooseBeanKeys.EulerRotation )    
    @Override
    public Point3D getEulerRotations() {
        return eulerRotations_;
    }
    
    final protected void setEulerRotations( double xRot, double yRot, double zRot ) {
        eulerRotations_ = new Point3D( xRot, yRot, zRot );
    }

    @BeanGetter( name = LooseBeanKeys.EulerUncertainty )
    @Override
    public Point3D getEulerRotationUncertainty() {
       return eulerUncertainty_;
    }
    
    final protected void setEulerRotationUnceratinty( double xRotUncertainty, double yRotUncertainty, double zRotUncertainty ) {
        eulerUncertainty_ = new Point3D( xRotUncertainty, yRotUncertainty, zRotUncertainty );
    }
    
}
