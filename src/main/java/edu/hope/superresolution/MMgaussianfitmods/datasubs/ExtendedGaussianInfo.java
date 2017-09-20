/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.MMgaussianfitmods.datasubs;

import edu.hope.superresolution.fitters.FindLocalMaxima;
import edu.hope.superresolution.fitters.GenericBaseGaussianFitThread;
import edu.valelab.gaussianfit.data.SpotData;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 *  Class for Extended GaussianInfo, which takes all other preferences
 *    And provides a copy constructor-like class
 *    This Is A Modification of GaussianInfo Class, in an effort to utilize code
 *    from Localization Microscopy, but is a poor implementation strategy.
 * 
 * TODO:  SCRAP THE ENTIRE BACKEND PROVIDED BY THIS CLASS. This data structure is 
 *        a result of trying to cannibalize Localization Microscopy Code, but it is 
 *        non-expandable.
 * 
 * @author Microscope
 */
public class ExtendedGaussianInfo {

    /**
     * Remnant Constants from Gaussian Info
     */
    protected static final Object gfsLock_ = new Object();
    protected ImagePlus siPlus_;
    protected ImageProcessor siProc_;
    protected BlockingQueue<SpotData> sourceList_;
    protected List<SpotData> resultList_;

    // half the size (in pixels) of the square used for Gaussian fitting
    protected int halfSize_ = 8;
    protected double baseLevel_ = 100; // base level of the camera in counts

    // settings for maximum finder
    protected int noiseTolerance_ = 100;

    // Needed to calculate # of photons and estimate error
    // the real PCF is calculated as photonConversionFactor_ * gain_
    protected double photonConversionFactor_ = 10.41;
    protected double gain_ = 50;
    protected float pixelSize_ = 107; // nm/pixel
    protected float zStackStepSize_ = 50;  // step size of Z-stack in nm
    protected double timeIntervalMs_ = 100;

    // Filters for results of Gaussian fit
    protected double widthMax_ = 200;
    protected double widthMin_ = 100;
    protected boolean useWidthFilter_ = false;

    protected double nrPhotonsMin_ = 100;
    protected double nrPhotonsMax_ = 100000;
    protected boolean useNrPhotonsFilter_;

    // Settings affecting Gaussian fitting
    protected int maxIterations_ = 200;
    protected int mode_;
    protected int shape_;
    protected int fitMode_;

    // Setting determinig tracking behavior
    protected boolean endTrackAfterBadFrames_;
    protected int endTrackAfterNBadFrames_;

    protected boolean stop_ = false;

    //Additional GUI tags that were not merged into GaussianInfo
    protected String positionString_ = "";  //Legacy From Localization Code
    
    //Additional Operations
    private FindLocalMaxima.FilterType prefilterType_ = FindLocalMaxima.FilterType.NONE;
    private GenericBaseGaussianFitThread.DataEnsureMode dataEnsureMode_ = GenericBaseGaussianFitThread.DataEnsureMode.none;
    private double snr_ = 6;  //Used to Set the Desired Signal To Noise Ratio
    private int intensityThreshold_;
    private int maxTrackTravel_;  //The Maximum Travel (in pixels) assumed when tracking
    
    //Nullary Constructor
    public ExtendedGaussianInfo() {
    }

    //Copy Constructor - Same Classes
    public ExtendedGaussianInfo(ExtendedGaussianInfo subObject) {

        noiseTolerance_ = subObject.noiseTolerance_;
        photonConversionFactor_ = subObject.photonConversionFactor_;
        gain_ = subObject.gain_;
        pixelSize_ = (float) subObject.pixelSize_;
        zStackStepSize_ = (float) subObject.zStackStepSize_;
        timeIntervalMs_ = subObject.timeIntervalMs_;
        widthMin_ = subObject.widthMin_;
        widthMax_ = subObject.widthMax_;
        useWidthFilter_ = subObject.useWidthFilter_;
        nrPhotonsMin_ = subObject.nrPhotonsMin_;
        nrPhotonsMax_ = subObject.nrPhotonsMax_;
        useNrPhotonsFilter_ = subObject.useNrPhotonsFilter_;
        shape_ = subObject.shape_;
        fitMode_ = subObject.fitMode_;
        baseLevel_ = subObject.baseLevel_;
        endTrackAfterBadFrames_ = subObject.endTrackAfterBadFrames_;
        endTrackAfterNBadFrames_ = subObject.endTrackAfterNBadFrames_;

        positionString_ = subObject.positionString_;

        //Perform copy on other un-get-set variables.
        //DO NOT rely on these in the future
        siPlus_ = subObject.siPlus_;
        siProc_ = subObject.siProc_;
        sourceList_ = subObject.sourceList_;
        resultList_ = subObject.resultList_;
        halfSize_ = subObject.halfSize_;

        // Settings affecting Gaussian fitting
        maxIterations_ = subObject.maxIterations_;
        mode_ = subObject.mode_;
        prefilterType_ = subObject.prefilterType_;
        dataEnsureMode_ = subObject.dataEnsureMode_;
        snr_ = subObject.snr_;
        intensityThreshold_ = subObject.intensityThreshold_;
        maxTrackTravel_ = subObject.maxTrackTravel_;

    }

