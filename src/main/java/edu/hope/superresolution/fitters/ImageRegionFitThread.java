/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.fitters;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.fitprocesses.FitProcessContainer;
import edu.hope.superresolution.genericstructures.FitThreadCallback;
import edu.valelab.gaussianfit.data.GaussianInfo;
import edu.valelab.gaussianfit.data.SpotData;
import edu.valelab.gaussianfit.utils.MMWindowAbstraction;
import edu.valelab.gaussianfit.utils.ProgressThread;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.awt.Polygon;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import edu.hope.superresolution.genericstructures.BlockingQueueEndCondition;

/**
 * 
 * Abstract Super Class That Provides a Process for fitting An ImagePlus against a set of Rois.
 * <p>
 * This class may either be instantiated via its constructor on the correct ImagePlus and Rois
 * that are composed to form the correct search area, or it may be lazy initialized or changed
 * via the setImagePlus and setRois()/addRoi method.  Note, these lazy initialization methods
 * all return booleans as to whether the value was set.  A no set condition occurs if 
 * a Runnable is currently running, because it is outside of the purpose of this class to
 * change the parameters of a fit mid-fit.  In the event of reuse, the isRunning() class
 * will return the state of the Runnable, and all modifications should be done afterward.
 * <p>
 * Additionally, extending classes must implement a createWithNewImagePlus( ) method.
 * This method is intended to copy all relevant attributes of the current FitThread, 
 * while attaching a new ImagePlus. This is meant to be the copy constructor of all 
 * subclasses in this sense.
 * <p>
 * <pre>
 * Basic Run Procedure:
 * 
 *     1. Establish ImagePlus Properties (i.e. slices, etc.)
 *     2. Create FitStackThreads using createFitStackThreadInstance() [abstract]
 *      {Per Image In the Stack}
 *     3. Discover the Potential Fit Points of Interest using discoverPointsOfInterest [abstract]
 *     4. Apply Each Potential Fit to the BlockingQueue with a cropped Image of halfSize_
 *      {End Per Image In the Stack}
 *     5. Wait For FitStackThreads to Finish
 *     6. Call The ListFinished Callback to use ResultingListInfo in Calling Context
 * </pre> 
 * <p>
 * Deprecated Use Case:This class takes care of initiation functions, join functions, and the basic flow
 * of processing each image.  In the case of an ImagePlus that is an ImageStack, a given Roi
 * will be applied to all Images In the Stack.
 * 
 *
 * @author Microscope
 */
abstract public class ImageRegionFitThread extends ExtendedGaussianInfo implements Runnable {
    
   /**General Lock to allow for modification of any settings to be used 
    *   while thread is running.  This takes care of the general case for pre-run
    *   variables as this lock is called on run. Synchronization for dynamic in-process 
    *   variables (i.e. any variable changed externally but used in sub-class abstract implementations)
    *   must be the responsibility of the implementing class.
    */
    protected final ReentrantLock modifyLock_ = new ReentrantLock();
    
    private volatile Thread t_;  //self-stored calling thread
    private ImagePlus ip_;
    private boolean running_ = false;
    private FitStackThread[] stackFitThreads_;
    private List<Roi> rois_ = new ArrayList<Roi>();
    private Roi roi_;
    
    //Callback for calling Context
    private final FitThreadCallback<SpotData> listCallback_;
    
    /**
     * EndConditionTest Class For StackThreads
     * 
     *  Assumes SpotData Object will have a frame number of -1 as endKey
     */
    private final BlockingQueueEndCondition<SpotData> endCondTest_ =
                    new BlockingQueueEndCondition<SpotData>(){
                        @Override
                        public boolean isEndCondition(SpotData queueObj) {
                            return queueObj.getFrame() == -1;
                        }
    
                    };
    
