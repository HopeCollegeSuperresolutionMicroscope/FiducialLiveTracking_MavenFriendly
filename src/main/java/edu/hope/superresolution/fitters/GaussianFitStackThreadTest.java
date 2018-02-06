/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.fitters;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.Utils.AdditionalGaussianUtils;
import edu.hope.superresolution.Utils.IJMMReportingUtils;
import edu.hope.superresolution.Utils.ThompsonGaussianEstimationUtil;
import edu.hope.superresolution.fitprocesses.FitProcessContainer;
import edu.hope.superresolution.genericstructures.BlockingQueueEndConditionTest;
import edu.valelab.gaussianfit.DataCollectionForm;
import edu.valelab.gaussianfit.data.SpotData;
import edu.valelab.gaussianfit.fitting.ZCalibrator;
import edu.valelab.gaussianfit.utils.GaussianUtils;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.micromanager.utils.ReportingUtils;

/**
 *
 * @author Microscope
 */
public class GaussianFitStackThreadTest extends FitStackThread {

    //Public Keys for SpotData
    public static final String NUM_PHOTON_UNCERTAINTY = "TEST THIS";//GaussianFitStackThread.class.getName() + ".NumPhotonUncertainty";
    public static final String INTENSITY_MAX_UNCERTAINTY = "TEST THIS TOO"; //GaussianFitStackThread.class.getName() + ".IntensityMaxUncertainty";
    public static final String INTENSITY_MAX = "TEST THIS THREE";//GaussianFitStackThread.class.getName() + ".IntensityMax";
    
    private final double cPCF = photonConversionFactor_ / gain_;
    private final ZCalibrator zc = DataCollectionForm.zc_;
    //private final double sigmaMin_;  //This is Equal to Abbe Limit/2 (or half-Size search Area/2) (approx Gaussian Sigma)
    
    /**
     *  Fit Thread to Perform a Gaussian Fit to a Preselected Spot.  It is Assumed that the 
     *   Spot's imageProcessor's will be scaled to suitable analysis size and relatively centered
     *   on the spot.
     * 
     * @param sourceList - The BlockingQueue of SpotData to pull preliminary unfit Spots from
     * @param endCondTest - A BlockingQueueEndConditionTest object with method for identifying the endCondition of the BlockingQueue
     * @param resultList - The synchronized List to append FittedSpots to
     * @param siPlus - The imagePlus associated with the fitting
     * @param resolveLimit - The limit in which Gaussians are considered discernable (This was expected to be the Abbe Limit originally), provides a minimum fitWidth
     * @param shape - Whether the fit is meant to be circular, oval in x and y, or elliptical
     * @param fitMode - The mode used to establish the fit
     */
    public GaussianFitStackThreadTest(BlockingQueue<SpotData> sourceList, 
                                BlockingQueueEndConditionTest<SpotData> endCondTest,
                                List<SpotData> resultList, ImagePlus siPlus, 
                                int resolveLimit, int shape,
                                FitProcessContainer.OptimizationModes fitMode) {
        super(sourceList, endCondTest, resultList, siPlus, resolveLimit, shape, null, fitMode);
        //Later Initialization of FitProcessContainer for sake of readability
        setFitProcess( new GaussianFit( shape, fitMode, baseLevel_ ) );
        //sigmaMin_ = resolveLimit / 2;
        
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
            double[] paramsOut = getFitProcess().dofit(ip, maxIterations_);
            // Note that the copy constructor will not copy pixel data, so we loose those when spot goes out of scope
            BoundedSpotData spotData = new BoundedSpotData( spot, getPixelSize() );
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
               
               double s = Math.abs(paramsOut[GaussianFit.S]) * pixelSize_;

               double xMax = (paramsOut[GaussianFit.XC] - halfWidth + spot.getX()) * pixelSize_;
               double yMax = (paramsOut[GaussianFit.YC] - halfWidth + spot.getY()) * pixelSize_;
               // express background in photons after base level correction
               // From Thompson Paper, We Propagate Noise + any dilineation from BaseLevel in the fit
               double bgr = cPCF * Math.sqrt( Math.pow( Math.pow(paramsOut[GaussianFit.BGR], 2) - baseLevel_, 2) + Math.pow( noiseTolerance_, 2 ));
               // calculate error using formula from Thompson et al (2002)
               // (dx)2 = (s*s + (a*a/12)) / N + (8*pi*s*s*s*s * b*b) / (a*a*N*N)
               //ij.IJ.log("Sigma is: " + s + "\nBackground is: " + bgr + "\nNumPhotons: " + N + "\n PixelSize: " + pixelSize_);
               double uncertaintyPos = ThompsonGaussianEstimationUtil.calculateLateralUncertainty( s, bgr, N, pixelSize_ );
               //ij.IJ.log( "Lateral Uncertainty is: " + uncertaintyPos);
               double uncertaintyNumPhotons = ThompsonGaussianEstimationUtil.calculatePhotonUncertainty( N, s, bgr, pixelSize_ );
               
               if (paramsOut.length >= 6) {
                  sx = paramsOut[GaussianFit.S1] * pixelSize_;
                  sy = paramsOut[GaussianFit.S2] * pixelSize_;
                  a = sx / sy;
                  
                  /*double z = 0.0;              
               
                  if (zc.hasFitFunctions()) {
                     z = zc.getZ(2 * sx, 2 * sy);
                     spotData.setZCenter(z);
                  }*/
                  
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
               if( width > widthMin_ && width < widthMax_ && uncertaintyPos < width) {                       
                  return spotData;
               }

            } else {
                //ij.IJ.log("Spot Fit Fell Through");
            }
         } catch (Exception ex) {
            IJMMReportingUtils.logError(ex);
            IJMMReportingUtils.logError("Thread run out of memory  " + 
                    Thread.currentThread().getName());
            IJMMReportingUtils.showError("Fitter out of memory.\n" +
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
       
       if (mode_ == 1) {
          for (int i = 0; i < xWidth; i++) {
             for (int j = 0; j < yHeight; j++) {
                rmsResidual += GaussianUtils.sqr(AdditionalGaussianUtils.gaussianSquareBGR(finalParams, i, j)
                                                           - siProc.get(i, j) );
             }
          }
       } else if (mode_ == 2) {
          for (int i = 0; i < xWidth; i++) {
             for (int j = 0; j < yHeight; j++) {
                rmsResidual += GaussianUtils.sqr(AdditionalGaussianUtils.gaussian2DXYSquareBGR(finalParams, i, j) 
                                                            - siProc.get(i, j));
             }
          }
       } else if (mode_ == 3) {
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
