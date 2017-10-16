/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.fitSettingsTest;

import edu.hope.superresolution.fitprocesses.FitProcessContainer;
import edu.hope.superresolution.genericstructures.SettingsDependentObjectBasic;
import edu.hope.superresolution.genericstructures.iSettingsObject;
import edu.valelab.gaussianfit.DataCollectionForm;
import edu.valelab.gaussianfit.algorithm.GaussianFit;
import edu.valelab.gaussianfit.data.GaussianInfo;
import edu.valelab.gaussianfit.data.SpotData;
import edu.valelab.gaussianfit.fitting.ZCalibrator;
import ij.ImagePlus;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import edu.hope.superresolution.genericstructures.BlockingQueueEndCondition;
import java.util.concurrent.Callable;

/**
 * 
 * Generic Super Class that anticipates fitting a specific SpotDataClass structure
 * that has been provided for it.  This was intended to be used with ImageRegionFitThread
 * classes, which isolate a section of data that should be able to be fit to the intended fit.
 * That data, including the pixel data, is packaged into the SpotDataClass and appended to a 
 * BlockingQueue (or Stack) on which a specific FitThread Should be instantiated.
 * The Extension of this Generic Class is meant to anticipate the given SpotDataClass
 * and produce a meaningful SpotDataClass in return in a result list.
 * <p>
 * In this way, Any SpotDataClass Designed, should be treated as a containing members
 * that hold the source to fit and other relevant data and calculations resulting from the fit.
 * In the case of Gaussian fitting, SpotData holds both its Pixel data and after runFitProcess()
 * is filed with data about the sigma of the fit and uncertainties.  This behavior may be
 * modified to more complex behavior, like recognition of a specific marker that was 
 * identified in a previous image perhaps.
 * <p>
 * **Original Design**
 * This is a more General Implementation of The GaussianFitStackThread from edu.valelab.gaussianfit. 
 * Its implementation allows for the use of the previously tested GaussianFit Object from 
 * edu.valelab.gaussianfit.algorithm.  The need for better defocusing parameters
 * and more complex fits with, in general, updated parameters would suggest a basic
 * FitStackThread class that invokes a particular implementation of FitProcess.
 * <p>
 * Use of Getter and Setter Functions for FitProcess is required to maintain uniformity
 * in design.  The FitProcessContainer set at this super level, is expected to be 
 * called in the extending class's implementation of #runFitProcess( SpotDataClass), and 
 * will throw an fatal exception if the FitProcess is not set.  Manipulation of the FitProcess
 * through reference is encouraged in the Extending Class, since each FitProcess will have a varied 
 * implementation and return of parameters.
 * <p>
 * Note:  To this end, GaussianFit has been copied and adopted to match the standard
 * 
 * @param <SettingsClass> The Object that is meant to contain all settings for the Extending Class.
 *                        This is intended to be be Explicitly specified in the extension
 *                        to provide a measure of Type Checking on compile-time and 
 *                        to allow for certainty when called specific properties in the subClass.
 * @param <SpotDataClass> The Object Used to hold the initial data with which to fit 
 *                        (pertinent inputs) and which also holds the results of a sucessful
 *                        fit operation.  The intention is an object that will hold both 
 *                        source and result material in the end.
 * 
 * 
 * @see edu.valelab.gaussianfit.algorithm.GaussianFit
 * @see edu.valelab.gaussianfit.GaussianFitStackThread
 * @see edu.hope.superresolution.fitters.FitProcess
 * 
 * @author Microscope
 */
public abstract class FitStackThread<SettingsClass extends iSettingsObject, SpotDataClass> extends SettingsDependentObjectBasic<SettingsClass> implements Callable<Integer> {

   private Thread t_;
   private boolean stopNow_ = false;
   private BlockingQueue<SpotDataClass> sourceList_;
   private List<SpotDataClass> resultList_;
   private BlockingQueueEndCondition endCondTest_;
   private boolean stop_ = false;
   //protected member for FitProcessContainer Access
   protected FitProcessContainer fitProcess_;
   
