/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.Utils;

/**
 *  Utility Class That Implements Various Calculations for uncertainties as outlined
 *   in "Precise Nanometer Localization Analysis for Individual Fluorescent Probes"
 *   By Thompson, Larson, and Webb.  This Class may be added to or Extended to implement
 *   further equations later on.
 * 
 * @see  <a href="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC1302065/pdf/11964263.pdf"> Thompson Paper</a>
 * @author Microscope
 */
public class ThompsonGaussianEstimationUtil {
    
     /**
     *  Function that uses the formula (17) from Thompson et al. (2002) to calculate lateral
     *   uncertainty in a calculated measurement.
     * <p>
     *  Equation is of the form: uncertainty^2 = (sigma^2 + (a^2/12)) / numPhotons + (8*pi*sigma^4*background^2) / (a*numPhotons)^2
     * 
     * @param sigma      - Gaussian Sigma that describes estimated Gaussian Distribution of measured intensity
     * @param background - The Background in number of photons
     * @param numPhotons - The number of Photons in a Measurement
     * @param pixelSize  - The size of a square pixel in 1 Dimension (nm)
     * @return - The Value of the Uncertainty in the Gaussian Sigma.  This is the amount to either side 
     *           that we might shift the Gaussian Center as estimated by the referenced paper.
     * 
     * @see <a href="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC1302065/pdf/11964263.pdf"> Thompson Paper</a>
     */
    public static double calculateLateralUncertainty( double sigma, double background, 
                                                 double numPhotons, double pixelSize) {
        double uncertainty = (sigma * sigma + (pixelSize * pixelSize) / 12) / numPhotons
                       + (8 * Math.PI * sigma * sigma * sigma * background * background) / (pixelSize * pixelSize * numPhotons * numPhotons);
        uncertainty = Math.sqrt(uncertainty);
        return uncertainty;
    }
    
    /**
     *  Function that uses the formula (19) from Thompson et al. (2002) to calculate Number of Photon
     *   uncertainty in a calculated measurement.
     * <p>
     *  Equation is of the form: uncertainty^2 = numPhotons + 4*pi*sigma^2*background^2/(pixelSize^2)
     * 
     * 
     * @param numPhotons - The number of Photons in a Measurement
     * @param sigma      - Gaussian Sigma that describes estimated Gaussian Distribution of measured intensity
     * @param background - The Background in number of photons
     * @param pixelSize  - the size of a square pixel in 1 Dimension (nm)
     * @return The Uncertainty in the number of Photons Counted for the assumed Gaussian distribution
     * 
     * @see <a href="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC1302065/pdf/11964263.pdf"> Thompson Paper</a>
     */
    public static double calculatePhotonUncertainty( double numPhotons, double sigma, 
                                                double background, double pixelSize  ) {
        double uncertainty = numPhotons + 4 * Math.PI*sigma*sigma*background*background/(pixelSize*pixelSize);
        uncertainty = Math.sqrt(uncertainty);
        return uncertainty;
    }
    
}
