/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import ij.VirtualStack;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class That maintains a Specific Instance Copy of A Directory that can provide
 * references to virtualStacks and Directories.  This Instance will contain a unique 
 * Temporary Directory On Initialization.  Instances set to a real directory will
 * switch all resources over to the new Directory.
 * 
 * @author Microscope
 */
public class VirtualDirectoryManager {
    
    private File dir_;
    private boolean isTempDir_;
    
    public VirtualDirectoryManager() {
        
        try {
            dir_ = createTempDirectory();
        } catch (IOException ex) {
            Logger.getLogger(VirtualDirectoryManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        isTempDir_ = true;
        
    }
    
    /**
     * Creates a directory based off the name for the stack to be appended.  The stackBase Will Be made Unique
     * Via Iteration, if multiple instances are created.
     * 
     * @param stackBaseName - the Base Name For a folder to place All additions under 
     * @return 
     */
    public VirtualStack createNewVirtualStack(int width, int height, ColorModel cm, String stackBaseName ) throws IOException {
        
        String testPath = dir_.getAbsolutePath() + File.separatorChar + stackBaseName;
        File testFile;
        int differentiator = 0;
        do {
            testFile = new File( testPath + "_" + differentiator );
        } while( testFile.exists() );
        
        if (!(testFile.mkdir())) {
            throw new IOException("Could not create temp directory: " + testFile.getAbsolutePath() );
        }
        
        return new VirtualStack( width, height, cm, testFile.getAbsolutePath() );
    }
    
    /**
     * Gets The Directory for saving Acquisition Related Images and data
     * <p>
     * This defaults to A Temporary Directory unless reset
     * @return 
     */
    public File getSaveDir() {
        return dir_;
    }
    
    /**
     * Sets the AcquisitionSaveDir for saving Acquisition Related Images and data
     * for ImageWindows and such. Must be an existing Directory.  Will Copy All Directory 
     * objects over to the Other Directory
     * TODO: Finish behavior for more robust implementation
     *//*
    public void setAcquisitionSaveDir( File newDir ) {
        
        assert( newDir.isDirectory() );
        
        //Delete the Temporary Directory
        if( isTempDir_ && acquisitionSaveDirectory_.exists() ) {
            acquisitionSaveDirectory_.delete();
        }
        
        acquisitionSaveDirectory_ = newDir;
        isTempSaveDir_ = false;

    }*/
    
    
     /**
     * Create Temporary Directory for the Acquisition Storage
     * @return - The File Containing the Path of the Directory
     * @throws IOException 
     */
    private File createTempDirectory()
            throws IOException {
        final File temp;

        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        //Make it deleteItself on Exit from the JVM
        temp.deleteOnExit();
        
        return (temp);
    }
}