    /**
     *  Constructor 
     * 
     * @param ip             ImagePlus to be Fit 
     * @param roi            The Region of Interest to crop from each Image in 
     *                       the ImagePlus for Fitting
     * @param listCallback   A Callback for interaction with the calling context 
     *                       upon Completion of Fitting
     * @param positionString Micro-manager Image Sequence Labeling String For Evaluation
     */
    public ImageRegionFitThread( ImagePlus ip, Roi roi, FitThreadCallback<SpotData> listCallback, String positionString ) {
        ip_ = ip;
        positionString_ = positionString;
        roi_ = roi;
        listCallback_ = listCallback;
    }
    
    /**
     *  Partial Copy Constructor - Copies ExtendedGuassianInfo Object
     * 
     * @param ip           ImagePlus to be Fit
     * @param roi          The Region of Interest to crop from each Image in the 
     *                     ImagePlus for Fitting
     * @param listCallback A Callback for interaction with the calling context 
     *                      upon Completion of Fitting
     * @param extGaussInfo ExtendedGaussianInfo Object to Copy
     */
    public ImageRegionFitThread( ImagePlus ip, Roi roi, FitThreadCallback<SpotData> listCallback, ExtendedGaussianInfo extGaussInfo ) {
        super( extGaussInfo );
        ip_ = ip;
        roi_ = roi;
        listCallback_ = listCallback;
    }
    
    /**
     * Sets the Roi for use in the analysis. This will 
     * override the previous Rois.
     * 
     * @param roi The Roi to search in the Image, must have actual height, width, and be an Area
     * @return <code>true</code> if the Roi was set 
     *         or <code>false</code> if called while process was running
     * @throws IllegalArgumentException If Roi is an invalid version, this runtime exception is thrown
     *                                  it is up to the caller to catch and exit or simply continue due to
     *                                  the overarching nature of Roi class.
     * 
     * @see ij.gui.Roi
     */
    final public boolean setRoi( Roi roi ) throws IllegalArgumentException {
        if (isRunning()) {
            return false;
        }

        modifyLock_.lock();
        try {
            if (!roiIsValid( roi )) {
                throw new IllegalArgumentException("Roi Does Not Meet Requirements for " + this.getClass().getName());
            }
            rois_.clear(); //Reset the List
            rois_.add(roi);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } finally {
            modifyLock_.unlock();
        }
        return true;
    }
    
    /**
     * Overload - Sets the Region of Interest in the Image to a selection of Rois.  This will 
     * override the previous Rois if successful.
     * <p>
     * This is an All or Nothing Operation.  Meaning, if an Roi does not match the specified 
     * anticipated Roi type from the parameter set, the old set will remain unchanged.
     * It is recommended that a programmer make use of the roiIsValid() method
     * to filter their set before passing it in.  Otherwise, catching the exception and 
     * removing the offending will do so as well, but only for the Roi that triggered the throw.
     * 
     * @param rois The Rois to search in the Image, must have actual height, width, and be an Area
     * @return <code>true</code> if the Roi was set 
     *         or <code>false</code> if called while process was running
     * @throws IllegalArgumentException If Roi is an invalid version, this runtime exception is thrown
     *                                  it is up to the caller to catch and exit or simply continue due to
     *                                  the overarching nature of Roi class.
     */
    final public boolean setRoi( Roi[] rois ) throws IllegalArgumentException {
        if( isRunning() ) {
            return false;
        }
        
        //Prevents starting a run or other modifications until the logic is done
        modifyLock_.lock();
        List< Roi > copyList = new ArrayList();
        try {
            for( int i =0; i < rois.length; i++ ) {
                Roi roi = rois[i];
                if (!roiIsValid( roi )) {
                    throw new IllegalArgumentException("Roi Does Not Meet Requirements for " + this.getClass().getName());
                }
                copyList.add(roi);
            }
            //set the copyList to the new Rois if there wasn't a problem
            rois_ = copyList;
        } catch (IllegalArgumentException ex ) {
            //rethrow the exception
            throw ex;
        } finally {
            modifyLock_.unlock();
        }
        
        return true;
        
    }
    
