/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.views;

import edu.hope.superresolution.models.FiducialArea;
import edu.hope.superresolution.MMgaussianfitmods.datasubs.BoundedSpotData;
import edu.hope.superresolution.models.ModelUpdateListener;
import edu.hope.superresolution.models.FiducialLocationModel;
import edu.hope.superresolution.models.ModelUpdateDispatcher;
import edu.valelab.gaussianfit.data.SpotData;
import edu.valelab.gaussianfit.utils.ReportingUtils;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.Roi;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Microscope
 */
public class ImageViewController implements ModelUpdateListener {
    
    private ImageWindow imgWin_;  //Window For the Intended selection interaction
    private FiducialLocationModel fLocationModel_;  //location Model to be displayed
    private boolean showFiducials_ = true;  //Whether or not to show fiducials
    private Overlay ov_ = new Overlay();  //reusable Overlay for fiducial display
    private Roi recentRoi_;   //Roi most recently acquired for use with mouse events
    
    private final List<BoundedSpotData> fiducialSpotData_ = new ArrayList<BoundedSpotData>(); //Used for local storage of spots
    
    private boolean showAll_ = false;  //Whether or not to show all the fiducial Areas in an overlay
    private FiducialLocationModel.BoxDisplayTypes boxDisplayMode_ = 
                                        FiducialLocationModel.BoxDisplayTypes.select;
    
    public ImageViewController( ImageWindow imgWin/*, FiducialLocationModel fLocationModel*/ ) {
        
        imgWin_ = imgWin;
        imgWin_.setVisible(true);
        
        //fLocationModel_ = fLocationModel;
        recentRoi_ = imgWin_.getImagePlus().getRoi();
        imgWin_.getCanvas().addMouseListener( new InteractionHandler() );
        imgWin_.getImagePlus().setOverlay( ov_ );
    }
    
    public void dispose() {
        imgWin_.dispose();
    } 
    
    //Set the current FiducialLocationModel for this View
    /*public void setFiducialLocationModel( FiducialLocationModel fLocationModel ) {
        fLocationModel_ = fLocationModel;
    }*/
    
    private void updateOverlay() {
 
        //Clear the overlay and Roi
        imgWin_.getImagePlus().deleteRoi();
        ov_.clear();
        
        if( showAll_ ) {
            for( FiducialArea fArea : fLocationModel_.getFiducialAreaList() ) {
                updateOverlayForFiducialArea( fArea );
            }
        }
        else{
            updateOverlayForFiducialArea( fLocationModel_.getSelectedFiducialArea() );
        }
        
        if( showFiducials_) {
            imgWin_.getImagePlus().setOverlay(ov_);
            imgWin_.getImagePlus().setHideOverlay(false);
        }
        else {
            imgWin_.getImagePlus().setOverlay(ov_);
            imgWin_.getImagePlus().setHideOverlay(true);
        }
        
        //add the ROI For the Current Selectable One
        FiducialArea selected = fLocationModel_.getSelectedFiducialArea();
        //Necessary filtering for the sake of an overzelous deletion operation or lazy initialization
        if( selected != null ){
            imgWin_.getImagePlus().setRoi( selected.getSelectionArea() );
        }
    }
    
    void updateOverlayForFiducialArea( FiducialArea fArea ) {
        fiducialSpotData_.clear();
        if( fArea == null || fArea.getSelectionArea() == null )
        {
            //Must have been a Removal Process
            return;
        }
        fiducialSpotData_.addAll( fArea.getAllPossibleSpotsCopy() );
        //Add Each possible Spot to the area
        for( BoundedSpotData spot : fiducialSpotData_ ){
            ov_.add( spot.getBoundingBox() );
            if( spot.isVirtual() ) {
                spot.getBoundingBox().setStrokeColor(Color.BLUE);
            }
            if( showAll_ ) {
                //Add Bounding Boxes Based on Box Display Mode
                if( boxDisplayMode_ == FiducialLocationModel.BoxDisplayTypes.select ) {
                    ov_.add( fArea.getSelectionArea() );
                } else if ( boxDisplayMode_ == FiducialLocationModel.BoxDisplayTypes.track ) {
                    ov_.add( fArea.getTrackSearchArea() );
                }
                
            }
        }
        /*BoundedSpotData spot = fArea.getRawSelectedSpot();
        if (showAll_) {
            if (spot.isVirtual()) {
                spot.getBoundingBox().setStrokeColor(Color.cyan);
            } else {
                spot.getBoundingBox().setStrokeColor(Color.red);
            }
            ov_.add( fArea.getTrackSearchArea() );
        }
        ov_.add( spot.getBoundingBox() );*/
    }

