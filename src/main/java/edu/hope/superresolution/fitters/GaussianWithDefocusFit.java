/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.fitters;

import edu.hope.superresolution.fitprocesses.FitProcessContainer;
import static edu.hope.superresolution.fitters.GaussianFit.BGR;
import static edu.hope.superresolution.fitters.GaussianFit.INT;
import static edu.hope.superresolution.fitters.GaussianFit.S;
import static edu.hope.superresolution.fitters.GaussianFit.XC;
import static edu.hope.superresolution.fitters.GaussianFit.YC;
import ij.process.ImageProcessor;
import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.PointVectorValuePair;


/**
 * 
 * This Class is an encapsulation of a Gaussian Fit to a diffraction limited spot 
 * with A Defocus Term.  The Encapsulation currently only does so by Levenberg-Marquardt Optimization.
 * <p>
   As opposed to the Legacy Fitter from Micro-manager, this class fits
   an assumed Gaussian with Zernike[2,0] polynomial (defocus) term added.
   The Legacy Convergence Checker helper Class is replicated as well as similar encapsulation
   is implemented as with edu.valelab.gaussianfit.algorithm.GaussianFit.java.
   This takes an area assumed to be within the Airy Disk for evaluation.  Full 
   evaluation with regards to changes in the defocus term is not implemented with 
   regards to convergence, but the unit circle is normalized to a theoretical stable point.  
   Additionally, it is recommended that a Spot Image Processor is within 2*sigma bounds to maximize
   relevance for solving the Defocus term without a Curve.
<p>
 TODO - Determine if adaptation of Simplex or Simplex-MLE is necessary ( as in
 GaussianFit.java)
 *  
 * @see GaussianWithDefocusModel
 * @see edu.valelab.gaussianfit.algorithm.GaussianFit.java
 * 
 * 
 * @author Microscope
 */
public class GaussianWithDefocusFit implements FitProcessContainer {
    
    //Indices Corresponding to Parameters
    public static final int IDX_A0 = 0;
    public static final int IDX_BGR = 1;
    public static final int IDX_XC = 2;
    public static final int IDX_YC = 3;
    public static final int IDX_SIGMA = 4;
    public static final int IDX_ADEFOCUS = 5;
    
    //Convergence Values (To Be Exposed Via get's later)
    private static final double DELTA_INTENSITY_LIM = 10;
    private static final double DELTA_BGR_LIM = 2;
    private static final double DELTA_XC_LIM = .1;
    private static final double DELTA_YC_LIM = .1;
    private static final double DELTA_SIGMA_LIM = .1;
    private static final double DELTA_ADEFOCUS_LIM = .1;  //Arbitrary Value at this point
    
    public static final int numParams_ = 6;
    
    //Levenberg-Marquart Optimizer
    private final OptimizationModes currentOptimizationMode_;
    private LeastSquaresOptimizer lmo_;
    private LMChecker lmChecker_;
    private LeastSquaresBuilder problemBuilderBase_;
    
    private double[] params0_;
        
    public GaussianWithDefocusFit() {
        
        //Establish Modes for Curve Fitting and Create the Reusable Optimizer
        currentOptimizationMode_ = OptimizationModes.Levenberg_Marquardt;
        lmo_ = new LevenbergMarquardtOptimizer();
        lmChecker_ = new LMChecker();
        params0_ = new double [ numParams_ ];
        //Base Problem Created without full Parameters
        problemBuilderBase_ = new LeastSquaresBuilder().
                                                   checkerPair(lmChecker_).
                                                   maxEvaluations(1000).
                                                   lazyEvaluation( false );
    }
    
    
    /**
     * Performs a fit to a given Bounded ImageProcessor but returns the additional
     *  parameter of the Zernike[2,0] term as well.  Implements Class estimate 
     *  parameters function, before calling fit.
     * <p>
     * Only One Gaussian is fit to the ImageProcessor.  The Defocus Coefficient will increase 
     * with expected Defocus.  
     * <p>
     * <pre>
     * Clarifying Notes on Return structure Parameters:
     *      Fits Equation: A*exp(-((x-xC)^2+(y-yC)^2)/(2*sigma^2)) 
     *                          + ADefocus*(2*((x-xC)^2+(y-yC)^2)/(N*sigma)^2 - 1)
     * 
     * 1. ADefocus will be of greater magnitude as Defocus Occurs but is meant for comparative evaluation
     * 2. sigma relates to the Gaussian Waist as defined above
     * 3. N*sigma is the normalization for which the Zernike Coefficient is orthogonal
     * 4. xC and yC may have additional error do to the current Levenberg-Marquardt convergence Checker
     *      This Implies |xC| + DELTA_XC_LIM or |yC| + DELTA_YC_LIM > ||xC|| or ||yC||
     * 5. ALL PHYSICAL VALUES ARE IN IMAGE VALUES (i.e. Pixel Bit Values, Pixel values (decimal))
     * 
     * </pre>
     * @param siProc ImageProcessor of a Spot (Expected to be bounded to anticipated Airy Disk at most)
     * @param maxIterations Maximum number of iterations before failing to fit
     * @return Returns the Parameters of the resulting fit.  {0.0} if no match was found 
     *         or { peak, background, xCenter, yCenter, sigma, ADefocus }
     * 
     * @see GaussianWithDefocusModel
     * @see #estimateParameters(ij.process.ImageProcessor) 
     */
    @Override
    public double[] dofit( ImageProcessor siProc, int maxIterations ) {

        estimateParameters(siProc);

        //This could be Done in Estimate Parameters with less reduncancy
        short[] imagePixels = (short[]) siProc.getPixels();
        int width = siProc.getWidth(), height = siProc.getHeight();
        int numPoints = width * height;
        GaussianWithDefocusModel model = new GaussianWithDefocusModel(numPoints);

        for (int i = 0; i < numPoints; ++i) {
            model.addPixel(i % width, i / width, (imagePixels[i] & 0xffff));
        }
        
        //Create A Least Square Problem from the Base
        LeastSquaresProblem problem = problemBuilderBase_.
                                        start( params0_ ).
                                        model( model ).
                                        target( model.getPixelValues() ).
                                        lazyEvaluation( false ).
                                        maxIterations( maxIterations ).
                                        build();
    
        double[] paramsOut = {0.0};

        //Acquire optimum value
        try {
            LeastSquaresOptimizer.Optimum optimum = lmo_.optimize(problem);
            paramsOut = optimum.getPoint().toArray();
        } catch (ConvergenceException ex) {
            ij.IJ.log("Convergence Exception Occured " + ex.getMessage());
        }
        
        return paramsOut;
        
    }
        