     /**
     * Adds the Roi to the list of Regions of interest for fitting in the analysis. 
     * Unlike SetRoi() methods this will be appended to the current areas. 
     * 
     * @param roi The Roi to search in the Image, must have actual height, width, and be an Area
     * @return <code>true</code> if the Roi was set 
     *         or <code>false</code> if called while process was running
     * @throws IllegalArgumentException If Roi is an invalid version, this runtime exception is thrown
     *                                  it is up to the caller to catch and exit or simply continue due to
     *                                  the overarching nature of Roi class.
     * 
     * @see ij.gui.Roi
     */
    final public boolean addRoi( Roi roi ) throws IllegalArgumentException {
        if (isRunning()) {
            return false;
        }

        modifyLock_.lock();
        try {
            if (!roiIsValid( roi )) {
                throw new IllegalArgumentException("Roi Does Not Meet Requirements for " + this.getClass().getName());
            }
            rois_.add(roi);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } finally {
            modifyLock_.unlock();
        }
        return true;
    }
    
    /**
     * Overload - Adds the Region of Interests in the Image to the current selection of Rois.  
     * Unlike SetRoi() methods this will be appended to the current areas.
     * <p>
     * This is an All or Nothing Operation.  Meaning, if an Roi does not match the specified 
     * anticipated Roi type, all previous Rois that were added from the parameter set will
     * be removed as well.  It is recommended that a programmer make use of the roiIsValid() method
     * to filter their set before passing it in.  Otherwise, catching the exception and 
     * removing the offending Roi will do so as well, but only for the Roi that triggered the throw.
     * There is no guarantee about other Rois after the offending Roi.
     * 
     * @param rois The Rois to search in the Image, must meet an extending Classes roiIsValid() requirements
     * @return <code>true</code> if the Roi was set 
     *         or <code>false</code> if called while process was running
     * @throws IllegalArgumentException If Roi is an invalid version, this runtime exception is thrown
     *                                  it is up to the caller to catch and exit or simply continue due to
     *                                  the overarching nature of Roi class.
     */
    final public boolean addRoi( Roi[] rois ) throws IllegalArgumentException {
        if( isRunning() ) {
            return false;
        }
        
        //Prevents starting a run or other modifications until the logic is done
        modifyLock_.lock();
        List< Roi > copyList = new ArrayList();
        try {
            for( int i =0; i < rois.length; i++ ) {
                Roi roi = rois[i];
                if (!roiIsValid( roi )) {
                    throw new IllegalArgumentException("Roi Does Not Meet Requirements for " + this.getClass().getName());
                }
                copyList.add(roi);
            }
            //set the copyList to the new Rois if there wasn't a problem
            rois_.addAll(copyList);
        } catch (IllegalArgumentException ex ) {
            //rethrow the exception
            throw ex;
        } finally {
            modifyLock_.unlock();
        }
        
        return true;
        
    }
    
    /**
     * Resets the Rois for the fit to none.  This means that the entire ImageArea is processed
     * if the Runnable is run this way.  
     * @return 
     */
    final public boolean resetRois( ) {
        if( isRunning() ) {
            return false;
        }
        
        modifyLock_.lock();
        try{ 
            rois_.clear();
        } finally {
            modifyLock_.unlock();
        }
        
        return true;
        
    }

    /**
     * Get the resultList_ of fitted Spots
     * 
     * @return An ArrayList of SpotData corresponding to found and fitted points
     * @throws Exception When Attempt to get ResultList is called before this thread
     *                   has finished running.
     */
    public ArrayList<SpotData> getResultList( ) throws Exception {
        
        if( isRunning() )
        {
            throw new Exception( "Currently Running Operation");
        }
        
        //Create Shallow copy of resultList_
        return new ArrayList<SpotData>( resultList_ );
    }
 
    /**
     *  Sets the ImagePlus for use in the analysis
     *  <p> 
     *  Note: Only changes ImagePlus if Thread is not currently running
     * 
     * @param ip The ImagePlus to perform analysis on
     * @return <code>true</code> if the imagePlus was set 
     *         or <code>false</code> if called while process was running
     */
    public boolean setImagePlus( ImagePlus ip ) {
        
        if( isRunning() )
        {
            return false;
        }
        
        modifyLock_.lock();
        try{
            ip_ = ip;
        } finally {
            modifyLock_.unlock();
        }
        return true;
    }
    
