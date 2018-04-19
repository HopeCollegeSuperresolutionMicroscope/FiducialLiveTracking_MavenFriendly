/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.processors.iFiducialAreaProcessor;

/**
 *  Interface for Implementing a Parameter Model for a Given Processor
 *    Example: See GaussianFit ParamModel and GaussianFitProcessor
 * 
 * @author Justin Hanselman
 */
public interface ProcessorParamModel {
    
    //Used to set the Current Processor with any stored Model Parameters
    //public void setCurrentProcessor( iFiducialAreaProcessor fAreaProc );
    
    //Used to get The Current Processor
    public iFiducialAreaProcessor getCurrentProcessor();
    
    //Takes an Extended GaussianInfo object as the currentParameters
    public void updateProcessorModelSettings( ExtendedGaussianInfo settings );
    
    //Takes a microscopeModel with which to update the gaussianFitModel
    public void updateProcessorMicroscopeModel( MicroscopeModel microscopeModel );
    
    public ExtendedGaussianInfo getCurrentProcessorModelSettings();
    
    public MicroscopeModel getCurrentMicroscopeModel();
    
}