    @Override
    public void guiEnable(boolean enable) {
        imgWin_.setVisible( enable );
    }

    /**
     * Callback to store the new model reference if the model reference is null, 
     * (never initialized or altered through onUnregisteredToModel).
     * 
     * @param registeredModel 
     */
    @Override
    public void onRegisteredToModel(ModelUpdateDispatcher registeredModel) {
        if( fLocationModel_ == null && registeredModel instanceof FiducialLocationModel ) {
            fLocationModel_ = (FiducialLocationModel) registeredModel;
        }
        
    }

    /**
     * Callback to set the current Model reference to null when unregistered in preparation
     * for other location models.
     * <p>
     * TODO:  expand this to deal with current imageWindows and redrawing as well.
     * 
     * @param unregisteredModel 
     */
    @Override
    public void onUnregisteredToModel(ModelUpdateDispatcher unregisteredModel) {
        if( fLocationModel_ != null && unregisteredModel == fLocationModel_ ) {
            fLocationModel_ = null;
        }
    }
    
    private class InteractionHandler extends MouseAdapter {

        private int pressCoordsX_;
        private int pressCoordsY_;

        /**
         * Callback Event For Handling mouseClick Releases
         * Currently Handles logging of initial click location
         * Also Checks to see if an Roi was changed and acts on any changes 
         */
        @Override
        public void mousePressed(MouseEvent e) {
            //Store Coordinates for initial click (used to see if a movement/occured or is just a click)
            pressCoordsX_ = e.getX();
            pressCoordsY_ = e.getY();
            if( showFiducials_ )
            {
                if( e.getButton() == MouseEvent.BUTTON1 ) {
                    // set the Fiducial By passing it back and changing it
                    //fLocationModel_.getSelectedFiducialArea()
                }
            }
            
            //Check for any changes in the Roi that would have happened
            actOnRoiChanges( );
        }

