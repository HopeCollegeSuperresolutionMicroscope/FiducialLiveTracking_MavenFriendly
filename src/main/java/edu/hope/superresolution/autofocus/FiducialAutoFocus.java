/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.autofocus;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.exceptions.NoFiducialException;
import edu.hope.superresolution.livetrack.LiveTracking;
import edu.hope.superresolution.models.FiducialArea;
import edu.hope.superresolution.models.FiducialLocationModel;
import edu.hope.superresolution.models.LocationAcquisitionModel;
import edu.valelab.gaussianfit.utils.ReportingUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.MMAcquisition;
import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
import org.micromanager.api.ImageCache;
import org.micromanager.api.ImageCacheListener;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.SequenceSettings;
import org.micromanager.api.TaggedImageStorage;
import org.micromanager.utils.AutofocusBase;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.MMException;
import org.micromanager.utils.MMScriptException;

/**
 *
 * @author Microscope
 */
public class FiducialAutoFocus extends AutofocusBase /*implements Autofocus*/  {

    public final static String DEVICE_NAME = "Fiducial Tracking and Fitting AutoFocus";
    
    private final static String BASE_INCREMENT_UM_STR = "Step Increment (um)";
    private final static String SLOP_TRAVEL_STR = "Slop Travel Present";
    private final static String SLOP_TRAVEL_UM_STR = "Slop Travel (um)";
    private final static String THRESHOLD_UNCERTAINTY_STR = "Max Threshold Uncertainty (nm)";
    private final static String MAX_ANTICIPATED_TRAVEL_STR = "Maximum Anticipated Lateral Travel (um)";
    
    //Default Values For First Time Start
    //Should become Programmatic
    public double BASE_STEP_UM = .1; 
    public boolean IS_SLOP_TRAVEL = false;
    public double SLOP_TRAVEL = .4;
    public double MAX_SCORE_UNCERTAINTY_THRESHOLD = 20;
    
    private ScriptInterface app_;
    private CMMCore core_;
    private LiveTracking fiducialFocusPlugin_ = null;
    private FiducialLocationModel lastFLocModel_ = null;
    private String currentAcqName_ = null;
    
    //Foolish Globals Currently
    //private ImagePlus ipCurrent_;
    private ImageProcessor ipCurrent_;
    private double threshold_ = 10;
    private int numImagesTaken_ = 0;
    private double curDist_ = 0;
    private double prevOutOfFocusZScore_ = 0;
    private double prevOutOfFocusZUncertainty_ = 0;
    private double currentRelativeZScore_ = 0;
    private double currentRelativeZUncertainty_ = 0;
    private double beginningStdDev_ = 0;
    private int maxNumNoFocus_ = 3;
    private int slopNoFocusCount_ = 0;
    private int maxBackAndForth_ = 5;
    private int noFocusCount_ = 0;
    private int dir_ = 1; //Direction for moving
    private double relativeXPixelTranslation_ = 0;
    private double relativeYPixelTranslation_ = 0;
    //private double absXPixelTranslation_ = 0;
    //private double absYPixelTranslation_ = 0;
    
    //local global list of all translationPoints for the currentAcquisition
    private List<FocusFrameTranslationObj> translationPoints_ = new ArrayList(); 
    
    //Score Method Enumeration
    public ScoreMethods scoreMethod_ = ScoreMethods.avgRelativeZ;
    public enum ScoreMethods {
        avgRelativeZ, minRelativeZWithUncertainty
    }
    
    /**
     * Helper Class to Overcome Post-Acquisition Editing Due to the lack of utility
     *  in ScriptInterface.  It would be much easier to REMOVE The Acquisition frames
     *  But somehow, a core snap still adds to the Acquisition.  Which is maddening.
     * <p>
     *  
     */
    private class FocusFrameTranslationObj{ 
        public double xAbsPixelTranslation_;
        public double yAbsPixelTranslation_;
        public int frameFocusStarted_;
        public int numFramesToEnd_;
        
