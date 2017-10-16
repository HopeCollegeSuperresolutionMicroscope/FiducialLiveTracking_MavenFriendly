package edu.hope.superresolution.fitters;

import edu.hope.superresolution.fitprocesses.FitProcessContainer;
import edu.valelab.gaussianfit.DataCollectionForm;
import edu.valelab.gaussianfit.algorithm.GaussianFit;
import edu.valelab.gaussianfit.data.GaussianInfo;
import edu.valelab.gaussianfit.data.SpotData;
import edu.valelab.gaussianfit.fitting.ZCalibrator;
import ij.ImagePlus;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import edu.hope.superresolution.genericstructures.BlockingQueueEndCondition;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * 
 * This is a more General Implementation of The GaussianFitStackThread from edu.valelab.gaussianfit. 
 * Its implementation allows for the use of the previously tested GaussianFit Object from 
 * edu.valelab.gaussianfit.algorithm.  The need for better defocusing parameters
 * and, in general, updated parameters would suggest a basic FitStackThread class
 * that invokes a particular implementation of FitProcess.
 * <p>
 * Use of Getter and Setter Functions for FitProcess is required to maintain uniformity
 * in design.  The FitProcessContainer set at this super level, is expected to be 
 * called in the extending class's implementation of #runFitProcess( SpotData), and 
 * will throw an fatal exception if the FitProcess is not set.  Manipulation of the FitProcess
 * through reference is encouraged in the Extending Class, since each FitProcess will have a varied 
 * implementation and return of parameters.
 * <p>
 * Note:  To this end, GaussianFit has been copied and adopted to match the standard
 * 
 * @see edu.valelab.gaussianfit.algorithm.GaussianFit
 * @see edu.valelab.gaussianfit.GaussianFitStackThread
 * @see edu.hope.superresolution.fitters.FitProcess
 * 
 * @author Microscope
 */
public abstract class FitStackThread extends GaussianInfo implements Runnable {

   private Thread t_;
   private boolean stopNow_ = false;
   private BlockingQueueEndCondition endCondTest_;
   //protected member for FitProcessContainer Access
   protected FitProcessContainer fitProcess_;
   
   /**
    *  Main Constructor - Sets up all terms for use in Extending Class
    * 
    * @param sourceList  BlockingQueue of SpotData That is Expected 
    * @param endCondTest End condition test implementation to determine if a 
    *                       sourceList SpotData item is the end condition
    * @param resultList  The synchronized list reference to add matches to
    * @param siPlus      The ImagePlus corresponding to the StackThread (unnecessary?)
    * @param halfSize    The Abbe Limit (in pixels)
    * @param shape       Legacy (parameter for shape)
    * @param fitProcess  FitProcessContainer to be exposed to extending classes and
    *                    intended for use in #runFitProcess.  Since each fit process is
    *                    unique in parameter returns and orders, the extending class
    *                    is expected to present its own class ahead of time.
    * @param fitMode     The fitMode we initially want to set for the fitProcess
    */
   public FitStackThread(BlockingQueue<SpotData> sourceList, 
           BlockingQueueEndCondition<SpotData> endCondTest,
           List<SpotData> resultList, ImagePlus siPlus, int halfSize,
           int shape, FitProcessContainer fitProcess,
           FitProcessContainer.OptimizationModes fitMode) {
      //GaussianInfo Protected Members
      //Most seems unnessary
      siPlus_ = siPlus;
      halfSize_ = halfSize;
      sourceList_ = sourceList;
      resultList_ = resultList;
      shape_ = shape;
      fitMode_ = fitMode.getIntValue();
      
      //Current context members
      endCondTest_ = endCondTest;
      //If we want to initialize this immediately, otherwise it is the burden of
      // setFitProcess() to be called
      if( fitProcess != null ) {
        fitProcess_ = fitProcess;
        fitProcess_.setOptimizerMode(fitMode);
      }
      
   }

   public void listDone() {
      stop_ = true;
   }

   /**
    * Create and Start the Thread
    * 
    * @deprecated This method allows for rampant growth of threads and will be completely removed
    *             in favor of using ThreadPools in the future.
    */
   public void init() {
      stopNow_ = false;
      t_ = new Thread(this);
      t_.start();
   }