    public void setPrefilterType( FindLocalMaxima.FilterType prefilterType ) {
        prefilterType_ = prefilterType;
    }
    
    public FindLocalMaxima.FilterType getPrefilterType( ) {
        return prefilterType_;
    }
    
    public void setDataEnsureMode( GenericBaseGaussianFitThread.DataEnsureMode dataEnsureMode ) {
        dataEnsureMode_ = dataEnsureMode;
    }
    
    public GenericBaseGaussianFitThread.DataEnsureMode getDataEnsureMode( ) {
        return dataEnsureMode_;
    }
    
    public boolean setPositionString(String positionString) {
        positionString_ = positionString;
        return true;
    }

    public String getPositionString() {
        return positionString_;
    }

    protected void print(String myText) {
        ij.IJ.log(myText);
    }

    public void setNoiseTolerance(int n) {
        noiseTolerance_ = n;
    }

    public int getNoiseTolerance() {
        return noiseTolerance_;
    }

    public void setPhotonConversionFactor(double f) {
        photonConversionFactor_ = f;
    }

    public double getPhotonConversionFactor() {
        return photonConversionFactor_;
    }

    public void setGain(double f) {
        gain_ = f;
    }

    public double getGain() {
        return gain_;
    }

    public void setPixelSize(float f) {
        pixelSize_ = f;
    }

    public double getPixelSize() {
        return pixelSize_;
    }

    public void setZStackStepSize(float f) {
        zStackStepSize_ = f;
    }

    public double getZStackStepSize() {
        return zStackStepSize_;
    }

    public void setTimeIntervalMs(double f) {
        timeIntervalMs_ = f;
    }

    public double getTimeIntervalMs() {
        return timeIntervalMs_;
    }

    public void setWidthMin(double f) {
        widthMin_ = f;
    }

    public double getSigmaMin() {
        return widthMin_;
    }

    public void setWidthMax(double f) {
        widthMax_ = f;
    }

    public double getSigmaMax() {
        return widthMax_;
    }

    public void setUseWidthFilter(boolean filter) {
        useWidthFilter_ = filter;
    }

    public boolean getUseWidthFilter() {
        return useWidthFilter_;
    }

    public void setNrPhotonsMin(double min) {
        nrPhotonsMin_ = min;
    }

    public double getNrPhotonsMin() {
        return nrPhotonsMin_;
    }

    public void setNrPhotonsMax(double max) {
        nrPhotonsMax_ = max;
    }

    public double getNrPhotonsMax() {
        return nrPhotonsMax_;
    }

    public void setUseNrPhotonsFilter(boolean filter) {
        useNrPhotonsFilter_ = filter;
    }

    public boolean getUseNrPhotonsFilter() {
        return useNrPhotonsFilter_;
    }

    public void setMaxIterations(int maxIter) {
        maxIterations_ = maxIter;
    }

    public void setBoxSize(int boxSize) {
        halfSize_ = boxSize / 2;
    }

    public void setShape(int shape) {
        shape_ = shape;
    }

    public int getShape() {
        return shape_;
    }

    public void setFitMode(int fitMode) {
        fitMode_ = fitMode;
    }

    public int getFitMode() {
        return fitMode_;
    }

    public void setBaseLevel(double baseLevel) {
        baseLevel_ = baseLevel;
    }

    public double getBaseLevel() {
        return baseLevel_;
    }

    public void setEndTrackBool(boolean endTrack) {
        endTrackAfterBadFrames_ = endTrack;
    }

    public boolean getEndTrackBool() {
        return endTrackAfterBadFrames_;
    }

    public void setEndTrackAfterNFrames(int nFrames) {
        endTrackAfterNBadFrames_ = nFrames;
    }

    public int getEndTrackAfterNFrames() {
        return endTrackAfterNBadFrames_;
    }
    
    public void setSNR( double snr ) {
        snr_ = snr;
    }
    
    public double getSNR( ) {
        return snr_;
    }

    public void setIntensityThreshold( int intensityThreshold ) {
        intensityThreshold_ = intensityThreshold;
    }
    
    public int getIntensityThreshold( ) {
        return intensityThreshold_;
    }
    
    public void setMaxTrackTravel( int maxTrackTravel ) {
        maxTrackTravel_ = maxTrackTravel;
    }
    
    public int getMaxTrackTravel( ) {
        return maxTrackTravel_;
    }
}