    /**
     * Taking a Bounded Image Processor for a possible Airy Disk, create parameters
     * that are close to the optimum value.
     * <p>
     * Details:
     * <p>
     * <pre>
     *  xC, yC   - Calculated as Centroid of Image
     *  bgr      - Calculated as Average of Image Borders (not ideal, shouuld be replaced
     *             with an estimate of a background area)
     *  sigma    - hard-code to approximation of 1 pixel 
     *              TODO: could be more specifically ( .42*AiryDisk = .42*lambda/(2*NA) )
     *  A0       - Intensity is chosen as the average Pixel Values around the Centroid surrounded by rectangle [Sigma x Sigma]
     *  ADefocus - Currently Hard-coded to 0
     *              TODO: allow for passing of previous Defocus Value Potentially (possible overload)
     * </pre>
     * @param siProc - Current bounded ImageProcessor That Should contain the potential Airy Disk
     */
    /*
    private void estimateParameters( ImageProcessor siProc ) {
        short[] imagePixels = (short[]) siProc.getPixels();

      // Hard code estimate for sigma (expressed in pixels):
      params0_[IDX_SIGMA] = 1.0;
      double bg = 0.0;
      int n = 0;
      int lastRowOffset = (siProc.getHeight() - 1) * siProc.getWidth();
      for (int i = 0; i < siProc.getWidth(); i++) {
         bg += (imagePixels[i] & 0xffff);
         bg += (imagePixels[i + lastRowOffset] & 0xffff);
         n += 2;
      }
      for (int i = 1; i < siProc.getHeight() - 1; i++) {
         bg += (imagePixels[i * siProc.getWidth()] & 0xffff);
         bg += (imagePixels[(i + 1) * siProc.getWidth() - 1] & 0xffff);
         n += 2;
      }
      params0_[IDX_BGR] = bg / n;

      // print("Total signal: " + ti + "Estimate: " + params0_[0]);
      // estimate center of mass
      double mx = 0.0;
      double my = 0.0;
      double mt = 0.0;
      for (int i = 0; i < siProc.getHeight() * siProc.getWidth(); i++) {
         //mx += (imagePixels[i] - params0_[4]) * (i % siProc.getWidth() );
         //my += (imagePixels[i] - params0_[4]) * (Math.floor (i / siProc.getWidth()));
         mt += (imagePixels[i] & 0xffff );  //Overall Sum of Pixel Area
         mx += ((imagePixels[i] & 0xffff)) * (i % siProc.getWidth());
         my += ((imagePixels[i] & 0xffff)) * (Math.floor(i / siProc.getWidth()));
      }
      params0_[IDX_XC] = mx / mt;
      params0_[IDX_YC] = my / mt;
      
      //Take Average of Pixels Around First Sigma at approximate Centroid
      int width = siProc.getWidth(), height = siProc.getHeight();
      int sigma = (int) Math.ceil( params0_[IDX_SIGMA] );
      int colRelStartIdx = (int) (Math.floor( params0_[IDX_XC] - sigma ) );
      int rowStartIdx = (int) (Math.floor( params0_[IDX_YC]-sigma )*width );
      int area = (2*sigma+1)*(2*sigma+1);
      double ti = 0.0;
      //Iterate Across Area Centered on one pixel with sigma to either side
      for( int row0Idx = rowStartIdx; row0Idx <= rowStartIdx + width*2*sigma; row0Idx+= width ) {
          for( int i = colRelStartIdx; i <= colRelStartIdx + 2*sigma; ++i) {
              ti += (imagePixels[row0Idx + i ] & 0xffff);
              n++;
          }
      }
      ti -= ((bg / n) * area);
      params0_[IDX_A0] = ti / area;//(2 * Math.PI * params0_[IDX_SIGMA] * params0_[IDX_SIGMA]);
      params0_[IDX_ADEFOCUS] = 0.0; //Could be a function of the previous Defocus Value
      //ij.IJ.log("Centroid: " + mx/mt + " " + my/mt);
      // set step size during estimate
    }
    */
    