        /**
         * Callback Event For Handling mouseClick Releases
         * Currently Evaluates if the release moved to determine if a selection click occurred
         * Also Checks to see if an Roi was changed and acts on any changes 
         * 
         * @param e 
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            
            
            //To Avoid Selecting in Overlapping Areas, Only Allow when not showing all
            if( showFiducials_ && !showAll_ )
            {
              if( e.getButton() == MouseEvent.BUTTON1 ) {
                 //Check to make sure the mouse didn't move so it's a selection click
                 if (e.getX() == pressCoordsX_ && e.getY() == pressCoordsY_) {
                        int xScreen = imgWin_.getCanvas().offScreenX(pressCoordsX_);
                        int yScreen = imgWin_.getCanvas().offScreenY(pressCoordsY_);
                        //List<FiducialArea> fAreas = fLocationModel_.getFiducialAreaList();
                        FiducialArea fArea = fLocationModel_.getSelectedFiducialArea();
                        //for( int i = 0; i < fAreas.size(); ++i ) {
                        //fArea = fAreas.get(i);
                        if( fArea != null ) {
                            FindAndSelectFiducialAreaSpot( fArea, xScreen, yScreen );
                        }
                    }
                }
            }
            
            //Check for any changes in the Roi that would have happened
            actOnRoiChanges();
        }
      
    }
   
    /**
     * Given x and y coordinates, search the spots of a fiducial and set the first match
     * for a coordinate within the spot's BoundingBox as the selectedSpot.
     * 
     * @param fArea - The Fiducial Area To Search in
     * @param xCoord - The Coordinates (for the Image) of the X search spot
     * @param yCoord - The Coordinates (for the Image) of the Y search spot
     * @return <code>true</code> if a spot was found and set <code>false</code>if not
     */
    private boolean FindAndSelectFiducialAreaSpot( FiducialArea fArea, int xCoord, int yCoord ) {
        
        Roi fRoi, fSpot;

        if (fArea != null) {
            fRoi = fArea.getSelectionArea();
            if (xCoord - fRoi.getXBase() > 0 && xCoord - fRoi.getXBase() < fRoi.getFloatWidth()
                    && yCoord - fRoi.getYBase() > 0 && yCoord - fRoi.getYBase() < fRoi.getFloatHeight()) {
                List<BoundedSpotData> spotList = fArea.getAllRawPossibleSpots();
                for (int a = 0; a < spotList.size(); ++a) {
                    BoundedSpotData spot = spotList.get(a);
                    fSpot = spot.getBoundingBox();
                    if (xCoord - fSpot.getXBase() > 0 && xCoord - fSpot.getXBase() < fSpot.getFloatWidth()
                            && yCoord - fSpot.getYBase() > 0 && yCoord - fSpot.getYBase() < fSpot.getFloatHeight()) {
                        try {
                            fArea.setSelectedSpotRaw(spot);
                            return true;
                        } catch (Exception ex) {
                            //Guaranteed to to be from same set unless thread accessed
                            ReportingUtils.logError(ex);
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    // @param - isPressEvent, allows for filtering
    private void actOnRoiChanges( ) {
        
        Roi roi = imgWin_.getImagePlus().getRoi();
        
        if( !roiIsSameAsRecent( roi ) ) {
            fLocationModel_.setCurrentFiducialAreaRegion( roi );
            if( roi != null ) {
                //Decouple roi from other imagePluses
                recentRoi_ = (Roi) roi.clone();
            } else {
                recentRoi_ = roi;
            }
        }
        
    }
    
    //Helper function for comparing Roi components since 
    private boolean roiIsSameAsRecent( Roi roi ) {
        //Shouldn't take null, but in case this happens from an unanticipated pass
        if( recentRoi_ == null || roi == null ) {
            //Somehow they both were null
            return roi == recentRoi_;
        }
        
        if( roi.getXBase() != recentRoi_.getXBase() || roi.getYBase() != recentRoi_.getYBase() 
                || roi.getFloatWidth() != recentRoi_.getFloatWidth() || roi.getFloatHeight() != recentRoi_.getFloatHeight() ) {
            return false;
        }
        
        return true;
    }
    
    //Non-MVC workaround to assure that controller can get current ROI
    //  if other plugins change them by chance
    private void setModelCurrentRoi( Roi roi ) {
        //Due to the programmatic nature of changing ROIS, this one queries the ImagePlus
        fLocationModel_.setCurrentFiducialAreaRegion( roi );
    }
    
    private void setModelCurrentRoi() {
        setModelCurrentRoi( imgWin_.getImagePlus().getRoi() );
    }
    
    public void setShowOverlay( boolean show ) {
        showFiducials_ = show;
    }
    
    private void setShowAll( boolean enable ) {
        showAll_ = enable;
        updateOverlay();
    }
    
    private void setBoxDisplayMode() {
        boxDisplayMode_ = fLocationModel_.getDisplayBoxMode();
        updateOverlay();
    }
    
    @Override
    public void update(ModelUpdateDispatcher caller, int event) {
        if (caller instanceof FiducialLocationModel) {
            switch (event) {
                case FiducialLocationModel.EVENT_ELEMENT_SET:  //An Element was set
                    //updateOverlay();
                    break;
                case FiducialLocationModel.EVENT_ELEMENT_SELECTED:  //An New Element was selected
                case FiducialLocationModel.EVENT_ELEMENT_REMOVED:
                case FiducialLocationModel.EVENT_FIDUCIAL_AREA_DATA_CHANGED:
                    updateOverlay();
                    break;
                case FiducialLocationModel.EVENT_STORE_ROI_REQUEST:
                    setModelCurrentRoi();
                    break;
                case FiducialLocationModel.EVENT_SHOW_ALL:
                    setShowAll(true);
                    break;
                case FiducialLocationModel.EVENT_SHOW_CURRENT:
                    setShowAll(false);
                    break;
                case FiducialLocationModel.EVENT_FIDUCIAL_BOX_DISPLAY_CHANGE:
                    setBoxDisplayMode();
                    break;

            }
        }
    }

}
