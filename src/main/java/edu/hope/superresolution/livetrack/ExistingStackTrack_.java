/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.livetrack;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import edu.hope.superresolution.Utils.IJMMReportingUtils;
import edu.hope.superresolution.exceptions.NoTrackException;
import edu.hope.superresolution.genericstructures.AbstractDriftModel;
import edu.hope.superresolution.genericstructures.iDriftModel;
import edu.hope.superresolution.models.FiducialLocationModel;
import edu.hope.superresolution.models.LinearDriftModel2D;
import edu.hope.superresolution.models.LocationAcquisitionModel;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.io.FileInfo;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;


/**
 *
 * @author Microscope
 */
public class ExistingStackTrack_ implements PlugInFilter {

    private ImagePlus imp_;
    private LocationAcquisitionModel locAcq_;
    
    private final Map<Integer, Double> driftMap_ = new HashMap<Integer, Double>();
    
    /**
     * Only permits *.csv files
     */
    public static class CSVFilter extends FileFilter {

        public CSVFilter () {
            
        }
        
        @Override
        public boolean accept(File pathname) {
            
            if( pathname.isDirectory() ) {
                return true;
            }
            
            String name = pathname.getName();
            if( name == null ) {
                return false;
            }
            
            return name.endsWith(".csv");
        }

        @Override
        public String getDescription() {
            return "Only CSV Files";
        }
    
    }
    
    @Override
    public int setup(String arg, ImagePlus imp) {
       
       if( imp == null ) {
            return STACK_REQUIRED;
       }
        
       imp_ = imp;
       //Add This imp_ and its ImageWindow to LocationAcquisition
       ImageWindow impWindow;
       if( imp_.isVisible() ) {
            impWindow = imp_.getWindow();
       } else {
           //Somehow the ImageWindow was removed in the middle of this
           //This should attach itself to imp_, weirdly non-transparent
           impWindow = new ImageWindow(imp_);
       }
       
        //Create An Action Listener for Clicking on the track Button
        //This One Assumes a stackImagePlus
        LocationAcquisitionModel.AcquisitionSubmitAction trackAction = new LocationAcquisitionModel.AcquisitionSubmitAction() {
            
            //We currently assume that this is a set of all tracked fiducials
            @Override
            public void submitResponse( ) {
                //For the sake of simplicity, we assume we track all slices
                //Workaround to determine stackDepth
                int stackSize = imp_.getImageStackSize();
                //Generate CSV Writer
                String fullPath = generateFullPathForDrift(imp_);
                
                //TODO:  Finalize
                List<AbstractDriftModel> trackList = new LinkedList<AbstractDriftModel>();
                
                //Set the locationAcquisition track mode
                locAcq_.setTrackComparisonMode(LocationAcquisitionModel.TrackComparisonModes.TrackFromFirst);
                for( int i = 1; i <= stackSize; i++ ) {
                    imp_.setSlice(i);
                    try {
                        //This will currently only register tracked fiducials
                        FiducialLocationModel fModel = locAcq_.pushNextFiducialLocationModel(imp_.getProcessor(), true);
                        trackList.add( fModel.getDriftAbsoluteFromFirstModel() );
                    } catch(NoTrackException ex) {
                        IJMMReportingUtils.showError("Could Not Find Track a Fiducial!");
                        
                    }
                    
                }


                
                
                //Spawn a file Chooser to save it
                File saveFile;
                JFileChooser fc = new JFileChooser(imp_.getOriginalFileInfo().directory);
                fc.setAcceptAllFileFilterUsed(false);
                fc.addChoosableFileFilter(new CSVFilter() );
                int result = fc.showSaveDialog(fc);
                if ( result == JFileChooser.CANCEL_OPTION || result == JFileChooser.ERROR_OPTION ) {
                   //Prompt for Thread
                   return;
                } else {
                   saveFile = fc.getSelectedFile();
                }
                
               if( saveFile == null || saveFile.getName() == null ) 
                {
                    return;
                }
                
               //Just add .csv if its missing
               if( !saveFile.getName().endsWith(".csv") ) {
                   saveFile = new File( saveFile.getAbsolutePath() + ".csv" );
                           
               }
               
                FileWriter writer;
                try {
                    writer = new FileWriter(saveFile);
                    //WRITE ALL DATA TO CSV  
                } catch (IOException ex) {
                    //Should Prompt for new save as well (TODO)
                    IJMMReportingUtils.showError(ex);
                    return;
                }
                
                StatefulBeanToCsv<AbstractDriftModel> csvwriter = new StatefulBeanToCsvBuilder<AbstractDriftModel>(writer).build();
                
                //TestBean test = new TestBean();
                try {
                    //This is a post-process operation, since we really want to verify any changes in the future
                    csvwriter.write(trackList);
                    writer.close();
                } catch (CsvDataTypeMismatchException ex) {
                    Logger.getLogger(ExistingStackTrack_.class.getName()).log(Level.SEVERE, null, ex);
                } catch (CsvRequiredFieldEmptyException ex) {
                    Logger.getLogger(ExistingStackTrack_.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(ExistingStackTrack_.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                
                
                
            } 
        };
       
        //Need to change for plugin Instance changes
       locAcq_ = new LocationAcquisitionModel( impWindow, trackAction, null );
       //This is okay because we've separated Fiducial Form and response
       locAcq_.enableSelectedLocationModelGUIs(true);
       
       return DOES_8G+DOES_16+STACK_REQUIRED+PlugInFilter.DOES_STACKS;
        
    }
    
    @Override
    public void run(ImageProcessor ip) {
        //This unfortunately is immediately and asynchronously run on every image in a stack
        //This may be statically interrupted if we'd like to do it?
    }

    private String generateFullPathForDrift( ImagePlus ip ) {
        FileInfo fi = ip.getOriginalFileInfo();
        return fi.directory + "\\" + fi.fileName + "_AbsoluteDriftInfo.csv";
    }
    
    //To Be relocated to a file
    private void storeAbsoluteDriftFile( ) {
        ImagePlus ip = new ImagePlus();


    }
    
}
