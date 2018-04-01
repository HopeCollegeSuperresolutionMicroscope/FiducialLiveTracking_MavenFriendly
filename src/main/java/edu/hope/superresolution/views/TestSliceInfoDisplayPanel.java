/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.views;

import com.google.common.eventbus.Subscribe;
import edu.hope.superresolution.ImageJmodifieds.ImPlusListenerDockablePanel;
import edu.hope.superresolution.ImageJmodifieds.PropertyNotifyingImagePlus;
import java.awt.Button;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Justin Hanselman
 */
public class TestSliceInfoDisplayPanel extends ImPlusListenerDockablePanel {
    
    private PropertyNotifyingImagePlus ImPlusRef_;
    private final Object testLock_;
    
    public TestSliceInfoDisplayPanel( final Object lock ) {
        testLock_ = lock;
        setLayout( new MigLayout( "insets 1, fill", "[40][100]") );
        sliceTitle_ = new Label();
        sliceTitle_.setText("The Current Slice Number is: ");
        sliceNumber_ = new Label();
        sliceNumber_.setText("");
        promptButton_ = new Button( "OK");
        ActionListener endTest = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized( testLock_ ) {
                    testLock_.notify();
                }
            }
            
        };
        promptButton_.addActionListener(endTest);
        add(sliceTitle_);
        add(sliceNumber_);
        add(promptButton_);
    }

    @Override
    public void registerToPropertyNotifyingImagePlus(PropertyNotifyingImagePlus propImPlus) {
        propImPlus.RegisterToEventBus( this );
        ImPlusRef_ = propImPlus;
        
    }

    @Override
    public void unregisterFromPropertyNotifyingImagePlus(PropertyNotifyingImagePlus propImPlus) {
        propImPlus.UnregisterFromEventBus( this );
    }
    
    @Subscribe
    public void onStackPositionChange( PropertyNotifyingImagePlus.StackPositionChangedEvent evt ) {
        if( evt.sameOriginator(ImPlusRef_) ) {
            sliceNumber_.setText( Integer.toString(evt.getCurrentSlice()) );
        }
    }
    
    private Label sliceTitle_;
    private Label sliceNumber_;
    private Button promptButton_;
    
}
