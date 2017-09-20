/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.ImageJmodifieds;

import com.google.common.eventbus.Subscribe;



/**
 *  Interface for testing Listener for Event Bus in PropertyNotifierImagePlus.  
 *  Should provide returned events or an evaluation of the events when implemented in Testing
 * 
 * @author Desig
 */
public interface IPropertyNotifierImagePlusEventListener {
    
    @Subscribe
    public boolean onImageProcessorChangeEvent( PropertyNotifyingImagePlus.ImageProcessorChangeEvent evt );
    
    @Subscribe
    public boolean onCalibrationChangedEvent( PropertyNotifyingImagePlus.CalibrationChangedEvent evt );
    
    @Subscribe
    public boolean onFileInfoChangeEvent( PropertyNotifyingImagePlus.FileInfoChangedEvent evt );
    
    @Subscribe
    public boolean onImageChangedEvent( PropertyNotifyingImagePlus.ImageChangedEvent evt );
    
    @Subscribe
    public boolean onImageDimensionsChangedEvent( PropertyNotifyingImagePlus.ImageDimensionsChangedEvent evt );

    @Subscribe
    public boolean onImageTypeChangedEvent( PropertyNotifyingImagePlus.ImageTypeChangedEvent evt );

    @Subscribe
    public boolean onOverlayChangedEvent( PropertyNotifyingImagePlus.OverlayChangedEvent evt );

    @Subscribe
    public boolean onOverlayVisibilityChangedEvent( PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt );

    @Subscribe
    public boolean onRoiChangeEvent( PropertyNotifyingImagePlus.RoiChangeEvent evt );    

    @Subscribe
    public boolean onStackDimensionsChangedEvent( PropertyNotifyingImagePlus.StackDimensionsChangedEvent evt );   
    
    @Subscribe
    public boolean onStackPositionChangedEvent( PropertyNotifyingImagePlus.StackPositionChangedEvent evt );   

    @Subscribe
    public boolean onTitleChangedEvent( PropertyNotifyingImagePlus.TitleChangedEvent evt );   
    
    @Subscribe
    public boolean onDummyEvent( PropertyNotifyingImagePlus.DummyEvent evt );
}