   /**
    * Self-implememting initiation of Thread and starts the run process
    * 
    * @return <code>true</code> if it began running, or <code>false</code> if it was already running
    * 
    * @deprecated This method allows for rampant growth of threads and will be completely removed
    *             in favor of using ThreadPools in the future.
    */
   public boolean init() {

       if ( isRunning() ) {
           return false;
       }
      t_ = new Thread(this);
      setRunning( true );
      t_.start();
      return true;
   }
   
   /**
    * Synchronized access to see if a thread is running
    * 
    * @return 
    */
   public synchronized boolean isRunning() {
       return running_;
   }
   
   /**
    * Synchronized setRunning variable
    * @param enable 
    */
   private synchronized void setRunning( boolean enable ) {
       running_ = enable;
   }
   
   /**
    * Self-implememting join on self-stored Thread
    * 
    * @return <code>true</code> if it began running, or <code>false</code> if it was already running
    * 
    * @deprecated This method allows for rampant growth of threads and will be completely removed
    *             in favor of using ThreadPools in the future.
    */
   public boolean join(long millis) throws InterruptedException {
       if (!isRunning()) {
           return false;
       }
      t_.join(millis);
      return true;
   } 

   /**
    * Stops stackFitThreads that are used to process the potentially detected Spots.
    * Additionally, ends the Self-Implemented Thread.
    * 
    * @deprecated This method allows for rampant growth of threads and will be completely removed
    *             in favor of using ThreadPools in the future.
    */
   public void stop() {
      if (stackFitThreads_ != null) {
         for (FitStackThread stackFitThread : stackFitThreads_) {
            if (stackFitThread != null) {
               stackFitThread.stop();
            }
         }
      }
      t_ = null;
      setRunning(false);
   }

    @Override
    public void run() {
        
       // ij.IJ.log("Running Fit Process");
        //Wait for any modifying methods to finish
        modifyLock_.lock();
        try {
            //Test to make sure the Roi is an expected one
            if (!roiIsValid(roi_)) {
                return;
            }

            fitRoi(roi_);
        } finally {
            modifyLock_.unlock();
        }
        
        //return the independent resultList_ to the callback
        //Due to new initialization of list, resultList_ is independent
        //ij.IJ.log("Calling List Full");
        listCallback_.onListFull(resultList_);
        //Trigger any ThreadEnd Events
        listCallback_.onThreadEnd();
    }
    
    
    /**
     *  Fit all data in the current Image within the Roi
     *  
     * @param roi the ROI in which we search for Spots
     */
    private void fitRoi( Roi roi ) {
      // List with spot positions found through the Find Maxima command
      sourceList_ = new LinkedBlockingQueue<SpotData>();
      resultList_ = Collections.synchronizedList( new ArrayList<SpotData>() );

      
      //Set up Number of Threads in Spot Fitting
      int nrThreads = ij.Prefs.getThreads();
      if (nrThreads > 8) {
         nrThreads = 8;
      }
      
       long startTime = System.nanoTime();
      int nrPositions = 1;
      int nrChannels = ip_.getNChannels();
      int nrFrames = ip_.getNFrames();
      int nrSlices = ip_.getNSlices();
      int maxNrSpots = 0;

      boolean isMMWindow = MMWindowAbstraction.isMMWindow(ip_);

      //Legacy Code to set Position based on Micro-manager
      if (isMMWindow) {
         String[] parts = positionString_.split("-");
         nrPositions = MMWindowAbstraction.getNumberOfPositions(ip_);
         int startPos = 1; int endPos = 1;
         try{
             if (parts.length > 0) {
                 startPos = Integer.parseInt(parts[0]);
             }
             if (parts.length > 1) {
                 endPos = Integer.parseInt(parts[1]);
             }
         } catch ( NumberFormatException ex ) {
             //The Position string was non-existent, set it at 1 and numberPositions
             startPos = 1;
             endPos = nrPositions;
         }
         if (endPos > nrPositions) {
            endPos = nrPositions;
         }
         if (endPos < startPos) {
            endPos = startPos;
         }
         for (int p = startPos; p <= endPos; p++) {
            MMWindowAbstraction.setPosition(ip_, p);
            int nrSpots = analyzeImagePlus(ip_, p, nrThreads, roi);
            if (nrSpots > maxNrSpots) {
               maxNrSpots = nrSpots;
            }
            //print( "Number of Spots is: " + nrSpots );
         }
      
      }
      else
      {
        int nrSpots = analyzeImagePlus(ip_, 1, nrThreads, roi);
        if (nrSpots > maxNrSpots) {
            maxNrSpots = nrSpots;
        }

      }
      
      long endTime = System.nanoTime();

      // Add data to data overview window
      if (resultList_.size() < 1) {
         //ReportingUtils.showError("No spots found");
        print("Zero Spots Found");
         setRunning( false );
         return;
      }
      
      //Extra Data Storage From Legacy
      
      //Report The Current Stuff
      
      // report duration of analysis
      double took = (endTime - startTime) / 1E9;
      double rate = resultList_.size() / took;
      DecimalFormat df2 = new DecimalFormat("#.##");
      DecimalFormat df0 = new DecimalFormat("#");
     /* print("Analyzed " + resultList_.size() + " spots in " + df2.format(took)
              + " seconds (" + df0.format(rate) + " spots/sec.)");
      /*int i = 1;
      for( SpotData spot : resultList_ ) {
          print( "Spot" + 1 + ": width = " + spot.getWidth() + "\n" 
                  + "Spot bgr = " + spot.getBackground() );
      }*/

      setRunning( false );

    }
    
