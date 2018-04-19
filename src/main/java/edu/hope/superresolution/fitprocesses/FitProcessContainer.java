/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.fitprocesses;

import ij.process.ImageProcessor;

/**
 * Interface for containers of a fitting algorithm for an imageArea, assumed to be
 * within the approximate Abbe Limit.  This container allows for the setting of 
 * various optimization search methods if they exist, and provides a uniform interface
 * dofit() for any implementer to provide imageAreas (pre-constrained to an assumed
 * Limit, primarily Abbe Limit for single point source, for best fitting).  
 * <p>
 * These FitProcessContainers are intended for use in multiple Spot Evaluations
 * across an image through the use of a <code>FitStackThread</code> object that operates on a 
 * stack of multiple constrained imageAreas (each being successively passed to dofit()).
 * <p>
 * Tips for Implementation: Since this returns an unknown set of Parameters, define 
 * public static final indices for the data being returned for ease of access to the array. 
 * <p>
 * Since this implementation is not as abstract as it can be, any new implementations 
 * must take care to modify code that is calling it, to use its indices and conventions.
 * 
 * @author Justin Hanselman
 */
public interface FitProcessContainer {

    /**
     * Enumerated values Used to indicate the Optimization Processes anticipated 
     * for this class.  This is expected to be expanded as new methods are refined
     * and so should be added to so that new FitProcessContainers might maintain 
     * compatibility with previous iterations while using a new value for setOptimizationMode
     */
    public enum OptimizationModes {
        Simplex(1, "Simplex"),
        Levenberg_Marquardt(2, "Levenberg-Marquardt"),
        Simplex_Maximum_Likelihood(3, "Simplex-MLE"),
        Levenberg_Marquardt_Weighted(4, "Weighted Levenberg-Marquardt");
        
        private final int val_;
        private final String msgKey_;
        
        OptimizationModes( int val, String msg ) {
            val_ = val;
            msgKey_ = msg;
        }
        
        /**
         *  Gets the Corresponding Optimization Mode To the current Base Int.
         * 
         * @param baseInt The Base Int Value as registered with the enumerated Class
         * @return The Corresponding Optimization Mode or null if their is not corresponding Value.
         */
        static public OptimizationModes getOptimizationMode( int baseInt ) {
            for( OptimizationModes mode : values() ) {
                if( mode.equals( baseInt ) ) {
                    return mode;
                }
            }
            //Should be a throw for the sake of uniformity and clarity
            return null;
        }
        
        /**
         * Test for Integer Equality between comparison Value and enum
         * @param compVal The Value to compare
         * @return <code>true</code> if equal
         */
        public boolean equals( int compVal ) {
            return compVal == val_;
        }
        
         /**
         * Test for Message Equality between comparison Value and enum
         * @param compMsg The message String to compare
         * @return <code>true</code> if equal
         */
        public boolean equals( String compMsg ) {
            return msgKey_.equals(compMsg);
        }
 
        /**
         *  Get the underlying Integer Value assigned to a type
         *  @return 
         */
        public final int getIntValue() {
            return val_;
        }
        
        @Override
        public String toString() {
            return "The Enum has integer Value of: " + val_ 
                    + " and Message key Value of " + msgKey_;
        }
        
    };
    
    /**
     *  Performs the current fit Process on the SpotImage and to the number of iterations.
     *  The returned array corresponds to the Parameter Array planned out in the 
     *  implementing class's documentation.  
     * 
     * @param spotImage  The bounded image (expected to be within Abbe Limit for a point source)
     * @param numIterations The number of iterations before a failure is automatically thrown
     * @return {0.0} array if there was not fit performed, or the array of parameters
     */
    public double[] dofit( ImageProcessor spotImage, int numIterations );
    
    /**
     *  Enforces some semblance of parameter checking, by requiring the number of 
     *   parameters being fit to be returned.
     * 
     * @return The number of parameters expected to be in a fit array returned by dofit()
     * 
     * @see #dofit(ImageProcessor, int) 
     */
    public int getNumParams( );
    
    /**
     *  Set the Optimization Mode of the Given fit.  This Selects from the controlled
     *  enumerated class OptimizationModes, and allows for expansion of modes as 
     *  methods are improved and created.  
     * <p>
     *  If a new OptimizationMode is required, please modify the OptimizationModes enumerated
     *  class to reflect this.
     * 
     * @param mode The Mode Intended to be set
     * @return The current Set Mode
     */
    public OptimizationModes setOptimizerMode( OptimizationModes mode );
    
    /**
     *  Gets the Current OptimizationMode for any fitting to be performed
     * @return The Current OptimizationMode registered for dofit()
     */
    public OptimizationModes getOptimizerMode();
}
