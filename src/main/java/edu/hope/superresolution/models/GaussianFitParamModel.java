/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.processors.FiducialAreaProcessor;
import edu.hope.superresolution.processors.GaussianFitProcessor;
import edu.hope.superresolution.views.MicroscopeModelForm;

/**
 *
 *  Class That Provides a Level of Separation Between GaussianFitThread and
 *   User-Changeable Parameters...  This Actually Makes no sense currently,
 *   But For Other Implementations, may be useful
 * 
 * @author Justin Hanselman
 */
public class GaussianFitParamModel extends ModelUpdateDispatcher implements ProcessorParamModel {

    public static final int SETTINGS_UPDATE = 1;
    
    //Take all Parameters in here
   
    private GaussianFitProcessor gaussFitProc_;
    //TEMPORARY: Fixed Microscope Model
    private MicroscopeModel microscopeModel_ ;
    private MicroscopeModelForm microModelForm_;
    
    //Form for Parameters
    private ExtendedGaussianInfo currentSettings_ = new ExtendedGaussianInfo(); //Creates null object to be populated
    
    /**
     *
     */
    public GaussianFitParamModel() { 
        //create Form and nullary Microscope Model(). 
        microModelForm_ = new MicroscopeModelForm();
        microscopeModel_ = new MicroscopeModel();
        //Register it to model, and model is updated to preferences
        //Not very apparent, but not rewriting the architecture
        microscopeModel_.registerModelListener(microModelForm_);
    }
    
    @Override
    public ExtendedGaussianInfo getCurrentProcessorModelSettings() {
        return currentSettings_;
    }
    
    @Override
    public FiducialAreaProcessor getCurrentProcessor() {
        return gaussFitProc_;
    }

    //Since this is the referring object, we will copy it
    @Override
    public void updateProcessorModelSettings( ExtendedGaussianInfo settings ) {
        currentSettings_ = new ExtendedGaussianInfo( settings );
        gaussFitProc_ = new GaussianFitProcessor( currentSettings_, microscopeModel_ );
        dispatch(SETTINGS_UPDATE );  //Alert any listeners that a refresh may be in order
    }

    @Override
    public void updateProcessorMicroscopeModel(MicroscopeModel microscopeModel) {
        microscopeModel_ = new MicroscopeModel( microscopeModel );
        gaussFitProc_ = new GaussianFitProcessor( currentSettings_, microscopeModel_ );
        dispatch(SETTINGS_UPDATE );
    }
    
    @Override
    public MicroscopeModel getCurrentMicroscopeModel() {
        return microscopeModel_;
    }

}