    /**
     * Check to see if an roi is valid to this Thread for fitting.
     * <p>
     * The public nature of this function allows for external checking.  This function
     * is also called as a contingency before running the thread.
     * 
     * @param roi The roi to check. Checks the internally saved roi during init()
     * @return <code>true<code> if roi is a valid roi for the sub-Class Implementations
     */
    abstract public boolean roiIsValid( Roi roi );
    
    /**
     * Initializes Threads to Search Through All Local Maximums found in the the method Thread
     * <p>
     * This could do with a large overhaul for ThreadPools as well.
     * 
     * @param siPlus
     * @param position
     * @param nrThreads
     * @param originalRoi
     * @return 
     */
    @SuppressWarnings("unchecked")
   private int analyzeImagePlus(ImagePlus siPlus, int position, int nrThreads, Roi originalRoi ) {

      int nrSpots = 0;
      // Start up IJ.Prefs.getThreads() threads for gaussian fitting
      stackFitThreads_ = new FitStackThread[nrThreads];
      for (int i = 0; i < nrThreads; i++) {
          //Get Actual FitStackThread from SubClass implementation
         stackFitThreads_[i] = createFitStackThreadInstance(sourceList_, endCondTest_, resultList_, 
                 siPlus, halfSize_, shape_, FitProcessContainer.OptimizationModes.getOptimizationMode(fitMode_));

         //Legacy Way of Updating Settings (Very constrained) should change later
         // TODO: more efficient way of passing through settings!
         stackFitThreads_[i].setPhotonConversionFactor(photonConversionFactor_);
         stackFitThreads_[i].setGain(gain_);
         stackFitThreads_[i].setPixelSize(pixelSize_);
         stackFitThreads_[i].setZStackStepSize(zStackStepSize_);
         stackFitThreads_[i].setTimeIntervalMs(timeIntervalMs_);
         stackFitThreads_[i].setBaseLevel(baseLevel_);
         stackFitThreads_[i].setNoiseTolerance(noiseTolerance_);
         stackFitThreads_[i].setSigmaMax(widthMax_);
         stackFitThreads_[i].setSigmaMin(widthMin_);
         stackFitThreads_[i].setNrPhotonsMin(nrPhotonsMin_);
         stackFitThreads_[i].setNrPhotonsMax(nrPhotonsMax_);
         stackFitThreads_[i].setMaxIterations(maxIterations_);
         stackFitThreads_[i].setUseWidthFilter(useWidthFilter_);
         stackFitThreads_[i].setUseNrPhotonsFilter(useNrPhotonsFilter_);
         stackFitThreads_[i].init();
      }

      // work around strange bug that happens with freshly opened images
      for (int i = 1; i <= siPlus.getNChannels(); i++) {
         siPlus.setPosition(i, siPlus.getCurrentSlice(), siPlus.getFrame());
      }

      int nrImages = siPlus.getNChannels() * siPlus.getNSlices() * siPlus.getNFrames();
      int imageCount = 0;
      try {
         for (int c = 1; c <= siPlus.getNChannels(); c++) {
            if ( !isRunning() ) {
               break;
            }
            for (int z = 1; z <= siPlus.getNSlices(); z++) {
               if ( !isRunning() ) {
                  break;
               }
               for (int f = 1; f <= siPlus.getNFrames(); f++) {
                  if ( !isRunning() ) {
                     break;
                  }
                  // to avoid making a gigantic sourceList and running out of memory
                  // sleep a bit when the sourcesize gets too big
                  // once we have very fast multi-core computers, this constant can be increased
                  if (sourceList_.size() > 100000) {
                     try {
                        Thread.sleep(1000);
                     } catch (InterruptedException ex) {
                        // not sure what to do
                     }
                  }

                  imageCount++;
                  ij.IJ.showStatus("Processing image " + imageCount);

                  //Generate Points of Interest
                  ImageProcessor siProc = null;
                  Polygon p = new Polygon();
                   //Not Really Portable For SpotData Processing due to static lock 
                  // Does Account for ImageProcessor sharing between getProcessor Method and this
                  synchronized (SpotData.lockIP) {
                     siPlus.setPositionWithoutUpdate(c, z, f);
                        siPlus.setRoi(originalRoi, false);
                        siProc = siPlus.getProcessor();
                        //Call Abstract discovery of Points
                        p = discoverPointsOfInterest( siProc );
                  }

                  if (p.npoints > nrSpots) {
                     nrSpots = p.npoints;
                  }
                  int[][] sC = new int[p.npoints][2];
                  for (int j = 0; j < p.npoints; j++) {
                                           
                     sC[j][0] = p.xpoints[j];
                     sC[j][1] = p.ypoints[j];
                  }
                      
                  //Sort Points Spatially Left to Right, Top to Bottom
                  Arrays.sort(sC, new ImageRegionFitThread.SpotSortComparator());
                
                  //Set up SpotData basic structures for FitStackThreads
                  SpotData spot;
                  for(int j = 0; j < sC.length; j++) {
                     // filter out spots too close to the edge based on halfSize_
                     //This is set by 
                     synchronized (SpotData.lockIP) {
                        spot = produceSpot( siProc, c, z, f, position, j, sC[j][0], sC[j][1] );
                     }
                     if( spot!= null ) {
                        try {
                           sourceList_.put(spot);
                        } catch (InterruptedException iex) {
                           Thread.currentThread().interrupt();
                           throw new RuntimeException("Unexpected interruption");
                        }
                     }
                  }
                              
                  ij.IJ.showProgress(imageCount, nrImages);
               }
            }
         }

      // start ProgresBar thread
      ProgressThread pt = new ProgressThread(sourceList_);
      pt.init();
      
      } catch (OutOfMemoryError ome) {
         ome.printStackTrace();
         ij.IJ.error("Out Of Memory");
      }

      // Send working threads signal that we are done:
      SpotData lastSpot = new SpotData(null, -1, 1, -1, -1, -1, -1, -1);
      try {
         sourceList_.put(lastSpot);
      } catch (InterruptedException iex) {
         Thread.currentThread().interrupt();
         throw new RuntimeException("Unexpected interruption");
      }

      // wait for worker threads to finish
      for (int i=0; i<nrThreads; i++) {
         try {
            stackFitThreads_[i].join();
            stackFitThreads_[i] = null;
         } catch (InterruptedException ie) {
         }
      }

      sourceList_.clear();
      return nrSpots;

   }
    
