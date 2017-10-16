/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.fitSettingsTest;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.Utils.AdditionalGaussianUtils;
import edu.hope.superresolution.Utils.ThompsonGaussianEstimationUtil;
import edu.hope.superresolution.fitprocesses.FitProcessContainer;
import edu.hope.superresolution.fitters.GaussianFit;
import edu.valelab.gaussianfit.DataCollectionForm;
import edu.valelab.gaussianfit.data.SpotData;
import edu.valelab.gaussianfit.fitting.ZCalibrator;
import edu.valelab.gaussianfit.utils.GaussianUtils;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.micromanager.utils.ReportingUtils;
import edu.hope.superresolution.genericstructures.BlockingQueueEndCondition;

/**
 *
 * @author Microscope
 */
public class GaussianFitStackThread extends FitStackThread<ExtendedGaussianInfo, SpotData> {
    
    private FitProcessContainer fitProcess_;  //The Container with various Fit Settings
    
    /**
     *  Fit Thread to Perform a Gaussian Fit to a Preselected Spot.  It is Assumed that the 
     *   Spot's imageProcessor's will be scaled to suitable analysis size and relatively centered
     *   on the spot.
     * 
     * @param sourceList - The BlockingQueue of SpotData to pull preliminary unfit Spots from
     * @param endCondTest - A BlockingQueueEndCondition object with method for identifying the endCondition of the BlockingQueue
     * @param resultList - The synchronized List to append FittedSpots to
     * @param settings - The settings Object that this Thread will operate off of
     */
    public GaussianFitStackThread(BlockingQueue<SpotData> sourceList, 
                                BlockingQueueEndCondition<SpotData> endCondTest,
                                List<SpotData> resultList,
                                ExtendedGaussianInfo settings ) {
        super(sourceList, endCondTest, resultList, null, settings );
        //Later Initialization of FitProcessContainer for sake of readability
        //Get the Settings Object For reference 
        ExtendedGaussianInfo sett = getSettingsRef();
        int shape = sett.getShape();
        //New Structure means this can be a part of each settings object, but for now we will keep it here
        FitProcessContainer.OptimizationModes fitMode = FitProcessContainer.OptimizationModes.getOptimizationMode(sett.getFitMode());
        double baseLevel = sett.getBaseLevel();
        fitProcess_ = new GaussianFit( shape, fitMode, baseLevel );
        
    }
    
