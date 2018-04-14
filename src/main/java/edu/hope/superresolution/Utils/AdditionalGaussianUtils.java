/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.Utils;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 *  Modified Utility Class from Localization Microscopy to produce Better Gaussian Results
 * <p>
 *   Additional functions: A Gaussian Background term that is squared to prevent negative background.
 * 
 * @author Microscope
 */
public class AdditionalGaussianUtils {

   public static final int INT = 0;
   public static final int BGR = 1;
   public static final int XC = 2;
   public static final int YC = 3;
   public static final int S = 4;
   public static final int S1 = 4;
   public static final int S2 = 5;
   public static final int S3 = 6;
   
   //initialize a Normal Distribution for generation of Z Scores
   private static final NormalDistribution basicNormalDistribution_ = new NormalDistribution(0, 1);

   /**
    * Produces the Z* associated with a 0 centered NormalDistribution.  This is the 
    * same Z* that is quoted in most z-Score Tables.  Assumes 2Tail distribution.
    * <p>
    * Thread-safe by virtual of finality and local implementation of NormalDistribution functions
    * 
    * @param CI - The Confidence interval (&lt;1 and &gt;0)
    * @return The inverseCumulativeProbability for a 0-centered, 1 std-Dev Gaussian Function (Z*)
    */
   public static double produceZScoreFromCI( double CI ) {
       
       //Adjust CI for one side since this is assumed centered
       CI = Math.abs((1 - CI)/2) + CI;
       
       return basicNormalDistribution_.inverseCumulativeProbability(CI);
       
   }
   
   /**
    * Gaussian function of the form:
    * A *  exp(-((x-xc)^2+(y-yc)^2)/(2 sigy^2))+b^2
    * A = params[INT]  (amplitude)
    * b = params[BGR]  (sqrt(background))
    * xc = params[XC]
    * yc = params[YC]
    * sig = params[S]
    * @param params - Parameters to be optimized
    * @param x - x position in the image
    * @param y - y position in the image
    * @return - array
    */
   public static double gaussianSquareBGR(double[] params, int x, int y) {
      if (params.length < 5) {
                       // Problem, what do we do???
                       //MMScriptException e;
                       //e.message = "Params for Gaussian function has too few values"; //throw (e);
      }

      double exponent = (Math.pow(x - params[XC], 2) + Math.pow(y - params[YC], 2)) 
                                    / (2 * Math.pow(params[S],2));
      double res = params[INT] * Math.exp(-exponent) + Math.pow( params[BGR], 2 );
      return res;
   }

   /**
    * Derivative (Jacobian) of the above function
    *
    * @param params - Parameters to be optimized
    * @param x - x position in the image
    * @param y - y position in the image
    * @return - array with the derivatives for each of the parameters
    */
   public static double[] gaussianSquareBGRJ(double[] params, int x, int y) {
      double q = gaussianSquareBGR(params, x, y) - Math.pow(params[BGR], 2);
      double dx = x - params[XC];
      double dy = y - params[YC];
      double[] result = {
         q/params[INT],
         2*params[BGR],
         dx * q/Math.pow(params[S], 2),
         dy * q/Math.pow(params[S], 2),
         (Math.pow(dx, 2) + Math.pow(dy, 2)) * q/Math.pow(params[S], 3)
      };
      return result;
   }


   /**
    * Gaussian function of the form:
    * f = A * e^(-((x-xc)^2/sigma_x^2 + (y-yc)^2/sigma_y^2)/2) + b^2
    * A = params[INT]  (total intensity)
    * b = params[BGR]  (sqrt(background))
    * xc = params[XC]
    * yc = params[YC]
    * sig_x = params[S1]
    * sig_y = params[S2]
    * 
    * @param params - Parameters to be optimized
    * @param x - x position in the image
    * @param y - y position in the image
    * @return - array
    */
   public static double gaussian2DXYSquareBGR(double[] params, int x, int y) {
      if (params.length < 6) {
                       // Problem, what do we do???
                       //MMScriptException e;
                       //e.message = "Params for Gaussian function has too few values"; //throw (e);
      }

      double exponent = ( (Math.pow(x - params[XC], 2))/(2*Math.pow(params[S1], 2)))  +
              (Math.pow(y - params[YC], 2) / (2 * Math.pow(params[S2], 2)));
      double res = params[INT] * Math.exp(-exponent) + Math.pow(params[BGR], 2);
      return res;
   }