   /**
    *  Function Used By Sub-Classes to create A Specific FitStackThread.  This 
    *  function just needs to return a new instance of FitStackThread.
    * <p>
  Since this was built off of modifying Localization Microscopy, settings are
  transferred from GaussianInfo already.  This means GaussianInfo settings should
  be changed for an instance of ImageRegionFitThread if the desire to change GaussianInfo
  for the return FitStackThread is supposed to be realized.  Any extra initialization should be taken 
  care of in this function (such as extra constructor parameters or validation).
    * 
    * @param sourceList  BlockingQueue of SpotData That is Expected 
    * @param endCondTest End condition test implementation to determine if a 
    *                       sourceList SpotData item is the end condition
    * @param resultList  The synchronized list reference to add matches to
    * @param siPlus      The ImagePlus corresponding to the StackThread (unnecessary?)
    * @param halfSize    The Abbe Limit (in pixels)
    * @param shape       Legacy (parameter for shape)
    * @param fitMode     The fitMode we initially want to set for the fitProcess
    * @return The new instance of a SubClass FitStackThread
    * 
    * @see FitStackThread
    */
   abstract protected FitStackThread createFitStackThreadInstance( final BlockingQueue<SpotData> sourceList, 
                                                                    final BlockingQueueEndCondition<SpotData> endCondTest,
                                                                    final List<SpotData> resultList, ImagePlus siPlus, int halfSize,
                                                                    int shape, FitProcessContainer.OptimizationModes fitMode );
   