    private void estimateParameters(ImageProcessor siProc) {
      short[] imagePixels = (short[]) siProc.getPixels();

      // Hard code estimate for sigma (expressed in pixels):
      params0_[S] = 0.9;
      double bg = 0.0;
      int n = 0;
      int lastRowOffset = (siProc.getHeight() - 1) * siProc.getWidth();
      for (int i = 0; i < siProc.getWidth(); i++) {
         bg += (imagePixels[i] & 0xffff);
         bg += (imagePixels[i + lastRowOffset] & 0xffff);
         n += 2;
      }
      for (int i = 1; i < siProc.getHeight() - 1; i++) {
         bg += (imagePixels[i * siProc.getWidth()] & 0xffff);
         bg += (imagePixels[(i + 1) * siProc.getWidth() - 1] & 0xffff);
         n += 2;
      }
      params0_[BGR] = bg / n;
      // estimate signal by subtracting background from total intensity
      double mt = 0.0;
      for (int i = 0; i < siProc.getHeight() * siProc.getWidth(); i++) {
         mt += (imagePixels[i] & 0xffff);
      }
      double ti = mt - ((bg / n) * siProc.getHeight() * siProc.getWidth());
      params0_[INT] = ti / (2 * Math.PI * params0_[S] * params0_[S]);
      // print("Total signal: " + ti + "Estimate: " + params0_[0]);
      // estimate center of mass
      double mx = 0.0;
      double my = 0.0;
      for (int i = 0; i < siProc.getHeight() * siProc.getWidth(); i++) {
         //mx += (imagePixels[i] - params0_[4]) * (i % siProc.getWidth() );
         //my += (imagePixels[i] - params0_[4]) * (Math.floor (i / siProc.getWidth()));
         mx += ((imagePixels[i] & 0xffff)) * (i % siProc.getWidth());
         my += ((imagePixels[i] & 0xffff)) * (Math.floor(i / siProc.getWidth()));
      }
      params0_[XC] = mx / mt;
      params0_[YC] = my / mt;
      //ij.IJ.log("Centroid: " + mx/mt + " " + my/mt);
      // set step size during estimate
   }
    

    @Override
    public int getNumParams() {
        return numParams_;
    }

    /**
     *  Sets the mode.  Currently Only Maintains Levenberg-Marquardt Optimazation
     * and so will return regardless of OptimizerInput
     * 
     * @param mode - the desired mode to try and set (In this case will not set)
     * @return returns setMode of those available
     */
    @Override
    public OptimizationModes setOptimizerMode(OptimizationModes mode) {
        return currentOptimizationMode_;
    }

    /**
     * Gets the current Optimization Mode that is still registered for a dofit().
     * This may be used to later check the assumption of a given optimization mode in fitting.
     *  
     * @return Current Optimization Mode that has been set
     */
    @Override
    public OptimizationModes getOptimizerMode() {
        return currentOptimizationMode_;
    }
    
    
    /**
     * Private Helper Class
 
 Taken from GaussianFit and maintains current HardCoded Convergence Values
 TODO:  Evaluate Defocus Coefficient Convergence Limits
     * 
     */
    private class LMChecker implements ConvergenceChecker<PointVectorValuePair> {

        int iteration_ = 0;
        boolean lastResult_ = false;

        @Override
        public boolean converged(int i, PointVectorValuePair previous, PointVectorValuePair current) {
            if (i == iteration_) {
                return lastResult_;
            }

            iteration_ = i;
            double[] p = previous.getPoint();
            double[] c = current.getPoint();

            if (Math.abs(p[IDX_A0] - c[IDX_A0]) < DELTA_INTENSITY_LIM
                    && Math.abs(p[IDX_BGR] - c[IDX_BGR]) < DELTA_BGR_LIM
                    && Math.abs(p[IDX_XC] - c[IDX_XC]) < DELTA_XC_LIM
                    && Math.abs(p[IDX_YC] - c[IDX_YC]) < DELTA_YC_LIM
                    && Math.abs(p[IDX_SIGMA] - c[IDX_SIGMA]) < DELTA_SIGMA_LIM
                    && Math.abs(p[IDX_ADEFOCUS] - c[IDX_ADEFOCUS]) < DELTA_ADEFOCUS_LIM ) {
                lastResult_ = true;
                return true;
            }

            lastResult_ = false;
            return false;
        }
      
    }
}