    @Override
    protected SpotData runFitProcess(SpotData spot) throws IllegalThreadStateException {
        try {
            // Note: the implementation assumes there is a non-null spot to return a cached version of the ImageProcessor
            ImageProcessor ip = spot.getImageProcessor();
            //make this halfWidth for half-size
            int iWidth = ip.getWidth();
            int iHeight = ip.getHeight();
            int halfWidth = iWidth/2;
            //Get Settings Object
            ExtendedGaussianInfo sett = getSettingsRef();
            //Instantiate all variables now, in the event that settingsLocks are changed in the future
            //These should be thread-safe given locking implementation in FitStackThread
            int maxIterations = sett.getMaxIterations();
            double pixelSize = sett.getPixelSize();
            double baseLevel = sett.getBaseLevel();
            int noiseTolerance = sett.getNoiseTolerance();
            double widthMin = sett.getSigmaMin(), widthMax = sett.getSigmaMax();
            double cPCF = sett.getPhotonConversionFactor() / sett.getGain();
            
            //Perform the Fit on the ImageProcessor For the Spot
            double[] paramsOut = fitProcess_.dofit(ip, maxIterations);
            // Note that the copy constructor will not copy pixel data, so we loose those when spot goes out of scope
            BoundedSpotData spotData = new BoundedSpotData( spot, pixelSize );
            double sx = 0;
            double sy = 0;
            double a = 1;
            double theta = 0;                              
            if (paramsOut.length >= 5) {
               /*//Check That Residuals are Within the assumed 3 * Noise Amplitude
               if( getRMSresiduals( ip, paramsOut ) > 3 * getNoiseTolerance() ) {
                   ij.IJ.log("Residuals are greater than 2 times NoiseTolerance");
                   return null;
               }*/
               
               //Total Number of Photons in the theoretical Distribution
               double N = cPCF * paramsOut[GaussianFit.INT]
                       * (2 * Math.PI * paramsOut[GaussianFit.S] * paramsOut[GaussianFit.S]);
               //ij.IJ.log("Int Raw is: " + paramsOut[GaussianFit.INT]);
               
               double s = Math.abs(paramsOut[GaussianFit.S]) * pixelSize;

               double xMax = (paramsOut[GaussianFit.XC] - halfWidth + spot.getX()) * pixelSize;
               double yMax = (paramsOut[GaussianFit.YC] - halfWidth + spot.getY()) * pixelSize;
               // express background in photons after base level correction
               // From Thompson Paper, We Propagate Noise + any dilineation from BaseLevel in the fit
               double bgr = cPCF * Math.sqrt( Math.pow( Math.pow(paramsOut[GaussianFit.BGR], 2) - baseLevel, 2) + Math.pow( noiseTolerance, 2 ));
               // calculate error using formula from Thompson et al (2002)
               // (dx)2 = (s*s + (a*a/12)) / N + (8*pi*s*s*s*s * b*b) / (a*a*N*N)
               //ij.IJ.log("Sigma is: " + s + "\nBackground is: " + bgr + "\nNumPhotons: " + N + "\n PixelSize: " + pixelSize_);
               double uncertaintyPos = ThompsonGaussianEstimationUtil.calculateLateralUncertainty( s, bgr, N, pixelSize );
               //ij.IJ.log( "Lateral Uncertainty is: " + uncertaintyPos);
               double uncertaintyNumPhotons = ThompsonGaussianEstimationUtil.calculatePhotonUncertainty( N, s, bgr, pixelSize );
               
               if (paramsOut.length >= 6) {
                  sx = paramsOut[GaussianFit.S1] * pixelSize;
                  sy = paramsOut[GaussianFit.S2] * pixelSize;
                  a = sx / sy;
                  
               }

               if (paramsOut.length >= 7) {
                  theta = paramsOut[GaussianFit.S3];
               }
               
               double width = 2 * s;
                         
               spotData.setMaxIntensity( paramsOut[GaussianFit.INT] );
               spotData.setMaxIntensityUncertainty( uncertaintyNumPhotons/
                                                        (2*Math.PI*paramsOut[GaussianFit.S] * paramsOut[GaussianFit.S]) );
               spotData.setNumPhotonUncertainty( uncertaintyNumPhotons );
               spotData.setData(N, bgr, xMax, yMax, 0.0, width, a, theta, uncertaintyPos);
               //ij.IJ.log( "Spot Fit: " + spotData.getX() + ", " + spotData.getY() +  " and width = " + spotData.getWidth() + " uncertainty = " + spotData.getSigma() );
               if( width > widthMin && width < widthMax && uncertaintyPos < width) {                       
                  return spotData;
               }

            } else {
                //ij.IJ.log("Spot Fit Fell Through");
            }
         } catch (Exception ex) {
            ReportingUtils.logError(ex);
            ReportingUtils.logError("Thread run out of memory  " + 
                    Thread.currentThread().getName());
            ReportingUtils.showError("Fitter out of memory.\n" +
                    "Out of memory error");
            throw new IllegalThreadStateException( ex.getMessage() );
         }

        return null;
    }
    
    //Calculates the RMS of the residucals given the final Params and modes
    double getRMSresiduals( ImageProcessor siProc, double[] finalParams ) {
       
       int xWidth = siProc.getWidth();
       int yHeight = siProc.getHeight();
       double rmsResidual = 0.0;
       
       ExtendedGaussianInfo sett = getSettingsRef();
       int mode = sett.getFitMode();
       
       if (mode == 1) {
          for (int i = 0; i < xWidth; i++) {
             for (int j = 0; j < yHeight; j++) {
                rmsResidual += GaussianUtils.sqr(AdditionalGaussianUtils.gaussianSquareBGR(finalParams, i, j)
                                                           - siProc.get(i, j) );
             }
          }
       } else if (mode == 2) {
          for (int i = 0; i < xWidth; i++) {
             for (int j = 0; j < yHeight; j++) {
                rmsResidual += GaussianUtils.sqr(AdditionalGaussianUtils.gaussian2DXYSquareBGR(finalParams, i, j) 
                                                            - siProc.get(i, j));
             }
          }
       } else if (mode == 3) {
          for (int i = 0; i < xWidth; i++) {
             for (int j = 0; j < yHeight; j++) {
                rmsResidual += GaussianUtils.sqr(AdditionalGaussianUtils.gaussian2DEllipsSquareBGR(finalParams, i, j)
                                                            - siProc.get(i, j));
             }
          }
       }
       
       return Math.sqrt(rmsResidual/(xWidth*yHeight));
    }

    
}
