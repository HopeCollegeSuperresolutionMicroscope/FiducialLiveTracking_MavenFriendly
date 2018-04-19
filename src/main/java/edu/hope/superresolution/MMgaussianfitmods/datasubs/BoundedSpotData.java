/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.MMgaussianfitmods.datasubs;

import edu.valelab.gaussianfit.data.SpotData;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;

/**
 *  Class that calculates and stores a bounded Roi that will encompass a given spot
 *   This is used for visual and two-dimensional search premises
 * 
 * @author Justin Hanselman
 */
public class BoundedSpotData extends SpotData {
    
    private Roi boundingBox_;
    private int halfWidth_;  //For the sake of calling super functions
    private final double pixelSizenm_;  //Pixel size in nm for conversion
    private double xPixelLoc_;  //Pixel Location of spot Center
    private double yPixelLoc_;  //PixelLocation of spot Center
    private boolean virtual_;  //Whether or not This Spot is Virtual (as opposed to detected)
    
    //Additional Uncertainties and data
    private double numPhotonUncertainty_ = 0;  //The Number of Photons Across the Spot (defined by this)
    private double maxIntensity_ = 0; //The Max Intensity of the PSF
    private double maxIntensityUncertainty_ = 0; // Uncertainty in the Max Intensity
    
    public BoundedSpotData(ImageProcessor ip, int channel, int slice, int frame, 
           int position, int nr, int x, int y, double pixelSizenm)
    {
        this( ip, channel, slice, frame, position, nr, x, y, pixelSizenm, false);  //non-virtual        
    }
    
    //Full Copy Constructor
    public BoundedSpotData( BoundedSpotData source ) {
        this( source, 0, 0 );  //Just a Shift Modification of (0, 0)
    }
    
    //Shifted Modified Copy Constructor
    // Should Include An Intensity Change, due to loss of light in defocus, but for a later time
    public BoundedSpotData( BoundedSpotData source, double xPixelShift, double yPixelShift ) {
        this( source, xPixelShift, yPixelShift, false );  //non-virtual
    }
    
    // Super Copy Constructor for SpotData Implementation
    public BoundedSpotData( SpotData spot, double pixelSizenm ) {
        this( spot, pixelSizenm, false ); //non-virtual
    }
    
    //Virtual Modification Constructors
    // Accessible only thorugh createVirtualSpot()
    // Argument Based Construction    
    private BoundedSpotData( ImageProcessor ip, int channel, int slice, int frame, 
           int position, int nr, int x, int y, double pixelSizenm, boolean virtual ) {
        super( ip, channel, slice, frame, position, nr, x, y);
        virtual_ = virtual;
        pixelSizenm_ = pixelSizenm;
        xPixelLoc_ = getXCenter()/pixelSizenm_;
        yPixelLoc_ = getYCenter()/pixelSizenm_;
        setBoundingBox();
    }
    
    //Shift Modifiable Copy Constructor - virtualizable
    private BoundedSpotData( BoundedSpotData source, double xShift, double yShift, boolean virtual ) {
        super( source );
        virtual_ = virtual;
        pixelSizenm_ = source.pixelSizenm_;
        //Change the Centers in SpotData, even though already set
        setXCenter( getXCenter() + xShift );
        setYCenter( getYCenter() + yShift );
        xPixelLoc_ = getXCenter()/pixelSizenm_;
        yPixelLoc_ = getYCenter()/pixelSizenm_;
        
        numPhotonUncertainty_ = source.numPhotonUncertainty_;
        maxIntensity_ = source.maxIntensity_;
        maxIntensityUncertainty_ = source.maxIntensityUncertainty_;

        setBoundingBox();
    }
    
    //Super Copy Constructor
    private BoundedSpotData( SpotData spot, double pixelSizenm, boolean virtual ) {
        super( spot );
        virtual_ = virtual;
        pixelSizenm_ = pixelSizenm;
        xPixelLoc_ = getXCenter()/pixelSizenm_;
        yPixelLoc_ = getYCenter()/pixelSizenm_;
        setBoundingBox();
    }
    
    /**  Create A Virtual Spot
     *  <p>
    *   Only way to access to private Virtualization constructors.  This returns 
    *   BoundedSpotData with virtual_ flag set to true (non-countable in tracks)
    *   @param copySource - The BoundedSpot whose characteristics will be modified (previous Spot in a track)
    *   @param xShift - Amount of nm in X that the VirtualSpot is shifted from copySource
    *   @param yShift - Amount of nm in Y that the VirtualSpot is shifted from copySource
    *   @param intRatio - The Ratio of (expected virtual intensity)/(copySource intensity)
    *   @param uncertainRatio - The Ratio of (expected virtual sigma)/(copySource sigma)
    *   @return A new BoundedSpotData with virtual_ flag set to true
    */
    public static BoundedSpotData createVirtualSpot( BoundedSpotData copySource, double xShift, 
                                                        double yShift, double intRatio, double uncertainRatio ) {
        BoundedSpotData temp = new BoundedSpotData(copySource, xShift, yShift, true );
        //Inefficient Use of Legacy Function to set Intensity and Uncertainty...  
        temp.setData( temp.getIntensity()*intRatio, temp.getBackground(), 
                                  temp.getXCenter(), temp.getYCenter(), temp.getZCenter(),
                                  temp.getWidth(), temp.getA(), temp.getTheta(), temp.getSigma() * uncertainRatio);
        return temp;
    }
    
    /**
    *   Get the X Spot Location (Image Pixel Coordinates)
    * 
    *   @return The Pixel location X value (fractional number of pixels)
    */
    public double getXPixelLocation() {
        return xPixelLoc_;
    }
    