   /**
    *  Main Constructor - Sets up all terms for use in Extending Class
    * 
    * @param sourceList  BlockingQueue of SpotDataClass That is Expected 
    * @param endCondTest End condition test implementation to determine if a 
    *                       sourceList SpotDataClass item is the end condition
    * @param resultList  The synchronized list reference to add matches to
    * @param fitProcess  FitProcessContainer to be exposed to extending classes and
    *                    intended for use in #runFitProcess.  Since each fit process is
    *                    unique in parameter returns and orders, the extending class
    *                    is expected to present its own class ahead of time.
    */
   public FitStackThread(BlockingQueue<SpotDataClass> sourceList, 
                            BlockingQueueEndCondition<SpotDataClass> endCondTest, 
                            List<SpotDataClass> resultList, FitProcessContainer fitProcess,
                            SettingsClass settingsObj) {
       super( settingsObj );
      
      //Current context members
      endCondTest_ = endCondTest;
      
   }

   public void listDone() {
      stop_ = true;
   }


   /**
    *  End After Current Iteration on Stack
    */
   public void stop() {
      stopNow_ = true;
   }

   /**
    * Callable call() - Pulls from the blocking Queue provided, analyzes the spotData
    * for features and then appends fitted spots to the resultList that was provided
    * from the instantiating context.
    * 
    * @return The number of Spots added to the result List reference by this Callable before returning.
    * @see #init() 
    */
   @Override
   final public Integer call() {

       int numSpots = 0;
       while (!stopNow_) {
           SpotDataClass spot;
           try {
               spot = sourceList_.take();
               // Look for signal that we are done, add back to queue if found
               if (endCondTest_.isEndCondition(spot)) {
                   //TODO: IllegalStateException should be handled so that poison is 
                   //readded
                   sourceList_.add(spot);
                   return numSpots;
               }
           } catch (InterruptedException iExp) {
               ij.IJ.log("Thread interruped  " + Thread.currentThread().getName());
               Thread.currentThread().interrupt(); //Reinterrupt for calling context listeners
               return numSpots;
           }

         //Enforce Use of super-level fitProcessor
         //assert( fitProcess_ != null );
         
         //Implement Abstract Method of Extension
         //This allows for multiple parameter and model changes without threading considerations
         SpotDataClass tempResult;
         //Lock the Settings Object so that extraneous calls don't interrupt it
         //It would be nice to use an isRunning Pattern as well... in the future
         lockSettings();
         try {
            tempResult = runFitProcess( spot );
            if( tempResult != null ) {
                resultList_.add(tempResult);
                numSpots += 1;
            }
         } catch ( IllegalThreadStateException ex ) {
             ij.IJ.log( "Fatal Thread Exception: " + ex.getMessage());
             return numSpots;
         } catch ( Exception ex ) {
             ij.IJ.log( "Non-Fatal Thread Exception: " + ex.getMessage());
         } finally {
             unlockSettings();
         }
      }
       
       return numSpots;
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
    * @see #runFitProcess(edu.valelab.gaussianfit.data.SpotDataClass) 
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
    * @see #runFitProcess(edu.valelab.gaussianfit.data.SpotDataClass) 
    */
   final protected FitProcessContainer getFitProcess( ) {
       return fitProcess_;
   }
   
   /**
    *  SubClass-Specific Fit Process for a given spot from the Stack. This method makes use of
    *  the getFitProcess() FitProcessContainer.  The reason that the FitProcessContainer 
    *  is not just called, is that data returned from the FitProcessContainer is expected to be 
    *  variable in format.  Because of this, logic such as formatting, translating, and
    *  producing the DataObject with the FitProcessContainer is meant to be done in this method as well.
    * <p>
    * Note, all calls to getSettings in this process are thread safe.
    * 
    * @param spot The current bounded Image that is to be evaluated as a single spot from the stack
    *             The X and Y as reportedf from this Spot are Centered on the ImageProcessor
    *             that was cropped (Should be changed to corner for beter flexibility)    *             
    * @return On success, a SpotDataClass Object with the corresponding parameter Data
    *         added to it for the fit.  In the event of no fit, null should be returned.
    *         In the event of an unrecoverable Exception, the function should throw
    *         an IllegalthreadStateException
    * @throws IllegalThreadStateException This Exception must be Thrown for unrecoverable 
    *                                     states in the function.  It will cause the 
    *                                     thread to stop immediately after throwing.
    */
   protected abstract SpotDataClass runFitProcess( SpotDataClass spot ) throws IllegalThreadStateException;
}