    /**
    * Derivative (Jacobian) of the above function
    *
     *
     * p = A,b,xc,yc,sigma_x,sigma_y
         f = A * e^(-((x-xc)^2/sigma_x^2 + (y-yc)^2/sigma_y^2)/2) + b^2
         J = {
          q/A,
          2*b,
          dx*q/sigma_x^2,
          dy*q/sigma_y^2,
          dx^2*q/sigma_x^3,
          dy^2*q/sigma_y^3
         }
    * @param params - Parameters to be optimized
    * @param x - x position in the image
    * @param y - y position in the image
    * @return - array with the derivates for each of the parameters
    */
   public static double[] gaussianJ2DXYSquareBGR(double[] params, int x, int y) {
      double q = gaussian2DXYSquareBGR(params, x, y) - Math.pow(params[BGR], 2);
      double dx = x - params[XC];
      double dy = y - params[YC];
      double[] result = {
         q/params[INT],
         2*params[BGR],
         dx * q/Math.pow(params[S1], 2),
         dy * q/Math.pow(params[S2], 2),
         Math.pow(dx, 2) * q /Math.pow(params[S1], 3),
         Math.pow(dy, 2) * q /Math.pow(params[S2], 3)
      };
      return result;
   }

   /**
    * Gaussian function of the form:
    * f =  A * e^(-(a*(x-xc)^2 + c*(y-yc)^2 + 2*b*(x-xc)*(y-yc))/2) + B^2
    * A = params[INT]  (total intensity)
    * B = params[BGR]  (sqrt(background))
    * xc = params[XC]
    * yc = params[YC]
    * a = params[S1]
    * b = params[S2]
    * c = params[S3]
    * @param params - Parameters to be optimized
    * @param x - x position in the image
    * @param y - y position in the image
    * @return - array
    */
   public static double gaussian2DEllipsSquareBGR(double[] params, int x, int y) {
      if (params.length < 7) {
                       // Problem, what do we do???
                       //MMScriptException e;
                       //e.message = "Params for Gaussian function has too few values"; //throw (e);
      }

      double exponent = ( (params[S1] * Math.pow(x - params[XC], 2)) +
                          (params[S3] * Math.pow(y - params[YC], 2)) +
                          (2.0 * params[S2] * (x - params[XC]) * (y - params[YC]))
                           ) / 2 ;
      double res = params[INT] * Math.exp(-exponent) + Math.pow(params[BGR], 2);
      return res;
   }


    /**
    * Derivative (Jacobian) of gaussian2DEllips
    * p = A,B,xc,yc,a,b,c
    * J = {
    * q/A,
    * 2*b,
    * (a*dx + b*dy)*q,
    * (b*dx + c*dy)*q,
    * -1/2*dx^2*q,
    * -dx*dy*q,
    * -1/2*dy^2*q
    * }
    * @param params - Parameters to be optimized
    * @param x - x position in the image
    * @param y - y position in the image
    * @return - array with the derivatives for each of the parameters
    */
   public static double[] gaussianJ2DEllipsSquareBGR(double[] params, int x, int y) {
      double q = gaussian2DEllipsSquareBGR(params, x, y) - Math.pow(params[BGR], 2);
      double dx = x - params[XC];
      double dy = y - params[YC];
      double[] result = {
         q/params[INT],
         2 * params[BGR],
         (params[S1] * dx + params[S2] * dy) * q,
         (params[S2] * dx + params[S3] * dy) * q,
         -0.5 * Math.pow(dx, 2) * q,
         -dx * dy * q,
         -0.5 * Math.pow(dy, 2) * q
      };
      return result;
   }

   /**
    * Converts paramers from 2DEllipse fit to theta, sigma_x and sigma_y
    *
    * @param a - params[S1] from Gaussian fit
    * @param b - params[S2] from Gaussian fit
    * @param c - params[S3] from Gaussian fit
    * @return double[3] containing, theta, sigmax, and sigmay in that order
    */
   public static double[] ellipseParmConversion(double a, double b, double c) {
      double[] result = new double[3];

      double u = (a - c) / b;
      double m = (-u + Math.sqrt(Math.pow(u, 2) + 1)) / 2.0;
      result[0] = Math.atan(m);

      double costheta = Math.cos(result[0]);
      double sintheta = Math.sin(result[0]);

      result[1] = Math.sqrt((Math.pow(costheta, 2) - Math.pow(sintheta, 2)) / ((costheta * a) - (sintheta * c)) );
      result[2] = Math.sqrt((Math.pow(costheta, 2) - Math.pow(sintheta, 2)) / ((costheta * c) - (sintheta * a)) );
      
      /*
      double c0 = Math.sqrt(0.5 + (a-c) / 2 * (Math.sqrt(sqr(a-c) + 4 * sqr(b))));
      double s0 = sqr(1- sqr(c0));

      result[1] = 1 / (a + c + b/(c0+s0));
      result[2] = 1 / (a + c - b/(c0+s0));

      if (result[2] > result[1])
         result[0] = Math.asin(c0);
      else
         result[0] = Math.acos(c0);
      */
      return result;
   }

}