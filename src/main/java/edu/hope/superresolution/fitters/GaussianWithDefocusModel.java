/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.fitters;

import static edu.hope.superresolution.fitters.GaussianWithDefocusFit.IDX_A0;
import static edu.hope.superresolution.fitters.GaussianWithDefocusFit.IDX_ADEFOCUS;
import static edu.hope.superresolution.fitters.GaussianWithDefocusFit.IDX_BGR;
import static edu.hope.superresolution.fitters.GaussianWithDefocusFit.IDX_SIGMA;
import static edu.hope.superresolution.fitters.GaussianWithDefocusFit.IDX_XC;
import static edu.hope.superresolution.fitters.GaussianWithDefocusFit.IDX_YC;
import edu.valelab.gaussianfit.utils.GaussianUtils;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

/**
 * This Class is an updated Version for Apache Commons Math3
 * 
 *   As opposed to the Legacy Fitter from Micro-manager, this class fits
 *   an assumed Gaussian with Zernike[2,0] polynomial (defocus) term added
 *   This takes a search area of the airy disk, and may be shrunk to improve Quadratic Fit.
 *   May be convoluted with an edge defining kernel to better represent points.
 * @author Justin Hanselman
 */
class GaussianWithDefocusModel implements MultivariateJacobianFunction {

    //Helper Class for Integer-Based Image Coordinates
    class PixelVector2D {

        int x_;
        int y_;

        public PixelVector2D(int x, int y) {
            x_ = x;
            y_ = y;
        }

        public int getX() {
            return x_;
        }

        public int getY() {
            return y_;
        }

    }
        
    private final PixelVector2D[] pixelPoints_;
    private final double[] pixelValues_;
    private int numPixels_ = 0; //used for length tracking
    double N_ = 6;  //Determined Theoretically

    /**
     * Constructor - Variable Zernike Unit Circle Multiplier
     * 
     * @param length The Fixed Length of data points expected to be added
     * @param N The multiple times sigma that Zernike Polynomial will be valid across (r/N*sigma)
     */ 
    public GaussianWithDefocusModel(int length, double N) {
        pixelPoints_ = new PixelVector2D[length];
        pixelValues_ = new double[length];
        N_ = N;
    }

    /**
     *  Constructor - Fixed Zernike Circle Multiplier (N = 6)
     * 
     * @param length The Fixed Length of data points expected to be added
     */
    public GaussianWithDefocusModel(int length) {
        pixelPoints_ = new PixelVector2D[length];
        pixelValues_ = new double[length];
    }

    /**
     *
     * Returns A Real Vector of Parameters and A Real Matrix of Jacobians
     * <p>
     * This Function takes Advantage of previous GaussianUtils.gaussian() 1D
     * function
     * <p>
     * Equation of Form:
     * <p>
     * I(x,y) = A0*exp( -((x-xC)^2+(y-yC)^2)/(2*sigma^2) ) +
     * Ad*(2*((x-xC)^2+(y-yC)^2)/(N*sigma)^2 -1) + BGR
     * <p>
     * <
     * pre>
     *   Parameters:
     *      x,y    - observed point coordinates of form Ii = I(xi, yi)
     *      N      - Fraction of sigma for the Defocus to normalize to a unit circle
     *      Variable Parameters (params):
     *      A0     - Maximum Intensity of Gaussian Peak
     *      BGR    - The Background Intensity
     *      xC, yC - respective centers of the Gaussian
     *      sigma  - standard Deviation of the Gaussian waist
     *      Ad     - Defocus Coefficient ( the larger the magnitude, the more defocused)
     *
     * @param params The Parameters being optimized: [ A0, BGR, xC, yC, sigma, Ad ]
     * @return RealVector - is corresponding estimate of function value given
     * the current params for each (x, y) value RealMatrix - A matrix of the
     * Jacobian of each Jacobian in regards to (x,y) point and with partials
     * taken in regards to parameters
     */
    @Override
    public Pair<RealVector, RealMatrix> value(RealVector params) {

        double a0 = params.getEntry(IDX_A0);
        double bgr = params.getEntry(IDX_BGR);
        double xC = params.getEntry(IDX_XC);
        double yC = params.getEntry(IDX_YC);
        double sigma = params.getEntry(IDX_SIGMA);
        double aDefocus = params.getEntry(IDX_ADEFOCUS);

        RealVector values = new ArrayRealVector(numPixels_);
        RealMatrix jacobian = new Array2DRowRealMatrix(numPixels_, 6);  //Size of Jacobian = size of params

        double curGaussian;

        //Iterate Through all saved PixelPoints
        for (int i = 0; i < numPixels_; ++i) {
            PixelVector2D pixel = pixelPoints_[i];
            int x = pixel.getX();
            int y = pixel.getY();
            double dx = x - xC;
            double dy = y - yC;

            curGaussian = GaussianUtils.gaussian(params.toArray(), x, y);
            //Set the Current Value
            values.setEntry(i, curGaussian
                    + aDefocus * (2 * (Math.pow(dx, 2) + Math.pow(dy, 2)) / Math.pow(N_ * sigma, 2) - 1));

            //Set the Jacobian (for less Arithmetic operations, don't use GaussianJ)
            double q = curGaussian - bgr;
            double p = 4 * aDefocus / Math.pow(N_, 2);
            //Modify Gaussian Jacobian parameters
            jacobian.setEntry(i, IDX_A0, q / a0); // d/dA0
            jacobian.setEntry(i, IDX_BGR, 1.0); // d/dBGR
            jacobian.setEntry(i, IDX_XC, dx / Math.pow(sigma, 2) * (q - p)); // d/dxC
            jacobian.setEntry(i, IDX_YC, dy / Math.pow(sigma, 2) * (q - p)); // d/dyC
            jacobian.setEntry(i, IDX_SIGMA, (Math.pow(dx, 2) + Math.pow(dy, 2)) / Math.pow(sigma, 3)
                    * (q - p));  // d/dsigma
            jacobian.setEntry(i, IDX_ADEFOCUS, 2 * ((Math.pow(dx, 2)
                    + Math.pow(dy, 2)) / Math.pow(N_ * sigma, 2) - 1)); // d/daDefocus

        }

        return new Pair< RealVector, RealMatrix>(values, jacobian);

    }

    /**
     * Add a Pixel to the underlying model sets:
     * <p>
     * <pre>
     * pixelPoints_ - Array of Pixel Integer Values pixelValues_ - Array of
     * Values ("target" for Least Squares Problem)
     * </pre>
     * <p>
     * Note: These Arrays are synchronized to their ith values through use of
     * this function
     *
     * @param x integer X location (image coordinates)
     * @param y integer Y location (image coordinates)
     * @param value intensity value measured at the coordinate
     * @return <code>true</code> if added to arrays or <code>false</code> if
     * array is full
     */
    public boolean addPixel(int x, int y, double value) {
        if (numPixels_ < pixelPoints_.length) {
            pixelPoints_[numPixels_] = new PixelVector2D(x, y);
            pixelValues_[numPixels_] = value;
            numPixels_++;
            return true;
        }

        return false;
    }

    /**
     * Retrieve the pixelValues_ array. "Target" for LeastSquares Builder.
     * <p>
     * Note: This returns a reference to the internal array to avoid copying.
     * Not Thread-safe nor recommended to manipulate data outside of using
     * addPixel()
     *
     * @return array of doubles to PixelValues (non-copied for speed)
     * 
     * @see #addPixel(int, int, double) 
     */
    public double[] getPixelValues() {
        return pixelValues_;
    }
    
}