   /**
    * Generate Points Of Interest that might possibly be turned into potential SpotData.
    * The ImageProcessor Passed Through will be successive Iterations of each ImageProcessor
    * contained in the ImagePlus stored in the ip_ member.
    * <p>
    * This only generates Points of Interest in (x,y) coordinates, and so allows for a custom 
    * strategy for determining potential points (i.e. peaks, edges, etc.) in extending class
    * <p>
    * Note: The points found will be used to produce a Preliminary Spot with a cropped
    * ImageProcessor of the specified width using setPointOfInterestHalfWidth( int )
    * 
    * @param currentImageProcessor The currentImageProcessor as selected By Iterating through the baseLevel ImagePlus member
    * @return Polygon object that represents all points of interest discovered
    */
   abstract protected Polygon discoverPointsOfInterest( ImageProcessor currentImageProcessor );
   
   /**
    *  Given the x and y coordinates produced from discoverPointsOfInterest, apply 
    *  any further postProcessing to generate value SpotData for evaluation by the given
    *  FitThread.
    * 
    * @param ip - The Image Processor in which the data is correlated
    * @param channel - The Channel Number as parsed by the super class
    * @param slice - The Slice Number as parsed by the super class
    * @param frame - the Frame Number as parsed by the super class
    * @param position - z-position number in the original stack
    * @param spotIdx - The Image Index Number for the area that the spot belongs to
    * @param x - The position in the image(pixels) in x
    * @param y - The position in the image(pixels) in y
    * @return - Returns a valid spot, or <code>null</code> if the Spot is post-filtered to not be valid
    */
   abstract protected SpotData produceSpot(  ImageProcessor ip, int channel,
                                              int slice, int frame, int position, 
                                              int spotIdx, int x, int y );

  /**
   * Custom Comparator Class as copied from FitAllThread.java
   * <p>
   * This Comparator Orders PixelData From left to Right and Top to Bottom (Image)
   */
  private class SpotSortComparator implements Comparator {

      // Return the result of comparing the two row arrays
      @Override
      public int compare(Object o1, Object o2) {
         int[] p1 = (int[]) o1;
         int[] p2 = (int[]) o2;
         if (p1[0] < p2[0]) {
            return -1;
         }
         if (p1[0] > p2[0]) {
            return 1;
         }
         if (p1[0] == p2[0]) {
            if (p1[1] < p2[1]) {
               return -1;
            }
            if (p1[1] > p2[1]) {
               return 1;
            }
         }
         return 0;
      }
   }
    
    
}
