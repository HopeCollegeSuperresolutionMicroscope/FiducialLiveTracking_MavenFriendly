/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import edu.hope.superresolution.processors.FiducialAreaProcessor;

/**
 *  Super Class For Objects Planning on operating on Fiducial Collections
 *   Sets a standard Fit and Track Object for the entire collection
 * 
 * @author Microscope
 */
public interface FiducialCollection {
    
    public FiducialAreaProcessor getFiducialAreaProcessor( );
    
    //Consideration of Copying the Processor should be made for any reevalutation of single images
    public void setFiducialAreaProcessor( FiducialAreaProcessor fAreaProcessor );
    
    /*public final void setGaussianTrackObject( GaussianTrackObject gaussTrack ) {
        gaussianTrack_ = gaussTrack;
    }
    
    public final GaussianTrackObject getGaussianTrackObject() {
        return gaussianTrack_;
    }*/
    
}