    /**
    *   Get the Y Spot Location (Image Pixel Coordinates)
    * 
    *   @return The Pixel location Y value (fractional number of pixels)
    */
    public double getYPixelLocation() {
        return yPixelLoc_;
    }
    
    /**
    *   Get the Size of a Pixel for the Spot (assumed square)
    * 
    *   @return size in nanometers of a pixel (assumed square)
    */
    public double getPixelSizenm() {
        return pixelSizenm_;
    }
    
    /**
    *   Returns whether or not this spot represents a physical detection 
    *     or a virtual (placeholding) representation of an anticipated spot
    *   <p>
    *   Note: This only returns true, if createVirtualSpot() is used to create a BoundedSpotData
    * 
    *   @return <code>true</code> if spot is a placeholder representation or <code>false</code>
    *           if the spot was the result of a gaussian fit discovery (really detected)
    *   
    *   @see #createVirtualSpot(edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData, double, double, double, double) 
    */
    public boolean isVirtual() {
        return virtual_;
    }
    
    /**
    *   Sets the Bounding Box that Encompasses the Spot Gaussian That Was Detected
    *     This Bounding Box can be used for display, or crude Haussian analysis over a small area
    */
    private void setBoundingBox( ) {
        
        double w = (getWidth() + getSigma())/pixelSizenm_; 
        double h = (getWidth() + getSigma())/pixelSizenm_;
        
        double xC = Math.floor( xPixelLoc_ - w/2);
        double yC = Math.floor( yPixelLoc_ - h/2);
        w += 2;  //Account for possibility of 1 pixel on either side
        h += 2;
        
        halfWidth_ = (int) Math.ceil( w/2 );
        boundingBox_ = new Roi( xC, yC, w, h );
        
        
    }
    
    /**
    *   Get the Gaussian Bounding Box for the Spot's Data
    * 
    *   @return An Roi object representative of the bounding box around the gaussian spot 
    *           for the image coordinates
    *   @see Roi
    */
    public Roi getBoundingBox() {
        return boundingBox_;
    }
    
    /**
    *   Override of SetData from SpotData
    *     Note: This is to allow a Legacy Override of Intensity, function inefficiently sets way too many parameters
     * @param intensity  Intensity of the detected or virtual gaussian
     * @param background Background intensity of the image
     * @param xCenter    Center X coordinate in nm (Current Image Coordinate System)
     * @param yCenter    Center Y coordinate in nm (Current Image Coordinate System)
     * @param zCenter    Center Z coordinate in nm (Current Image Coordinate System)
     * @param width      Width (FWHM) of the detected Gaussian in nm
     * @param a          Ratio of two orthogonal Gaussian Widths (longer/shorter)
     * @param theta      Shape factor for spot (rotation of assymetric peak)
     * @param sigma      Estimate of error in localization based on Web et al. formula
     *                   that uses # of photons, background and width of gaussian
     * 
     * @see SpotData
    */
    @Override
    public void setData(double intensity, 
           double background, 
           double xCenter, 
           double yCenter, 
           double zCenter,
           double width, 
           double a, 
           double theta, 
           double sigma) {
        
        super.setData( intensity, background, xCenter, yCenter, zCenter, width, a, theta, sigma );
        xPixelLoc_ = getXCenter()/pixelSizenm_;
        yPixelLoc_ = getYCenter()/pixelSizenm_;
        setBoundingBox();
        
    }
    
    @Override
    public ImageProcessor getSpotProcessor(ImageProcessor siProc, int halfSize) {
      if (getImageProcessor() != null)
         return getImageProcessor();
      synchronized(lockIP) {
         Roi spotRoi = new Roi(getX() - halfSize, getY() - halfSize, 2 * halfSize, 2 * halfSize );
         //siProc.setSliceWithoutUpdate(frame_);
         siProc.setRoi(spotRoi);
         return siProc.crop();
      }
   }
   
    //Additional SpotProcessor Methods With Automatic BoundingBox Parameters
    public ImageProcessor getSpotProcessor(ImagePlus siPlus ) {
        ImageProcessor ip = getSpotProcessor( siPlus, halfWidth_ );
        //Use the bounding Box_ to limit any sizes
        ip.setRoi( boundingBox_ );
        return ip.crop();        
    }

   public ImageProcessor getSpotProcessor(ImageProcessor siProc) {
        ImageProcessor ip = getSpotProcessor( siProc, halfWidth_ );
        //Use the bounding Box_ to limit any sizes
        ip.setRoi( boundingBox_ );
        return ip.crop();
   }
   
   public static ImageProcessor getSpotProcessor(ImageProcessor siProc, int halfSize, int x, int y) {
      synchronized(lockIP) {
         Roi spotRoi = new Roi(x - halfSize, y - halfSize, 2 * halfSize, 2 * halfSize);
         siProc.setRoi(spotRoi);
         try {
            return siProc.crop();
         } catch (java.lang.ArrayIndexOutOfBoundsException ex) {
            return null;
         }
      }
   }
  
   public void setNumPhotonUncertainty( double numPhotonUncertainty ) {
       numPhotonUncertainty_ = numPhotonUncertainty;
    }  
   
   public double getNumPhotonUncertainty( ) {
       return numPhotonUncertainty_;
   }
   
   public void setMaxIntensity( double maxIntensity ) {
       maxIntensity_ = maxIntensity;
   }
   
   public double getMaxIntensity( ) {
       return maxIntensity_;
   }
   
   public void setMaxIntensityUncertainty( double maxIntensityUncertainty ) {
       maxIntensityUncertainty_ = maxIntensityUncertainty;
   }

   public double getMaxIntensityUncertainty( ) {
       return maxIntensityUncertainty_;
   }
   
}