        /**
         * Constructor
         * 
         * @param xAbsPixelTranslation The Absolute X Translation (in Pixels) at the end of a focus event
         * @param yAbsPixelTranslation The Absolute Y Translation (in Pixels) at the end of a focus event
         * @param frameFocusStarted The Frame Number at Which the Focus Started
         * @param numFramesToEnd The Number Of Frames from the start for the focus to end
         */
        public FocusFrameTranslationObj( double xAbsPixelTranslation, double yAbsPixelTranslation,
                                            int frameFocusStarted, int numFramesToEnd ) {
            xAbsPixelTranslation_ = xAbsPixelTranslation;
            yAbsPixelTranslation_ = yAbsPixelTranslation;
            frameFocusStarted_ = frameFocusStarted;
            numFramesToEnd_ = numFramesToEnd;
        }        
    }
    
    /**
     * Inner Class That Implements Image Cache Behavior
     * This Class takes a current session of autofocus and uses the fiducial tracks to align the images
     * in the stack.  Implemented each time an autofocus is called on a new acquisition.
     * <p>
     * The goal is not to completely align an every image based on fiducials, unless the 
     * autofocus is occuring at every frame.  Rather, it provides a correction for any lateral drift
     * that occurred during the last autofocus event so that drift is minimal and continuous when 
     * analyzed by other softwares.
     */
    private class CacheShiftingListener implements ImageCacheListener {
        
        
            private final TaggedImageStorage shiftedStorage_;
            private final List<FocusFrameTranslationObj> translationPoints_; 
        
            /**
             * General Constructor
             * 
             * Listener That will store shifted images to shifted storage and will 
             * translate recieved Images using FocusFrameTranslation data from a synchronizedList
             * 
             * @param shiftedStorage - The Tagged Image Storage in which to store the shifted Images
             * @param translationPoints - A SYNCHRONIZED list of points, in which translation is associated with 
             *                            the last fiducial track formed.
             */
            public CacheShiftingListener( TaggedImageStorage shiftedStorage, List<FocusFrameTranslationObj> translationPoints ) {
                shiftedStorage_ = shiftedStorage;
                translationPoints_ = translationPoints;
            }
            