   /**
    *  End After Current Iteration on Stack
    */
   public void stop() {
      stopNow_ = true;
   }

   /**
    *  Join the current Thread that was created
    * @throws InterruptedException 
    * 
    * @deprecated This method allows for rampant growth of threads and will be completely removed
    *             in favor of using ThreadPools in the future.
    */
   public void join() throws InterruptedException {
      if (t_ != null)
         t_.join();
   }

   /**
    * Runnable run() - meant to be invoked by use of init() only
    * 
    * @see #init() 
    */
   @Override
   final public void run() {
      GaussianFit gs_ = new GaussianFit(shape_, fitMode_);
      double cPCF = photonConversionFactor_ / gain_;
      ZCalibrator zc = DataCollectionForm.zc_;

      while (!stopNow_) {
         SpotData spot;
         synchronized (gfsLock_) {
            try {
               spot = sourceList_.take();
               // Look for signal that we are done, add back to queue if found
               if ( endCondTest_.isEndCondition(spot) ) {
                  sourceList_.add(spot);
                  return;
               }
            } catch (InterruptedException iExp) {
               ij.IJ.log("Thread interruped  " + Thread.currentThread().getName());
               return;
            }
         }

         //Enforce Use of super-level fitProcessor
         assert( fitProcess_ != null );
         
         //Implement Abstract Method of Extension
         //This allows for multiple parameter and model changes without threading considerations
         SpotData tempResult;
         try {
            tempResult = runFitProcess( spot );
            if( tempResult != null ) {
                resultList_.add(tempResult);
            }
         } catch ( IllegalThreadStateException ex ) {
             ij.IJ.log( "Fatal Thread Exception: " + ex.getMessage());
             return;
         } catch ( Exception ex ) {
             ij.IJ.log( "Non-Fatal Thread Exception: " + ex.getMessage());
         }
      }
   }
  
   /**
    *  Sets the fit Process for the stack Thread.  Protected nature requires only 
    *  extending class manipulation.  This is the case for non-trivial constructors
    *  for a fitProcess.  
    * <p> 
    *  Note: This Does Not Guarantee the FitProcess is Used, but it allows for a uniform implementation
    *        Use of setFitProcess allows for uniform access to the fitProcessContainer.
    *        Other Objects may be Created, since their use in runFitProcess() is ambiguous
    * 
    * @param fitProcess The fitProcess to be made available through fitProcess_ or getFitProcess
    * 
    * @see #runFitProcess(edu.valelab.gaussianfit.data.SpotData) 
    */
   final protected void setFitProcess( FitProcessContainer fitProcess ) {
       fitProcess_ = fitProcess;
   }
   
   /**
    *  Get the FitProcessContainer set to the object.  This is meant to be used mainly 
    *  to provide uniform access to a FitProcess in extending Classes as they implement 
    *  their runFitProcess() method
    * 
    * @return The FitProcessContainer intended to be used
    * @see #runFitProcess(edu.valelab.gaussianfit.data.SpotData) 
    */
   final protected FitProcessContainer getFitProcess( ) {
       return fitProcess_;
   }
   
   /**
    *  SubClass-Specific Fit Process for a given spot from the Stack.  This Process
    *   allows for extending classes to call their own FitProcessContainers and handle 
    *   the data respectively.  
    * 
    * @param spot The current bounded Image that is to be evaluated as a single spot from the stack
    *             The X and Y as reportedf from this Spot are Centered on the ImageProcessor
    *             that was cropped (Should be changed to corner for beter flexibility)    *             
    * @return On success, a SpotData Object with the corresponding parameter Data
    *         added to it for the fit.  In the event of no fit, null should be returned.
    *         In the event of an unrecoverable Exception, the function should throw
    *         an IllegalthreadStateException
    * @throws IllegalThreadStateException This Exception must be Thrown for unrecoverable 
    *                                     states in the function.  It will cause the 
    *                                     thread to stop immediately after throwing.
    */
   protected abstract SpotData runFitProcess( SpotData spot ) throws IllegalThreadStateException;
}

