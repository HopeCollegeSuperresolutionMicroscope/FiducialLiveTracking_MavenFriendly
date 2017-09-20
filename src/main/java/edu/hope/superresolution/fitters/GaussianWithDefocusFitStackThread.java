/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.fitters;

import edu.hope.superresolution.fitprocesses.FitProcessContainer;
import edu.hope.superresolution.genericstructures.BlockingQueueEndConditionTest;
import edu.valelab.gaussianfit.data.SpotData;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.micromanager.utils.ReportingUtils;

/**
 *
 * @author Microscope
 */
public class GaussianWithDefocusFitStackThread extends FitStackThread {

    //Public Key for Storage of Relative Defocus Term in SpotData
    public final static String REL_DEFOCUS_SPOT_KEY = 
                                "GaussianWithDefocusFitStackThread.aDefocusRelative";
    
    public GaussianWithDefocusFitStackThread(BlockingQueue<SpotData> sourceList,
                                                BlockingQueueEndConditionTest<SpotData> endCondTest,
                                                List<SpotData> resultList, ImagePlus siPlus,
                                                int halfSize, int shape, FitProcessContainer.OptimizationModes fitMode) {
        super(sourceList, endCondTest, resultList, siPlus, halfSize, shape, null, fitMode);
        
        //Later Initialization of FitProcessContainer for sake of readability
        setFitProcess( new GaussianWithDefocusFit() );
        
    }

    /**
     *  Runs the Fit Process for the passed spot (from a BlockingQueue in super).
     *  <p>
     *  This Fit Processor Only applies One Levenberg-Marquardt Optimization to an
     *  expected Gaussian + Adefocus*Zernike[2,0].  For Further Details see, GaussianWithDefocusFit 
     *  and GaussianWithDefocusModel for parameter Explanations.  The process
     *  stores the extra term as a key from DEFOCUS_SPOT_KEY.
     * 
     * @param spot 
     * @return
     * @throws IllegalThreadStateException 
     */
    @Override
    protected SpotData runFitProcess(SpotData spot) throws IllegalThreadStateException {
        
        double cPCF = photonConversionFactor_ / gain_;
        
        try {
            // Note: the implementation will try to return a cached version of the ImageProcessor
            ImageProcessor ip = spot.getImageProcessor();
            int halfwidth = ip.getWidth()/2;
            double[] paramsOut = getFitProcess().dofit(ip, maxIterations_);
            // Note that the copy constructor will not copy pixel data, so we loose those when spot goes out of scope
            SpotData spotData = new SpotData(spot);
            double a = 1;
            double theta = 0;
            if (paramsOut.length >= 5) {
                double N = cPCF * paramsOut[GaussianWithDefocusFit.IDX_A0]
                        * (2 * Math.PI * paramsOut[GaussianWithDefocusFit.IDX_SIGMA]
                        * paramsOut[GaussianWithDefocusFit.IDX_SIGMA]);
                double xMax = (paramsOut[GaussianWithDefocusFit.IDX_XC] - halfwidth + spot.getX()) * pixelSize_;
                double yMax = (paramsOut[GaussianWithDefocusFit.IDX_YC] - halfwidth + spot.getY()) * pixelSize_;
                double s = paramsOut[GaussianWithDefocusFit.IDX_SIGMA] * pixelSize_;
                // express background in photons after base level correction
                double bgr = cPCF * (paramsOut[GaussianWithDefocusFit.IDX_BGR] - baseLevel_);
                // calculate error using formular from Thompson et al (2002)
                // (dx)2 = (s*s + (a*a/12)) / N + (8*pi*s*s*s*s * b*b) / (a*a*N*N)
                double error = (s * s + (pixelSize_ * pixelSize_) / 12) / N
                        + (8 * Math.PI * s * s * s * s * bgr * bgr) / (pixelSize_ * pixelSize_ * N * N);
                error = Math.sqrt(error);

                /* 
                *  Width is 2*sigma
                *  Abbe Limit correlates to at high NA to sigma = 
                *                                   {.42 to .5}*AbbeLimit
                */
                double width = 2 * s;  

                //get Relative Defocus Term
                double relativeDefocus = paramsOut[GaussianWithDefocusFit.IDX_ADEFOCUS];
                print(" The Relative Defocus is: " + relativeDefocus + " at sigma of " + s );
                spotData.addKeyValue( REL_DEFOCUS_SPOT_KEY, relativeDefocus );
                
                spotData.setData(N, bgr, xMax, yMax, 0.0, width, a, theta, error);
                 
                if ((!useWidthFilter_ || (width > widthMin_ && width < widthMax_))
                        && (!useNrPhotonsFilter_ || (N > nrPhotonsMin_ && N < nrPhotonsMax_))) {
                    return spotData;
                }

            }
        } catch (Exception ex) {
            ReportingUtils.logError(ex);
            ReportingUtils.logError("Thread run out of memory  "
                    + Thread.currentThread().getName());
            ReportingUtils.showError("Fitter out of memory.\n"
                    + "Out of memory error");
            //Immediate Exit
            throw new IllegalThreadStateException(ex.getMessage());
        }

        //No Value Condition
        return null;
    }
}