            @Override
            public void imageReceived(TaggedImage ti) {

                //Detect Poison Key From the TaggedImage Queue and close the storage
                if (ti.pix == null && ti.tags == null) {
                    ij.IJ.log("Poison Key Detected!");
                    shiftedStorage_.finished();
                    shiftedStorage_.close();
                    return;
                }
                
                //Check to make sure JSON data dtags aren't corrupted
                int bytes = 0, imWidth = 0, imHeight = 0, frame = -1;
                try {
                    bytes = MDUtils.getBytesPerPixel(ti.tags);
                    imWidth = MDUtils.getWidth(ti.tags);
                    imHeight = MDUtils.getHeight(ti.tags);
                    frame = MDUtils.getFrameIndex(ti.tags);
                } catch (JSONException ex) {
                    Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
                } catch (MMScriptException ex) {
                    Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (bytes <= 0 || imWidth <= 0 || imHeight <= 0 || frame < 0) {
                    return;
                }

                //Determine the current translation pertaining to the frame number of the taggedImage
                // And acquired translations (translationPoints_) off of Fiducial Tracking.
                // This assumes makes the nearest frame that is less than the current frame the
                // pertinent translation for the image.
                FocusFrameTranslationObj curObj = null;
                synchronized (translationPoints_) {
                    Iterator< FocusFrameTranslationObj> it = translationPoints_.iterator();
                    while (it.hasNext()) {
                        FocusFrameTranslationObj tObj = it.next();
                        if (frame >= tObj.frameFocusStarted_) {
                            curObj = tObj;
                        } else {
                            //Any Additional Objects are beyond frame Focus
                            break;
                        }
                    }
                }

                //There was something wrong with the frame acquisition
                if (curObj == null) {
                    return;
                }

                //Apply Translation to ImageBinary 
                int length = imWidth * imHeight;

                int dx = (int) curObj.xAbsPixelTranslation_;
                int dy = (int) curObj.yAbsPixelTranslation_;

                //ij.IJ.log("Should Shift x and y by: " + dx + "," + dy);
                TaggedImage shiftedTI;
                if (bytes == 1) {
                    byte[] copyPixels = new byte[length];
                    byte[] ref = (byte[]) ti.pix;
                    System.arraycopy(ref, 0, copyPixels, 0, imWidth * imHeight);
                    for (int i = 0; i < length; ++i) {
                        int x = i % imWidth, y = i / imWidth;
                        int tX = x + dx, tY = y + dy;
                        if (tX < 0 || tX >= imWidth || tY < 0 || tY >= imHeight) {
                            copyPixels[i] = 0;
                        } else {
                            copyPixels[i] = ref[tX + tY * imWidth];
                        }
                    }
                    shiftedTI = new TaggedImage(copyPixels, ti.tags);
                } else if (bytes == 2) {
                    short[] copyPixels = new short[length];
                    short[] ref = (short[]) ti.pix;
                    System.arraycopy(ref, 0, copyPixels, 0, imWidth * imHeight);
                    for (int i = 0; i < length; ++i) {
                        int x = i % imWidth, y = i / imWidth;
                        int tX = x + dx, tY = y + dy;
                        if (tX < 0 || tX >= imWidth || tY < 0 || tY >= imHeight) {
                            copyPixels[i] = 0;
                        } else {
                            copyPixels[i] = ref[tX + tY * imWidth];
                        }
                    }
                    shiftedTI = new TaggedImage(copyPixels, ti.tags);
                } else if (bytes == 4) {
                    int[] copyPixels = new int[length];
                    int[] ref = (int[]) ti.pix;
                    System.arraycopy(ref, 0, copyPixels, 0, imWidth * imHeight);
                    for (int i = 0; i < length; ++i) {
                        int x = i % imWidth, y = i / imWidth;
                        int tX = x + dx, tY = y + dy;
                        if (tX < 0 || tX >= imWidth || tY < 0 || tY >= imHeight) {
                            copyPixels[i] = 0;
                        } else {
                            copyPixels[i] = ref[tX + tY * imWidth];
                        }
                    }
                    shiftedTI = new TaggedImage(copyPixels, ti.tags);
                } else if (bytes == 8) {
                    long[] copyPixels = new long[length];
                    long[] ref = (long[]) ti.pix;
                    System.arraycopy(ref, 0, copyPixels, 0, imWidth * imHeight);
                    for (int i = 0; i < length; ++i) {
                        int x = i % imWidth, y = i / imWidth;
                        int tX = x + dx, tY = y + dy;
                        if (tX < 0 || tX >= imWidth || tY < 0 || tY >= imHeight) {
                            copyPixels[i] = 0;
                        } else {
                            copyPixels[i] = ref[tX + tY * imWidth];
                        }
                    }
                    shiftedTI = new TaggedImage(copyPixels, ti.tags);
                } else {
                    return;
                }

                //store Shifted Image
                try {
                    shiftedStorage_.setSummaryMetadata(shiftedTI.tags);
                    shiftedStorage_.putImage(shiftedTI);
                } catch (MMException ex) {
                    Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            /**
             *  Shifts the Location Acquisition Based on Average Travel for easier alignment.
             *  It then stores it in the same path directory under an "AutoFocusShift" Folder
             *  unless it is already in the folder, in which case it prompts the user to save it.
             * 
             * @param path - the path of the acquisition that was finished
             */
            @Override
            public void imagingFinished(String path) {

            }
            
        }  
    
    /**
     * Constructor
     */
    public FiducialAutoFocus() {  
      super();
      
      createProperty(BASE_INCREMENT_UM_STR, Double.toString(BASE_STEP_UM));
      createProperty(SLOP_TRAVEL_STR, Boolean.toString(IS_SLOP_TRAVEL));
      createProperty(THRESHOLD_UNCERTAINTY_STR, Double.toString(MAX_SCORE_UNCERTAINTY_THRESHOLD));
      //createProperty(MAX_ANTICIPATED_TRAVEL_STR, Double.toString(MAX_ANTICIPATED_TRAVEL));
      createProperty(SLOP_TRAVEL_UM_STR, Double.toString(SLOP_TRAVEL) );
      
      loadSettings();
    }
    
    @Override
    public void applySettings() {
        try {
            SLOP_TRAVEL = Double.parseDouble( getPropertyValue(SLOP_TRAVEL_UM_STR) );
            BASE_STEP_UM = Double.parseDouble( getPropertyValue(BASE_INCREMENT_UM_STR) );
            IS_SLOP_TRAVEL = Boolean.parseBoolean( getPropertyValue(SLOP_TRAVEL_STR) );
            MAX_SCORE_UNCERTAINTY_THRESHOLD = Double.parseDouble( getPropertyValue(THRESHOLD_UNCERTAINTY_STR) );
            //MAX_ANTICIPATED_TRAVEL = Double.parseDouble( getPropertyValue(MAX_ANTICIPATED_TRAVEL_STR) );

        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if( !IS_SLOP_TRAVEL ) {
            SLOP_TRAVEL = BASE_STEP_UM;
        }
        threshold_ = MAX_SCORE_UNCERTAINTY_THRESHOLD;
    }

    @Override
    public void setApp(ScriptInterface si) {
        app_ = si;
        core_ = si.getMMCore();
    }
    
    /**
     *  Used to Encapsulate All Methods For When a FullFocus counts as a new AutoFocus Session.
     *  This assumes that the most Current Acquisition is the one we are interested in
     *  and stores its information accordingly, such as Acquisition name and path,
     *  as well as creating and assigning an ImageCacheListener to the Image Cache.
     *  
     */
    private void startNewAutoFocusSession() {
  
        //Due to uncertainty about acquisition back end, get all Current information in same section
        String currentAcqPath = app_.getAcquisitionPath();
        String[] openAcqs = app_.getAcquisitionNames();
        String tempAcqName, tempPathName;
        ImageCache iCache = null;
        //Since we need the Semantic Path name for an acquisition, determine it
        //By checking each acquisition's ImageCache Path with the one listed as the current One.
        for (int i = 0; i < openAcqs.length; ++i) {
            tempAcqName = openAcqs[i];
            try {
                MMAcquisition acq = app_.getAcquisition(tempAcqName);
                tempPathName = acq.getImageCache().getDiskLocation();
                if (currentAcqPath != null && currentAcqPath.equals(tempPathName)) {
                    currentAcqName_ = tempAcqName;
                    iCache = acq.getImageCache();
                    break;
                } else {
                    //if both path names are null, select the first as the appropriate acquisition
                    if (tempPathName == null) {
                        currentAcqName_ = tempAcqName;
                        iCache = acq.getImageCache();
                        break;
                    }
                }

            } catch (MMScriptException ex) {
                Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //this should never be null by the time we've gotten an acquisition
        //not sure if Micro-manager has some hidden surprises though
        assert(iCache != null );

        //Create A MultiDimensional Acquisition for shifted images
        String autoSaveDirPath = currentAcqPath + "\\AutoFocusShifted";
        SequenceSettings set = app_.getAcquisitionSettings();
        JSONObject metaData = null;
        try {
            metaData = new JSONObject(SequenceSettings.toJSONStream(set));
        } catch (JSONException ex) {
            Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
        }
        File file = new File(autoSaveDirPath);
        int i = 0;
        //Loop through Possible Directories
        while (file.exists()) {
            file = new File(autoSaveDirPath + "_" + i);
            i++;
        }
        file.mkdir();
        TaggedImageStorage shiftedStorage;
        //Create New set for TranslationPoints (Synchronized)
        translationPoints_ = Collections.synchronizedList( new ArrayList() );
        //Create and Attach Image Listener
        try {
            shiftedStorage = new TaggedImageStorageDiskDefault(autoSaveDirPath, true, metaData); //TaggedImageStorageMultipageTiff(autoSaveDirPath, true, metaData/*, false, true, true*/);   
            ImageCacheListener acqShiftListener = new CacheShiftingListener( shiftedStorage, translationPoints_ );
            iCache.addImageCacheListener(acqShiftListener);
        } catch (IOException ex) {
            Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    int i = 0;
    int globalPos_ = 0;
    @Override
    public double fullFocus() throws MMException {

        applySettings();

        //Disable Live Mode since that counts as an acquisition
        if (app_.isLiveModeOn()) {
            app_.enableLiveMode(false);
        }

        //Get the Current Acquisition that this should pertain to.
        //Since we only can do detection during full focus, if the non-null name no longer 
        //exists, then we assume a new acquisition is calling this
        if (app_.isAcquisitionRunning() == true && 
                (currentAcqName_ == null || !app_.acquisitionExists(currentAcqName_) ) ) {
            startNewAutoFocusSession();
        }
        else {
            //TEST THIS
            //Should set the acquisitionName to null is this is non-acquisition (one-off)
            if( !app_.isAcquisitionRunning() ) {
                currentAcqName_ = null;
            }
        }

        //Store The Beginning Frame at which this focus occurred or zero if its called outside of acquisition
        int lastFrame = 0;
        if (currentAcqName_ != null) {
            try {
                lastFrame = app_.getAcquisition(currentAcqName_).getLastAcquiredFrame();
            } catch (MMScriptException ex) {
                Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //Register this to a Fiducial Focus Instance
        if (fiducialFocusPlugin_ == null) {
            setFiducialLocationAcquisitionModel();
        }

        ij.IJ.log("For Frame Number: " + lastFrame);

        boolean outOfFocus = false, comingIntoFocus = false, overShoot = false;
        int backAndForthCount = 0;
        boolean dirSwitch = false;
        double prevComingIntoFocusZScore = Double.MAX_VALUE;
        prevOutOfFocusZScore_ = Double.MAX_VALUE; //Reset PrevOutOfFocusZScore
        //Pause Acquisition to avoid blurred images
        app_.getAcquisitionEngine2010().pause();
        //Reset NumImages Taken Count For This Focus
        numImagesTaken_ = 0;
        relativeXPixelTranslation_ = 0;
        relativeYPixelTranslation_ = 0;
        int numStepsInDir = 0;
        int prevNumStepsInDir = 0;
        //absYPixelTranslation_ = 0;
        //absXPixelTranslation_ = 0;
        do {
            try {
                //Currently just take a picture at core level and shoot it to ipCurrent_
                snapSingleImage();
                //Since We're storing the score in object space, comput it
                computeScore(ipCurrent_);
                if( beginningStdDev_ == 0 ) {
                    beginningStdDev_ = currentRelativeZUncertainty_;
                }

                if (currentRelativeZScore_ < threshold_ + beginningStdDev_ ) {
                    //This is just to make sure we're not on the edge
                    if (comingIntoFocus && currentRelativeZScore_ < prevComingIntoFocusZScore - currentRelativeZUncertainty_) {
                        ij.IJ.log("ComingIntoFocus Z Score: " + currentRelativeZScore_ + " compared to " + (prevComingIntoFocusZScore - currentRelativeZUncertainty_) );
                        outOfFocus = true;
                        prevComingIntoFocusZScore = currentRelativeZScore_;
                    } else {
                        outOfFocus = false;
                    }
                } else {
                    if (currentRelativeZScore_ > prevOutOfFocusZScore_ + prevOutOfFocusZUncertainty_) {
                        //Change Direction
                        dir_ = (dir_ == -1) ? 1 : -1;
                        dirSwitch = true;
                        backAndForthCount++;
                    }
                    //Store the zScore and maxUncertainty threshold for determining wrong direction
                    //This allows some slop, given the slight drift in poor systems like our own
                    // Hopefully the stdDeviation will produce a buffer zone
                    if (currentRelativeZScore_ < prevOutOfFocusZScore_) {
                        prevOutOfFocusZScore_ = currentRelativeZScore_;
                        prevOutOfFocusZUncertainty_ = (currentRelativeZUncertainty_ == 0)
                                ? threshold_ : currentRelativeZUncertainty_;
                    }
                    outOfFocus = true;
                }
            } catch (NoFiducialException e) {
                if (backAndForthCount < maxBackAndForth_) {
                    dir_ = (dir_ == -1) ? 1 : -1;
                    dirSwitch = true;
                    backAndForthCount++;
                    outOfFocus = true;
                } else {
                    //If We have had no Fiducial For the max number of no Focus (assume it's too far gone)
                    outOfFocus = false;
                    if (currentAcqName_ != null) {
                        try {
                            app_.closeAcquisition(currentAcqName_);
                        } catch (MMScriptException ex) {
                            Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
                            ReportingUtils.showError(ex);
                        }
                    }
                }
            }

            //Move Here if detected out of focus
            //This Accounts for NoFiducialExceptions and score dilemmas
            if (outOfFocus) {
                boolean slopFocus = true;
                if (dirSwitch) {
                    dirSwitch = false;
                    int wanderOffset = ( backAndForthCount < 2 ) ? 0 : 1;
                    slopFocus = travelSlop(dir_,
                            (int) (SLOP_TRAVEL * wanderOffset / BASE_STEP_UM + prevNumStepsInDir));
                    prevNumStepsInDir = numStepsInDir;
                    numStepsInDir = 0;
                }
                //We Were heading in the right direction, and therefore, we will move again
                if (slopFocus) {
                    moveZStageRelative(BASE_STEP_UM * dir_);
                    numStepsInDir++;
                }
                comingIntoFocus = true;
            }

            i++;
        } while (outOfFocus);

        //Add the Translation if we are doing this for an acquisition
        // And We did not get a null LocationModel as a result of not finding anything
        if (lastFLocModel_ != null && currentAcqName_ != null) {
            FocusFrameTranslationObj tObj = new FocusFrameTranslationObj(lastFLocModel_.getAvgAbsoluteXPixelTranslation(),
                    lastFLocModel_.getAvgAbsoluteYPixelTranslation(),
                    lastFrame, numImagesTaken_);
            //Since Synchronized list, already thread-safe
            translationPoints_.add(tObj);
        }

        /*
        if ( lastFLocModel_ != null ) {
            absYPixelTranslation_ = lastFLocModel_.getAvgAbsoluteYPixelTranslation();
            absXPixelTranslation_ = lastFLocModel_.getAvgAbsoluteXPixelTranslation();
        }*/
        app_.getAcquisitionEngine2010().resume();

        return 0;
    }
     
    /**
     * Calculate just the average ZDepth across the Fiducial Areas in a Model and 
     *  the standard Deviation (without uncertainties)
     * 
     * @param fLocModel The locationModel that contains the Fiducial Areas to average
     * @return A 2 index Array [ avgZDepth, stdDev ]
     */
    private double[ ] calculateAverageZDepthAndStdDev( FiducialLocationModel fLocModel ) {
        List< FiducialArea > fiducials = fLocModel.getFiducialAreaList();
        
        double [] avgs = new double[]{0, 0};
        int numZMeasures = 0;
        //Average
        for( FiducialArea fArea : fiducials ) {
            if( !fArea.getRawSelectedSpot().isVirtual() ) {
                avgs[0] += fArea.getRelativeFocusDistance();
                numZMeasures++;
            }
        }
        avgs[0] /= numZMeasures;
        
        for( FiducialArea fArea : fiducials ) {
            if( !fArea.getRawSelectedSpot().isVirtual() ) {
                avgs[1] += Math.pow( fArea.getRelativeFocusDistance() - avgs[0], 2);
            }
        }
        avgs[1] = Math.sqrt( avgs[1]/numZMeasures );
        
        //ReportingUtils.showError("The avg is: " + avgZDepth / numZMeasures );
        return avgs;
    }
    
    /**
     *  Calculates the Standard Deviation of the ZDepth Values for each Fiducial's
     *   representative Z-Depth.  Does not include uncertainties.  Can Be Added Via Other Function Later.
     * 
     * @param fLocModel
     * @return 
     */
    private double calculateZDepthStdDev( FiducialLocationModel fLocModel ) {
        List< FiducialArea > fiducials = fLocModel.getFiducialAreaList();
        
        double avgZDepth = 0;
        //double sumZUncertaintiesSquare = 0;
        int numZMeasures = 0;
        for( FiducialArea fArea : fiducials ) {
            if( !fArea.getRawSelectedSpot().isVirtual() ) {
                avgZDepth += fArea.getRelativeFocusDistance();
                //sumZUncertaintiesSquare += Math.pow(fArea.getRelativeFocusDistanceUncertainty(), 2);
                numZMeasures++;
            }
        }
        
        avgZDepth = avgZDepth / numZMeasures;
        
        double stdDev = 0;
        for( FiducialArea fArea : fiducials ) {
            if( !fArea.getRawSelectedSpot().isVirtual() ) {
                stdDev += Math.pow( fArea.getRelativeFocusDistance() - avgZDepth, 2);
            }
        }
        return Math.sqrt(stdDev/numZMeasures);
        
    }
    
    private double calculateSumUncertainties( FiducialLocationModel fLocModel ) {
        
        List< FiducialArea > fiducials = fLocModel.getFiducialAreaList();
        
        double sumUncertainties = 0;
        int numZMeasures = 0;
        
        for( FiducialArea fArea : fiducials ) {
            if( fArea.getVirtualFrameTrackNumber() == 0 ) {
                sumUncertainties += Math.pow( fArea.getRelativeFocusDistanceUncertainty(), 2);
                numZMeasures++;
            }
        }
        return Math.sqrt( sumUncertainties )/numZMeasures;
    }
    
    private double calculateAverageSigma( FiducialLocationModel fLocModel ) {
        
        List< FiducialArea > fiducials = fLocModel.getFiducialAreaList();
        double avgSigma = 0;
        //Take the Average of 
        for( FiducialArea fiducial : fiducials ) {
            //Get The Current Fiducial Based off of Max()  (Poor Assumption)
            BoundedSpotData spot = fiducial.getRawSelectedSpot();
            avgSigma += spot.getSigma();
        }
        
        return avgSigma / fiducials.size();
    }
    
    //take a snapshot and save pixel values in ipCurrent_
    private boolean snapSingleImage() {
        try {
            core_.snapImage();
            Object img = core_.getImage();
            ImagePlus implus = newWindow();// this step will create a new window iff indx = 1
            implus.getProcessor().setPixels(img);
            ipCurrent_ = implus.getProcessor();
        } catch (Exception e) {
            IJ.log(e.getMessage());
            IJ.error(e.getMessage());
            return false;
        }

        ++numImagesTaken_;
        return true;
    }
   
   //making a new window for a new snapshot.
   private ImagePlus newWindow(){
      ImagePlus implus;
      ImageProcessor ip;
      long byteDepth = core_.getBytesPerPixel();

      if (byteDepth == 1){
         ip = new ByteProcessor((int)core_.getImageWidth(),(int)core_.getImageHeight());
      } else  {
         ip = new ShortProcessor((int)core_.getImageWidth(), (int)core_.getImageHeight());
      }
      ip.setColor(Color.black);
      ip.fill();

      implus = new ImagePlus(String.valueOf(curDist_), ip);

      return implus;
   }
    
      //waiting    
   private void delay_time(double delay){
      Date date = new Date();
      long sec = date.getTime();
      while(date.getTime()<sec+delay){
         date = new Date();
      }
   }
   
    @Override
    public double incrementalFocus() throws MMException {
        return fullFocus();
    }

    @Override
    public int getNumberOfImages() {
        return numImagesTaken_;
    }

    @Override
    public String getVerboseStatus() {
       return "This Focus Takes Fiducials and Uses their uncertainty in a Gaussian to determine a threshold for focusing";
    }

    @Override
    public double getCurrentFocusScore() {
        return currentRelativeZScore_;
    }

    @Override
    public String getDeviceName() {
        return DEVICE_NAME;
    }

    @Override
    public double computeScore(ImageProcessor ip) {

        //This is here in case another process calls this without a Plugin Object
        if (fiducialFocusPlugin_ != null) {
            setFiducialLocationAcquisitionModel();
        }
        //Get Location Acquisition Model For use of processor
        LocationAcquisitionModel locAcqModel = fiducialFocusPlugin_.getLocationAcqModel();
        try {
            lastFLocModel_ = locAcqModel.pushNextFiducialLocationModel(ip, true);
            relativeXPixelTranslation_ += lastFLocModel_.getAvgRelXPixelTranslation();
            relativeYPixelTranslation_ += lastFLocModel_.getAvgRelYPixelTranslation();
        } catch (NoFiducialException e) {
            e.printStackTrace();
            ij.IJ.log(e.getMessage());
            //Re Package the Error as a RunTimeError
            throw e;
        }
        
        switch( scoreMethod_ ) {
            case avgRelativeZ:
                double[] avgs = calculateAverageZDepthAndStdDev( lastFLocModel_ );
                currentRelativeZScore_ = avgs[0];
                currentRelativeZUncertainty_ = avgs[1];
                ij.IJ.log( "Current Z-Score: " + currentRelativeZScore_ + " and Uncertainty: " +currentRelativeZUncertainty_);
                //In the case that we have a Z Score that is lower due to having started at a low focus plane
                if( currentRelativeZScore_ < 0 ) {
                    lastFLocModel_.setFocusPlaneToCurrentFiducials();
                    prevOutOfFocusZScore_ = prevOutOfFocusZScore_ - currentRelativeZScore_;
                }
                return currentRelativeZScore_;
        }
 
        return 0;
    }

    @Override
    public void focus(double d, int i, double d1, int i1) throws MMException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     *  Sets The FiducialLocationAcquisitionModel Instance for this Autofocus Instance.
     *   This Should Block for User Selection if a Model is Being Created.
     * 
     */
    private void setFiducialLocationAcquisitionModel( ) {
        //Get The Current Location Model
        ArrayList< MMPlugin > instances = LiveTracking.getInstances();
        
        //If Not already implemented, Create A LiveTracking Object With AcqModel
        if( instances.size() <= 0 ) {
            //make a new LiveTracking Plugin Call and use it
            //fiducialFocusPlugin_ = new LiveTracking();
        }
        else {
            //select latest Instace (could use more logid later)
            fiducialFocusPlugin_ = ( LiveTracking) instances.get( instances.size() - 1 );
        }
        
    }
    
    /**
     *  Slop Travel Method - This Accounts for Poor clasping on z-stage where there
     *  is an anticipated slop area when switching directions (tension is relieved).
     *  This Slop area is the maximum anticipated movement value for a z-stage 
     *  before a full defocus is assumed in that direction.
     * 
     * @param baseMult - Used to specify Direction by sign and increment by BASE_STEP_UM*baseMult (Cannot Be 0)
     * @param offSet - The Multiplier number of BASE_STEP_UM for compensation in the slop direction first
     * @return <code>true</code> if the computedZScore is less than the previousOutofFocusScore
     *         <code>false</code> if there has been no improvement in focus through the slop region
     */
    private boolean travelSlop( int baseMult, int offSet ) {
        
        assert(baseMult!=0);
        
        ij.IJ.log( "Travel Slop in Direction:" + baseMult + " and for number of Steps "+ (SLOP_TRAVEL/BASE_STEP_UM + offSet));
        
        boolean noFid = false;
        double stepInc = 0;
        double prevZFocusWithUncertainty = 0;
        while( stepInc < SLOP_TRAVEL + offSet * BASE_STEP_UM ) {
            snapSingleImage();
            try {
                computeScore( ipCurrent_ );
            } catch ( NoFiducialException ex ) {
                noFid = true;
                slopNoFocusCount_++;
            }
            
            //If We have achieved a better score, we should see increase on second iteration
            if( !noFid && currentRelativeZScore_ < prevOutOfFocusZScore_ + prevOutOfFocusZUncertainty_) { 
                if( currentRelativeZScore_ < prevZFocusWithUncertainty + currentRelativeZUncertainty_ ) {
                    //prevOutOfFocusZScore_ = currentRelativeZScore_;
                    return true;
                } else {
                    prevZFocusWithUncertainty = currentRelativeZScore_ + currentRelativeZUncertainty_;
                }
            } else {
                stepInc += Math.abs( baseMult * BASE_STEP_UM);
                moveZStageRelative( baseMult * BASE_STEP_UM );
                //remove the location model if it was not a noFid
                if( !noFid ) {
                    //Get Location Acquisition Model For use of processor
                    LocationAcquisitionModel locAcqModel = fiducialFocusPlugin_.getLocationAcqModel();
                    locAcqModel.removeLastLocationAcquistion();
                }
                noFid = false;
                //reset this Zfocus to 0 so that we can perform the change
                prevZFocusWithUncertainty = 0;
            }
        }
        
        //If registered a Z Focus but there was no ability for a second iteration
        return prevZFocusWithUncertainty != 0; 
    }
    
    /**
     *  Moves the ZStage Device By the increment (in um) passed to it.
     * 
     * @param incrementUm The increment relative to the current position of the z-stage in um
     */
    private void moveZStageRelative(double incrementUm) {
        try {
            curDist_ = core_.getPosition(core_.getFocusDevice());
            //set z-distance to the lowest z-distance of the s
            core_.setPosition(core_.getFocusDevice(), curDist_ + incrementUm );
            core_.waitForDevice(core_.getFocusDevice());
            delay_time(300);
            String msg = "The relative Focus is: " + currentRelativeZScore_ + " compared to " + prevOutOfFocusZScore_ + prevOutOfFocusZUncertainty_;
            msg += " moved by " + incrementUm;
            if( incrementUm < 0 ) {
                globalPos_--;
            } else {
                globalPos_++;
            }
            ij.IJ.log( "GlobalPos Without slop = " + globalPos_ );
            ij.IJ.log(msg);
        } catch (Exception ex) {
            Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

}   
   
