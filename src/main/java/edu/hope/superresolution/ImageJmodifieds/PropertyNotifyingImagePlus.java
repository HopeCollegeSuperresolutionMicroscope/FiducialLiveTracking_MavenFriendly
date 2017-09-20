/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.ImageJmodifieds;

import com.google.common.eventbus.EventBus;
import edu.hope.superresolution.genericstructures.MasterSpecificEvent;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 
 * After Examining the ImagePlus class, this class is an extension that wraps most 
 * of the ImagePlus set methods (that actually set an internal member of concern)
 * with overridden methods, and applies a relevant EventBus Event that is dispatched.
 * <p>
 * This is meant to allow for treatment of an ImagePlus more like a model to other view
 * containers and is meant to facillitate "listening" to the canvas element that is linked
 * to it in an ImageWindow, so that ImageWindows may be expanded upon with docked widgets
 * that are aware of the state of the ImagePlus.
 * <p>
 * A Critical method that must be called if intending to use this class as a Notifier is,
 * RegisterToEventBus().  See below for more details.
 * <p>
 * <pre>
 * IMPORTANT NOTE ABOUT EVENTS.  Due to the convoluted nature of the actual methods
 * in the ImagePlus class, there are a few expectations for Events.
 * 
 * 1.  Since Multiple core values can be changed and rechanged (often to the same value)
 *    in the course of executing a method, Events are only dispatched after the entry-level
 *    method in this class is called.  That means, a queue of Events will be created 
 *    that is not Dispatched until all operations have been completed on the ImagePlus.
 * 2.  This Event Queue behavior (in order to only slightly enforce Thread-behavior) will also
 *     only dispatch after there is no internal method currently being run.  The 
 *     super class is not exactly thread-safe anyway, so please make an effort not to update values
 *     in subscriber methods. Only Query them.
 * 3. Due to the fact that the super class (ImagePlus) is basically a record keeper for
 *    ImageProcessors according to Stacks (whether only 1 or multiple slices), and it keeps
 *    relations to ImageWindows and Canvases, Most Events Dispatched are only concerned 
 *    with the ImagePlus Properties.  
 * 4. The Largest Exception is the ImageProcessorPropertyChanged Event.  If a subscribed 
 *    class wants to pay attention to changes to the ImageProcessor, (which corresponds
 *    most basically to keeping up-to-date on how the current image is displayed), listening
 *    to this event is the most pertinent.  However, due to the fact that the imageProcessor
 *    can change both reference and properties upon events (such as actual resetting of processor,
 *    various conditions when changing a position in a stack, etc.) This event class provides several 
 *    keys that should be checked.  This also produces a redundancy that must be considered
 *    given the queue nature of Event Dispatching (i.e PositionChangedEvent and ImagePropertyChanged.getSliceChanged()).
 *    Many other events indicate a change to the ImagePlus that may also change the ImageProcessor.
 *    For instance, if you set a Slice (there will be a position change update and
 *    an ImageProcessorChangedEvent indicating a position change update.)
 * 5.  For this reason each event is designed to provide the internal data that is expected to be used.
 *     The PositionUpdated Event Provides the reference to the ImageProcessor for
 *     accessing ImageProcessorProperties to avoid GetImageProcessor(), which may reset properties unfortunately.
 *     If one wants to listen for ImageProcessorChanges as well, then there should be the 
 *     decision, such as a shared boolean that only triggers on a PositionChagnedEvent and excludes redundant action,
 *     and that is not toggled at all, when the position is the same but a completely new or changed ImageProcessor is set.
 * 6.  To facillitate this Queue Behavior, Every Dispatch is ended with the QueueSendCompletedEvent.
 *     This will allow a measure of synchronization to subscribers, but if you would like to use
 *     this event to keep track of whether or not Events were the same, make certain
 *     that any asynchronous Threading is accounted for.  The only guarantee is that 
 *     the subscriber methods will be called in order, but there is no guarantee about waiting 
 *     for event processing to finish before dispatching the next.
 * </pre>
 * <p>
 * Note: This may seem complex but this is an attempt to adapt the ImagePlus Class, which 
 *       is being Completely rewritten for ImageJ2.  However, since we need ImageJ1.x
 *       and ImageJ2 is not yet completely available, this class is a nice account for 
 *       the incredibly unreliable source Code and its interaction with Canvas and ImageWindow.
 * 
 * @author Desig
 */
public class PropertyNotifyingImagePlus extends ImagePlus {

    /**
     * Event Classes For Specific Registration
     * Note: These Classes extend the MasterSpecificEvent abstract class in the event
     *       that a subscriber registers to Multiple PropertyNotifyingImagePluses.
     *       If this is the anticipation, use the sameOriginator( ) method with a 
     *       saved reference to the PropertyNotifyingImagePlus so that the specific instance 
     *       may be referenced.  This may also aid in making a static EventBus instance instead.
     */
    
    /**
     * Event Class - ImageProcessor that is stored internally has been changed
     * <p>
     * This Class is indicative that an ImageProcessor has changed.  There are multiple flags
     * that may be checked by the subscriber.  These flags may occur in any order or number,
     * due to the poor nature of how the imageProcessor is handled.  If an ImageProcessor is changed, 
     * Graphical Subscribers should take the reference passed in this event.  DO NOT USE getProcessor()
     * as this method actually can modify the imageProcessor again and trigger another event.
     * <p>
     * Note: There is the potential for an uninitialized Reference to Be null and
     * should be tested.  Subscribers should test accordingly.
     * <p>
     * Causing Methods: setProcessor(ImageProcessor), setProcessor( String, ImageProcessor)
     *                   setImage( Image ), setImage( ImagePlus )
     * 
     * @see ImagePlus#setProcessor(ij.process.ImageProcessor) 
     * @see ImagePlus#setProcessor(java.lang.String, ij.process.ImageProcessor) 
     * @see ImagePlus#setImage(java.awt.Image) 
     */
    public static class ImageProcessorChangeEvent extends MasterSpecificEvent {
        
        /**
         * Tags for Type of Change (correspond to isolated binary 1s)
         */
        public static final int REF_CHANGED = 1;
        public static final int TYPE_CHANGED = 2;
        public static final int DEPTH_CHANGED = 4;
        public static final int LUT_CHANGED = 8;
        public static final int DISPLAYRANGE_CHANGED = 16;
        public static final int STACKPOSTION_CHANGED = 32;
        public static final int CALIBRATION_CHANGED = 64;
        public static final int SNAPSHOT_CHANGED = 128;
        public static final int PIXELS_CHANGED = 256;
        public static final int LINEWIDTH_CHANGED = 512;
        public static final int ROI_CHANGED = 1024;
        public static final int MASK_CHANGED = 2048;
        public static final int XYDIM_CHANGED = 4096;
        
        
        private final ImageProcessor prevIP_;
        private final ImageProcessor curIP_;
        private final int evtFlags_;
        
        /**
         * Constructor 
         * 
         * @param originator originating instance for overlay change
         * @param prevIP - The previous ImageProcessor before the change
         * @param curIP - the current ImageProcessor
         * @param evtFlags - Corresponds to the Binary Flags that are public Static (a 1 means the event is present)
         */
        public ImageProcessorChangeEvent( PropertyNotifyingImagePlus originator, ImageProcessor prevIP, ImageProcessor curIP, int evtFlags  ) {
            super(originator);
            prevIP_ = prevIP;
            curIP_ = curIP;
            evtFlags_ = evtFlags;
        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevIP - The previous ImageProcessor before the change
         * @param curIP - the current ImageProcessor
         * @param evtFlags - Corresponds to the Binary Flags that are public Static (a 1 means the event is present)
         */
        public ImageProcessorChangeEvent( ImageProcessorChangeEvent originatorSource, ImageProcessor prevIP, ImageProcessor curIP, int evtFlags   ) {
            super( originatorSource );  //Copy Constructor for Super
            prevIP_ = prevIP;
            curIP_ = curIP;
            evtFlags_ = evtFlags;
        }
        
        public ImageProcessor getPreviousImageProcessorReference() {
            return prevIP_;
        }
        
        public ImageProcessor getCurrentImageProcessorReference() {
            return curIP_;
        }
        
        /**
         * Gets the Integer whose binary digits correspond to the public static flags of this class
         * @return 
         */
        public int getEventFlags() {
            return evtFlags_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.  Since binary flags only 
         * indicate a change of state, these are compounded.
         * 
         * @param event - the More Recent Event compared to this
         * @return New StackPositionChangedEvent object with prevCZT from this and curCZT from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof ImageProcessorChangeEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            ImageProcessorChangeEvent upCast = (ImageProcessorChangeEvent) event;
            return new ImageProcessorChangeEvent( upCast, prevIP_, upCast.getCurrentImageProcessorReference(),
                                                    evtFlags_ | upCast.getEventFlags() );  //Join Binary Flags
        }
        
    }
    
    //Only ImagePlus/Canvas Property
    /**
     * Event Class - Overlay for the ImagePlus has been changed
     * <p>
     * Causing Methods:  setOverlay(Overlay), setOverlay(Shape, Color, BasicStroke),
     *                   setOverlay(Roi, Color, int, Color)
     * 
     * @see #setOverlay(ij.gui.Overlay) 
     * @see ImagePlus#setOverlay(ij.gui.Overlay) 
     * @see ImagePlus#setOverlay(java.awt.Shape, java.awt.Color, java.awt.BasicStroke) 
     * @see ImagePlus#setOverlay(ij.gui.Roi, java.awt.Color, int, java.awt.Color) 
     * 
     */
    public static class OverlayChangedEvent extends MasterSpecificEvent {
        
        private final Overlay prevOv_;
        private final Overlay curOv_;
        
        /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevOv - The previousOverlay before the change
         * @param curOv - The currentOverlay
         */
        public OverlayChangedEvent( PropertyNotifyingImagePlus originator, Overlay prevOv, Overlay curOv ) {
            super(originator);
            prevOv_ = prevOv;
            curOv_ = curOv;
        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevOv - The previousOverlay before the change
         * @param curOv - The currentOverlay
         */
        public OverlayChangedEvent( OverlayChangedEvent originatorSource, Overlay prevOv, Overlay curOv ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevOv_ = prevOv;
            curOv_ = curOv;
        }
        
        public Overlay getPreviousOverlay() {
            return prevOv_;
        }
        
        public Overlay getCurrentOverlay() {
            return curOv_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New StackPositionChangedEvent object with prevCZT from this and curCZT from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof OverlayChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            OverlayChangedEvent upCast = (OverlayChangedEvent) event;
            return new OverlayChangedEvent( upCast, prevOv_, upCast.getCurrentOverlay() );
        }
        
    }
    
    /**
     * Event Class - The Overlay for the ImagePlus has changed its visibility.  At 
     *   the very least, this means the ImagePlus Window and Canvas has hidden the Overlay.
     * <p>
     *   Due to redundancy, This class may report previous and current states that are the same.
     *   This is Only the case for multiple visibility swaps occuring before this event was dispatched.
     *   This is an unfortunate reality for the underlying code.  A simple check should suffice
     *   if such a discrepancy is of concern.
     * <p>
     * Causing Methods:  setOverlay(Overlay), setOverlay(Shape, Color, BasicStroke),
     *                   setOverlay(Roi, Color, int, Color), setHideOverlay(boolean)
     * 
     * @see #setOverlay(ij.gui.Overlay) 
     * @see ImagePlus#setOverlay(ij.gui.Overlay) 
     * @see ImagePlus#setOverlay(java.awt.Shape, java.awt.Color, java.awt.BasicStroke) 
     * @see ImagePlus#setOverlay(ij.gui.Roi, java.awt.Color, int, java.awt.Color) 
     * 
     */
    public static class OverlayVisibilityChangedEvent extends MasterSpecificEvent {
        
        private final boolean prevIsVisible_;
        private final boolean curIsVisible_;
        private int numSwaps_;  //Used to track concatenation, such as number of switchs to visibility that occured 
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevIsVisible - If the Overlay was Previously Visible
         * @param curIsVisible - If the Overlay is Currently Visible
         */
        public OverlayVisibilityChangedEvent( PropertyNotifyingImagePlus originator, boolean prevIsVisible, boolean curIsVisible ) {
            super(originator);
            numSwaps_ = 1;
            prevIsVisible_ = prevIsVisible;
            curIsVisible_ = curIsVisible;
        }
        
        /**
         * Originator Copy (superclass) Constructor + number of Visibility of Transition Tracker
         * 
         * Takes An event and uses its Originator_, as well as increments its current numSwaps to store as its development.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevIsVisible - If the Overlay was Previously Visible
         * @param curIsVisible - If the Overlay is Currently Visible
         */
        public OverlayVisibilityChangedEvent( OverlayVisibilityChangedEvent originatorSource, boolean prevIsVisible, boolean curIsVisible ) {
            super( originatorSource );  //Copy Constructor for Super
            
            //increment number of swaps
            numSwaps_ = ++originatorSource.numSwaps_;
            
            prevIsVisible_ = prevIsVisible;
            curIsVisible_ = curIsVisible;
            
        }
        
        public boolean wasPreviouslyVisible() {
            return prevIsVisible_;
        }
        
        public boolean isCurrentlyVisible() {
            return curIsVisible_;
        }
        
        /**
         * Since this is meant for Queueing or cyclic calls, This records the number of time
         * that the Overlay Visibility was toggle from one state to another.
         * 
         * @return - The number of times that the visibility was changed.  (even means no state change)
         */
        public int getNumberOfVisibilityChanges() {
            return numSwaps_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.  Additionally, 
         * this context is the one whose number of visible swaps is incremented.
         * 
         * @param event - the More Recent Event compared to this
         * @return New StackPositionChangedEvent object with prevCZT from this and curCZT from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof OverlayVisibilityChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            OverlayVisibilityChangedEvent upCast = (OverlayVisibilityChangedEvent) event;
            return new OverlayVisibilityChangedEvent( upCast, prevIsVisible_, upCast.isCurrentlyVisible() );
        }
        
    }
    
    //THIS IS REALLY AN IMAGEPROCESSOR PROPERTY 
     /**
     * Event Class - LUT for the ImagePlus has been changed
     * <p>
     * Causing Methods:  setLUT(LUT)
     * 
     * @see ImagePlus#setLut(LUT)
     */
    public static class LUTChangedEvent extends MasterSpecificEvent {
        
        private final LUT prevLut_;
        private final LUT curLut_;
        
        /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevLut - The previous LUT before the change
         * @param curLut - The current LUT
         */
        public LUTChangedEvent( PropertyNotifyingImagePlus originator, LUT prevLut, LUT curLut ) {
            super(originator);
            prevLut_ = prevLut;
            curLut_ = curLut;
        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevLut - The previous LUT
         * @param curLut - The current LUT
         */
        public LUTChangedEvent( LUTChangedEvent originatorSource, LUT prevLut, LUT curLut ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevLut_ = prevLut;
            curLut_ = curLut;
            
        }
        
        public LUT getPreviousLUT() {
            return prevLut_;
        }
        
        public LUT getCurrentLUT() {
            return curLut_;
        }

        /**
         * Combine this LUTChangedEvent event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New LUTChangedEvent object with prevCZT from this and curCZT from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof LUTChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            LUTChangedEvent upCast = (LUTChangedEvent) event;
            return new LUTChangedEvent( upCast, prevLut_, upCast.getCurrentLUT() );
        }
        
    }
    
     /**
     * Event Class - Position (Channel, frame, slice) in the stack for the ImagePlus has changed
     * <p>
     * This Event Records the previous (Channel, frame, slice) and the current set one.  Since
     * A position change means the ImageProcessor necessarily changes, the ImageProcessor is
     * also provided (previous and current).  
     * <p>
     * The ImageProcessor is not guaranteed to be non-null.
     * It should be noted that there seems to be ambiguity between frames and slices across
     * implementations, so evaluate accordingly.
     * <p>
     * Causing Methods:  setCurrentSlice(int), setC(int), setZ(ing), setT(int), 
     *                    setSlice(int), setSliceWithoutUpdate(int), setPosition(int,int,int)
     *                    setPositionWithoutUpdate(int, int, int), setPosition(int)
     * 
     * Underlying Methods: updatPosition(int, int, int)
     * 
     * @see ImagePlus#setCurrentSlice(int)
     * @see ImagePlus#setC(int) 
     * @see ImagePlus#setZ(int) 
     * @see ImagePlus#setT(int) 
     * @see ImagePlus#setSlice(int) 
     * @see ImagePlus#setSliceWithoutUpdate(int) 
     * @see ImagePlus#setPosition(int, int, int) 
     * @see ImagePlus#setPositionWithoutUpdate(int, int, int) 
     * @see ImagePlus#setPosition(int) 
     */
    public static class StackPositionChangedEvent extends MasterSpecificEvent {
        
        private final int prevC_;
        private final int prevZ_;
        private final int prevT_;
        private final int curC_;
        private final int curZ_;
        private final int curT_;
        private final ImageProcessor prevIp_;
        private final ImageProcessor curIp_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevCZT - The previous Channel, Slice, Frame before the change
         * @param prevIp - The Previous ImageProcessor at that Position
         * @param curCZT - The current Channel, Slice, Frame
         * @param curIp - the Current ImageProcessor at the new Position
         */
        public StackPositionChangedEvent( PropertyNotifyingImagePlus originator, int[] prevCZT,
                                            ImageProcessor prevIp, int[] curCZT, ImageProcessor curIp ) {
            super(originator);
            //Standard enforcement
            assert (prevCZT.length == curCZT.length && curCZT.length == 3);

            prevC_ = prevCZT[0];
            prevZ_ = prevCZT[1];
            prevT_ = prevCZT[2];
            curC_ = curCZT[0];
            curZ_ = curCZT[1];
            curT_ = curCZT[2];
            prevIp_ = prevIp;
            curIp_ = curIp;
        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevCZT - The previous Channel, Slice, Frame before the change
         * @param prevIp - The Previous ImageProcessor at that Position
         * @param curCZT - The current Channel, Slice, Frame
         * @param curIp - the Current ImageProcessor at the new Position
         */
        public StackPositionChangedEvent( StackPositionChangedEvent originatorSource, int[] prevCZT,
                                                ImageProcessor prevIp, int[] curCZT, ImageProcessor curIp ) {
            super( originatorSource );  //Copy Constructor for Super
            
            //Standard enforcement
            assert( prevCZT.length == curCZT.length && curCZT.length == 3 );
            
            prevC_ = prevCZT[0];
            prevZ_ = prevCZT[1];
            prevT_ = prevCZT[2];
            curC_ = curCZT[0];
            curZ_ = curCZT[1];
            curT_ = curCZT[2];
            prevIp_ = prevIp;
            curIp_ = curIp;
            
        }
        
        public int getPreviousChannel() {
            return prevC_;
        }
        
        public int getCurrentChannel() {
            return curC_;
        }
        
        public int getPreviousSlice() {
            return prevZ_;
        }
        
        /**
         * Only Use this reference to perform getter queries.  Clone or copy before using further.
         * Manipulating this reference will not actually affect the previous ImageProcessor at that Position.
         * 
         * @return the previous ImageProcessorReference to the previous stack.
         */
        public ImageProcessor getPreviousImageProcessor( ) {
            return prevIp_;
        }
        
        public int getCurrentSlice() {
            return curZ_;
        }
        
        public int getPreviousFrame() {
            return prevT_;
        }
        
        public int getCurrentFrame() {
            return curT_;
        }
        
        /**
         * Only Use this reference to perform getter queries.  Clone or copy before storing
         * and any manipulation to the processor afterwards must be submitted via setProcessor() before the ImagePlus
         * registers the change.
         * 
         * @return the current ImageProcessorReference to the previous stack.
         */
        public ImageProcessor getCurrentImageProcessor( ) {
            return curIp_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New StackPositionChangedEvent object with prevCZT from this and curCZT from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof StackPositionChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            

            
            StackPositionChangedEvent upCast = (StackPositionChangedEvent) event;
            int[] prevCZT = new int[]{ prevC_, prevZ_, prevT_ };
            int[] curCZT = new int[]{upCast.getCurrentChannel(), upCast.getCurrentSlice(), upCast.getCurrentFrame() };
            return new StackPositionChangedEvent( upCast, prevCZT, prevIp_, curCZT, upCast.getCurrentImageProcessor() );
        }
        
    }
    
    /**
     * Event Class - Stack Size/Dimensions (channels, frame, slices) has changed
     * <p>
     * Note: This extends the StackPositionChangedEvent, so that those listening for position,
     * may verify if it was the result of a dimension change.
     * <p>
     * This Event Records the previous Stack Dimensions and the current ones as well as the
     * previous and current (Channel, frame, slice).  This change is recorded for simplicity sake.  As position change listeners
     * do not, necessarily have to be aware of dimension changes.
     * <p>
     * Causing Methods:  setDimensions( int, int, int ), getNSlices(), getNFrames(), getNChannels(),
     *                   getDimensions( boolean)
     * 
     * @see ImagePlus#setDimensions(int, int, int)
     */
    public static class StackDimensionsChangedEvent extends StackPositionChangedEvent {
        
        private final int prevNumChannels_;
        private final int prevNumSlices_;
        private final int prevNumFrames_;
        private final int curNumChannels_;
        private final int curNumSlices_;
        private final int curNumFrames_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevDimCZT - the previous dimensions in Channel, Slice, Frame
         * @param prevCZT - The previous Channel, Slice, Frame before the change
         * @param prevIp - The Previous ImageProcessor at that Position
         * @param curDimCZT - the current dimensions in Channel, Slice, Frame
         * @param curCZT - The current Channel, Slice, Frame
         * @param curIp - the Current ImageProcessor at the new Position
         */
        public StackDimensionsChangedEvent( PropertyNotifyingImagePlus originator, int[] prevDimCZT, int[] prevCZT,
                                            ImageProcessor prevIp, int[] curDimCZT, int[] curCZT, ImageProcessor curIp ) {
            super(originator, prevCZT, prevIp, curCZT, curIp);
            //Standard enforcement
            assert( prevDimCZT.length == curDimCZT.length && curDimCZT.length == 3 );
            
            prevNumChannels_ = prevDimCZT[0];
            prevNumSlices_ = prevDimCZT[1];
            prevNumFrames_ = prevDimCZT[2];
            curNumChannels_ = curDimCZT[0];
            curNumSlices_ = curDimCZT[1];
            curNumFrames_ = curDimCZT[2];
        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevDimCZT - the previous dimensions in Channel, Slice, Frame
         * @param prevCZT - The previous Channel, Slice, Frame before the change
         * @param prevIp - The Previous ImageProcessor at that Position
         * @param curDimCZT - the current dimensions in Channel, Slice, Frame
         * @param curCZT - The current Channel, Slice, Frame
         * @param curIp - the Current ImageProcessor at the new Position
         */
        public StackDimensionsChangedEvent( StackDimensionsChangedEvent originatorSource, int[] prevDimCZT, int[] prevCZT,
                                            ImageProcessor prevIp, int[] curDimCZT, int[] curCZT, ImageProcessor curIp ) {
            super( originatorSource, prevCZT, prevIp, curCZT, curIp);  //Copy Constructor for Super
            
            //Standard enforcement
            assert( prevCZT.length == curCZT.length && curCZT.length == 3 );
            
            prevNumChannels_ = prevDimCZT[0];
            prevNumSlices_ = prevDimCZT[1];
            prevNumFrames_ = prevDimCZT[2];
            curNumChannels_ = curDimCZT[0];
            curNumSlices_ = curDimCZT[1];
            curNumFrames_ = curDimCZT[2];
            
        }
        
        public int getPreviousNumChannels() {
            return prevNumChannels_;
        }
        
        public int getCurrentNumChannels() {
            return curNumChannels_;
        }
        
        public int getPreviousNumSlices() {
            return prevNumSlices_;
        }
        
        public int getCurrentNumSlices() {
            return curNumSlices_;
        }
        
        public int getPreviousNumFrames() {
            return prevNumFrames_;
        }
        
        public int getCurrentNumFrames() {
            return curNumFrames_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New StackPositionChangedEvent object with prevCZT from this and curCZT from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof StackDimensionsChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            StackDimensionsChangedEvent upCast = (StackDimensionsChangedEvent) event;
            int[] curCZT = new int[]{upCast.getCurrentChannel(), upCast.getCurrentSlice(), upCast.getCurrentFrame() };
            int[] curDimCZT = new int[]{ upCast.getCurrentNumChannels(), upCast.getCurrentNumSlices(), upCast.getCurrentNumFrames() };
            int[] prevDimCZT = new int[]{ prevNumChannels_, prevNumSlices_, prevNumFrames_ };
            int[] prevCZT = new int[]{ getPreviousChannel(), getPreviousSlice(), getPreviousFrame() };
            return new StackDimensionsChangedEvent( upCast, prevDimCZT, prevCZT, getPreviousImageProcessor(), curDimCZT, curCZT, upCast.getCurrentImageProcessor() );
        }
        
    }
   
     /**
     * Event Class - Title for the ImagePlus has changed
     * <p>
     * This Event Records the previous Title for the ImagePlus and the current Set one.
     * Title May be "", but never null in this event.
     * <p>
     * Causing Methods:  setProcessor(String, ImageProcessor), setStack(String, ImageStack),
     *                   copyAttributes( ImagePlus )
     * 
     * Underlying Methods: setTitle(String)
     * 
     * @see ImagePlus#setTitle(java.lang.String) 
     * @see ImagePlus#setProcessor(java.lang.String, ij.process.ImageProcessor)
     * @see ImagePlus#setStack(java.lang.String, ij.ImageStack) 
     * @see ImagePlus#copyAttributes(ij.ImagePlus) 
     */
    public static class TitleChangedEvent extends MasterSpecificEvent {
        
        private final String prevTitle_;
        private final String curTitle_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevTitle - The previous Title for the ImagePlus and Window
         * @param curTitle - The current Title for the ImagePlus and Window
         */
        public TitleChangedEvent( PropertyNotifyingImagePlus originator, String prevTitle, String curTitle ) {
            super(originator);

            //In case later revisions remove the null rule.
            if( prevTitle == null ) {
                prevTitle = "";
            }
            if( curTitle == null ) {
                curTitle = "";
            }
            
            prevTitle_ = prevTitle;
            curTitle_ = curTitle;
        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevTitle - The previous Title for the ImagePlus and Window
         * @param curTitle - The current Title for the ImagePlus and Window
         */
        public TitleChangedEvent( TitleChangedEvent originatorSource, String prevTitle, String curTitle ) {
            super( originatorSource );  //Copy Constructor for Super
            
            //In case later revisions remove the null rule.
            if( prevTitle == null ) {
                prevTitle = "";
            }
            if( curTitle == null ) {
                curTitle = "";
            }
            
            prevTitle_ = prevTitle;
            curTitle_ = curTitle;
            
        }
        
        public String getPreviousTitle() {
            return prevTitle_;
        }
        
        public String getCurrentTitle() {
            return curTitle_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New TitleChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof TitleChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            TitleChangedEvent upCast = (TitleChangedEvent) event;
            return new TitleChangedEvent( upCast, prevTitle_, upCast.getCurrentTitle() );
        }
        
    }
    
    /**
     * Event Class -Image for the ImagePlus has changed
     * <p>
     * This Event Records the previous image for the ImagePlus and the current Set one.
     * Image may be null.  It is expected that Image for the ImageProcessor and ImagePlus are the same,
     * but has not been exhaustively tested.
     * <p>
     * Causing Methods:  setProcessor(String, ImageProcessor), setStack(String, ImageStack),
     *                   setImage( ImagePlus )
     * 
     * 
     * @see ImagePlus#setImage(java.awt.Image) 
     * @see ImagePlus#setProcessor(java.lang.String, ij.process.ImageProcessor)
     * @see ImagePlus#setStack(java.lang.String, ij.ImageStack) 
     */
    public static class ImageChangedEvent extends MasterSpecificEvent {
        
        private final Image prevImg_;
        private final Image curImg_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevImg - The previous Image for the ImagePlus and Window
         * @param curImg - The current Image for the ImagePlus and Window
         */
        public ImageChangedEvent( PropertyNotifyingImagePlus originator, Image prevImg, Image curImg ) {
            super(originator);

            prevImg_ = prevImg;
            curImg_ = curImg;
        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevImg - The previous Image for the ImagePlus and Window
         * @param curImg - The current Image for the ImagePlus and Window
         */
        public ImageChangedEvent( ImageChangedEvent originatorSource, Image prevImg, Image curImg ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevImg_ = prevImg;
            curImg_ = curImg;
            
        }
        
        public Image getPreviousImage() {
            return prevImg_;
        }
        
        public Image getCurrentImage() {
            return curImg_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New ImageChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof ImageChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            ImageChangedEvent upCast = (ImageChangedEvent) event;
            return new ImageChangedEvent( upCast, prevImg_, upCast.getCurrentImage() );
        }
        
    }
    
     /**
     * Event Class - Calibration for the ImagePlus has changed
     * <p>
     * This Event Records the previous Calibration and the current Calibration.
     * <p>
     * Causing Methods:  setProcessor(String, ImageProcessor), setStack(String, ImageStack),
     *                   copyAttributes( ImagePlus )
     * 
     * Underlying Methods: setTitle(String)
     * 
     * @see ImagePlus#setTitle(java.lang.String) 
     * @see ImagePlus#setProcessor(java.lang.String, ij.process.ImageProcessor)
     * @see ImagePlus#setStack(java.lang.String, ij.ImageStack) 
     * @see ImagePlus#copyAttributes(ij.ImagePlus) 
     */
    public static class CalibrationChangedEvent extends MasterSpecificEvent {
        
        private final Calibration prevCal_;
        private final Calibration curCal_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevCal - The previous Calibration Instance
         * @param curCal - The current Calibration Instance
         */
        public CalibrationChangedEvent( PropertyNotifyingImagePlus originator, Calibration prevCal, Calibration curCal ) {
            super(originator);

            prevCal_ = prevCal;
            curCal_ = curCal;
        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevCal - The previous Calibration Instance
         * @param curCal - The current Calibration Instance
         */
        public CalibrationChangedEvent( CalibrationChangedEvent originatorSource, Calibration prevCal, Calibration curCal ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevCal_ = prevCal;
            curCal_ = curCal;
            
        }
        
        public Calibration getPreviousCalibration() {
            return prevCal_;
        }
        
        public Calibration getCurrentCalibration() {
            return curCal_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New CalibrationChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof CalibrationChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            CalibrationChangedEvent upCast = (CalibrationChangedEvent) event;
            return new CalibrationChangedEvent( upCast, prevCal_, upCast.getCurrentCalibration() );
        }
        
    }
    
    /**
     * Event Class - The Dimension of the underlying Image has changed
     * <p>
     * This Event Records the previous height and width and the current height and width
     * <p>
     * Causing Methods:  setProcessor(String, ImageProcessor), setStack(String, ImageStack),
     *                   copyAttributes( ImagePlus ), setImage( Image )
     * 
     * 
     * @see ImagePlus#setProcessor(java.lang.String, ij.process.ImageProcessor)
     * @see ImagePlus#setStack(java.lang.String, ij.ImageStack) 
     * @see ImagePlus#copyAttributes(ij.ImagePlus) 
     * @see ImagePlus#setImage(java.awt.Image) 
     */
    public static class ImageDimensionsChangedEvent extends MasterSpecificEvent {
        
        private final int prevWidth_;
        private final int curWidth_;
        private final int curHeight_;
        private final int prevHeight_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevWidth - The previous Width of the Image
         * @param curWidth - The current Width of the Image
         * @param prevHeight - The previous Width of the Image
         * @param curHeight - The current Width of the Image
         */
        public ImageDimensionsChangedEvent( PropertyNotifyingImagePlus originator,
                                                int prevWidth, int prevHeight, int curWidth, int curHeight ) {
            super(originator);

            prevWidth_ = prevWidth;
            curWidth_ = curWidth;
            prevHeight_ = prevHeight;
            curHeight_ = curHeight;
        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevWidth - The previous Width of the Image
         * @param curWidth - The current Width of the Image
         * @param prevHeight - The previous Width of the Image
         * @param curHeight - The current Width of the Image
         */
        public ImageDimensionsChangedEvent( ImageDimensionsChangedEvent originatorSource, 
                                                int prevWidth, int prevHeight, int curWidth, int curHeight ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevWidth_ = prevWidth;
            curWidth_ = curWidth;
            prevHeight_ = prevHeight;
            curHeight_ = curHeight;
            
        }
        
        public int getPreviousWidth() {
            return prevWidth_;
        }
        
        public int getCurrentWidth() {
            return curWidth_;
        }
        
        public int getPreviousHeight() {
            return prevHeight_;
        }
        
        public int getCurrentHeight() {
            return curHeight_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New ImageDimensionsChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof ImageDimensionsChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            ImageDimensionsChangedEvent upCast = (ImageDimensionsChangedEvent) event;
            return new ImageDimensionsChangedEvent( upCast, prevWidth_, prevHeight_,  upCast.getCurrentWidth(), upCast.getCurrentHeight() );
        }
        
    }
   
    /**
     * Event Class - The FileInfo Object Associated with the ImagePlus has changed
     * <p>
     * This Event Records the previous FileInfo and current FileInfo object.  These Objects may 
     * or may not be the same references.  It is the users burden to validate them, if that is important.
     * This is unfortunately due to the unexposed internal object.
     * <p>
     * Causing Methods:  setFileInfo( FileInfo )
     * 
     * 
     * @see ImagePlus#setFileInfo(ij.io.FileInfo) 
     */
    public static class FileInfoChangedEvent extends MasterSpecificEvent {
        
        private final FileInfo prevFi_;
        private final FileInfo curFi_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevFi - The previous FileInfo object
         * @param curFi - The current FileInfo object
         */
        public FileInfoChangedEvent( PropertyNotifyingImagePlus originator,
                                                FileInfo prevFi, FileInfo curFi ) {
            super(originator);

            prevFi_ = prevFi;
            curFi_ = curFi;

        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevFi - The previous FileInfo object
         * @param curFi - The current FileInfo object
         */
        public FileInfoChangedEvent( FileInfoChangedEvent originatorSource, 
                                                FileInfo prevFi, FileInfo curFi ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevFi_ = prevFi;
            curFi_ = curFi;
            
        }
        
        public FileInfo getPreviousFileInfo() {
            return prevFi_;
        }
        
        public FileInfo getCurrentFileInfo() {
            return curFi_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New FileInfoChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof FileInfoChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            FileInfoChangedEvent upCast = (FileInfoChangedEvent) event;
            return new FileInfoChangedEvent( upCast, prevFi_, upCast.getCurrentFileInfo() );
        }
        
    }
    
    /**
     * Event Class - The imageType of the ImagePlus has changed
     * <p>
     * This Event Records the previous imageType and the current ImageType.  Unfortunately, setting one does not necessarily indicate setting 
     * the same in the image processor.  Listening for ImageProcessor type changes is advised, but due to the possibility of
     * legacy code utilizing just imageType, this event is provided as well.
     * <p>
     * Causing Methods:  setFileInfo( FileInfo )
     * 
     * 
     * @see ImagePlus#setFileInfo(ij.io.FileInfo) 
     */
    public static class ImageTypeChangedEvent extends MasterSpecificEvent {
        
        private final int prevType_;
        private final int curType_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevType - The previous Type of the image
         * @param curType - The current type of the image
         */
        public ImageTypeChangedEvent( PropertyNotifyingImagePlus originator,
                                                int prevType, int curType ) {
            super(originator);

            prevType_ = prevType;
            curType_ = curType;

        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevType - The previous Type of the image
         * @param curType - The current type of the image
         */
        public ImageTypeChangedEvent( ImageTypeChangedEvent originatorSource, 
                                                int prevType, int curType ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevType_ = prevType;
            curType_ = curType;
            
        }
        
        /**
         * Type if of ImagePlus.Gray8, ImagePlus.Gray16, etc.
         * @return 
         */
        public int getPreviousImageType() {
            return prevType_;
        }
        
        /**
         * Type if of ImagePlus.Gray8, ImagePlus.Gray16, etc.
         * @return 
         */
        public int getCurrentImageType() {
            return curType_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New ImageTypeChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof ImageTypeChangedEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            ImageTypeChangedEvent upCast = (ImageTypeChangedEvent) event;
            return new ImageTypeChangedEvent( upCast, prevType_, upCast.getCurrentImageType() );
        }
        
    }
    
     /**
     * Event Class - The Roi that is applied across the stack of the ImagePlus has changed
     * <p>
     * Different than the ImageProcessor having its Roi changed, this tracks the current Roi of the ImagePlus, which may not 
     * be immediately applied to the ImageProcessor.  This tracks the current and previous Roi.
     * <p>
     * Causing Methods:  setRoi( Roi, boolean ), deleteRoi()
     * 
     * 
     * @see ImagePlus#setRoi(ij.gui.Roi, boolean)  
     * @see ImagePlus#deleteRoi() 
     */
    public static class RoiChangeEvent extends MasterSpecificEvent {
        
        private final Roi prevRoi_;
        private final Roi curRoi_;
        
         /**
         * Constructor
         * 
         * @param originator - originating instance for overlay change
         * @param prevRoi - The previous Roi of the ImagePlus
         * @param curRoi - The current Roi of the ImagePlus
         */
        public RoiChangeEvent( PropertyNotifyingImagePlus originator, Roi prevRoi, Roi curRoi ) {
            super(originator);

            prevRoi_ = prevRoi;
            curRoi_ = curRoi;

        }
        
        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param prevRoi - The previous Roi of the ImagePlus
         * @param curRoi - The current Roi of the ImagePlus
         */
        public RoiChangeEvent( RoiChangeEvent originatorSource, Roi prevRoi, Roi curRoi ) {
            super( originatorSource );  //Copy Constructor for Super
            
            prevRoi_ = prevRoi;
            curRoi_ = curRoi;
            
        }
        
        public Roi getPreviousRoi() {
            return prevRoi_;
        }
        
        public Roi getCurrentRoi() {
            return curRoi_;
        }

        /**
         * Combine this event with the other.  Assumes this event is the earliest,
         * so keeps its previous and takes the current from the event.
         * 
         * @param event - the More Recent Event compared to this
         * @return New RoiChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof RoiChangeEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            RoiChangeEvent upCast = (RoiChangeEvent) event;
            return new RoiChangeEvent( upCast, prevRoi_, upCast.getCurrentRoi() );
        }
        
    }
    
    /**
     * Event Class - For Testing Purposes Only, Presents an Dummy Event for Use Cases involving the Context system
     */
    protected static class DummyEvent extends MasterSpecificEvent {
        
        private int concatCount_;
        
        /**
         * General Constructor
         * @param orignator 
         */
        public DummyEvent( PropertyNotifyingImagePlus originator ) {
            super( originator );
            concatCount_ = 0;
        }
        
        /**
         * Incremental Copy Constructor - Increments the concatCount_
         * @param evt 
         */
        protected DummyEvent( DummyEvent evt ) {
            super( evt );
            concatCount_ = (evt.concatCount_ + 1);
        }
        
        /**
         * The number of times this event was concatenated, 
         * should be equivalent to the number of times it was duplicate when posted to
         * an undispatched eventQueue.
         * 
         * @return 
         */
        public int getConcatCount() {
            return concatCount_;
        }

        /**
         * Combine this event with the other.  Basically just increments the concatCount and 
         * produces a new Event with the incrementation.
         * 
         * @param event - the More Recent Event compared to this
         * @return New TitleChangedEvent object with prevTitle from this and curTitle from the parameter
         */
        @Override
        public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
            //Make Sure This is the same Instance
            if( !(event instanceof DummyEvent ) ) {
                throw new IllegalArgumentException( "Event To Concatenate is instanceof " + this.getClass().getName() );
            } else if( !sameOriginator( event ) ) {
                throw new IllegalArgumentException( "Attempting to Concatenate Events from two Seperate Originators");
            }
            
            //Copy Increment Constructor
            return new DummyEvent( this );
        }
        
    }
    
    /**
     * Helper Class for storing Information of a Calling Method.
     * This acts as a reentrant lock, but avoids using it so that threading is not
     * confused
     */
    private class CallingMethodRecord {
    
        private int numCalls_;
        private final String methodName_;
    
        public CallingMethodRecord( String methodName ) {
           methodName_ = methodName;
           numCalls_ = 1;
        }
        
        /**
         * Gets the method Name that was used to invoke this object
         * @return 
         */
        public String getMethodName() {
            return methodName_;
        }
        
        /**
         * Returns the number of times the method has been called without finishing
         * @return 
         */
        public int getNumCalls() {
            return numCalls_;
        }
        
        /**
         *  Increments the number of Calls that have not been terminated
         */
        public void incNumMethodCall() {
            numCalls_++;
        }
        
        /**
         *  Decrements the number of calls.  Should be called upon termination of the call.
         */
        public void decNumMethodCall() {
            numCalls_--;
        }
    }
    
    private static int busUniquenessIdx_ = 0;
    
    private EventBus bus_;
    private final Map< Long, CallingMethodRecord  > currentCallingThreadIds_ = new HashMap<Long, CallingMethodRecord>();  //Map of Current ThreadIds and CallingMethodRecord
    //An Access-ordered Event Queue of ClassNames and Their respective Objects
    private final Map<String, MasterSpecificEvent> pendingEventQueue_ = Collections.synchronizedMap( new LinkedHashMap<String, MasterSpecificEvent>( 20, .75f, true ) );

     /**
     * Constructs a PropertyNotifyingImagePlus from an Image or BufferedImage. The first
     * argument will be used as the title of the window that displays the image.
     * Throws an IllegalStateException if an error occurs while loading the
     * image.
     * @param title - The title for the imagePlus (that will be used in saves and ImageWindows)
     * @param img - the image to be stored in this ImagePlus
     */
    public PropertyNotifyingImagePlus(String title, Image img) {
        super(title, img);
        setupBusInstance();
    }

    /**
     * Constructs an PropertyNotifyingImagePlus from an ImageProcessor.
     * 
     * @param title - The title for the imagePlus (that will be used in saves and ImageWindows)
     * @param ip - The ImageProcessor with info about the actual image
     */
    public PropertyNotifyingImagePlus(String title, ImageProcessor ip) {
        super(title, ip);
        setupBusInstance();
    }

    /**
     * Constructs an PropertyNotifyingImagePlus from a TIFF, BMP, DICOM, FITS, PGM, GIF or JPRG
     * specified by a path or from a TIFF, DICOM, GIF or JPEG specified by a
     * URL.
     * @param pathOrURL - a TIFF, BMP, DICOM, FITS, PGM, GIF or JPRG specified by a path or URL.
     */
    public PropertyNotifyingImagePlus(String pathOrURL) {
        super(pathOrURL);
        setupBusInstance();
        
    }

    /**
     * Constructs an PropertyNotifyingImagePlus from a stack.
     * @param title - The title for the imagePlus (that will be used in saves and ImageWindows)
     * @param stack - the ImageStack that the ImagePlus will hold
     */
    public PropertyNotifyingImagePlus(String title, ImageStack stack) {
        super(title, stack);
        setupBusInstance();
        
        //This should ensure Stack Dimensions are correct
        getNChannels();
        getNSlices();
        getNFrames();
        
    }
    
    /**
     * Registers an Object to this object's separate EventBus instance.  
     * 
     * @param subscriber - an EventBus Subscriber to be registered to this objects Event's
     */
    public void RegisterToEventBus( Object subscriber ) {
        bus_.register(subscriber);
    }
    
    /**
     * Unregisters an Object from this instance's separate EventBus instance.
     * 
     * @param subscriber - the EventBus Subscriber to be unregistered from this objects Event's
     */
    public void UnregisterFromEventBus( Object subscriber ) {
        bus_.unregister(subscriber);
    }
    
    /**
     * Posts an Event to The Event Queue.  This is necessary to allow for these 
     * redundant and cyclic methods from ImagePlus to post all their events, posting Events
     * that will not be valid. 
     * <p>
     * Internals1: If an Event has already been queued, the new Event will be concatenated
     * with the old Event to produce only a single event.  See the corresponding Events for
     * details on their concatenation if curious.  Otherwise, know that only one Event of a type
     * will be present in the queue when it is dispatched.  Every new Event of the same type is significant 
     * to subscribers.
     * <p>
     * Internals2: If multiple Threads are operating on the ImagePlus at the same time,
     * The Queue will be dispatched as soon as all Threads have left their entry methods.
     * This is NOT THREADSAFE but provides a semblance of last implementation.  
     * <p>
     * TODO: figure out how to minimize recursion loops that will lock the event bus.
     * 
     * @param evt - A MasterSpecific Event to Queue to this
     */
    protected void postToEventQueue( MasterSpecificEvent evt ) {

        //Workaround for super.Constructors that call these methods as well
        if (pendingEventQueue_ != null) {
            String queueKey = evt.getClass().getName();
            synchronized (pendingEventQueue_) {
                //Check to see if there is another corresponding Event and Update it.
                MasterSpecificEvent prevEvent = pendingEventQueue_.get(queueKey);
                if (prevEvent != null) {             
                    //Concatenate This Event into a new one, (prev calls on most current, see Doc)
                    evt = prevEvent.concatenate(evt);
                }
                pendingEventQueue_.put(queueKey, evt); //This Automatically Appends Order to the End for LinkedHashMap (most recent)
            }
        }
        
    }
    
    /**
     * Test Method - For Retrieving Copy of EventQueue (Not Intended For Use outside of tests)
     * @return - Copy of the pendingEventQueue at the time it was called
     */
    protected Map<String, MasterSpecificEvent> getCopyOfEventQueue( ) {
        Map<String, MasterSpecificEvent> copy = new LinkedHashMap();
        //iterate through for a deep copy
        synchronized (pendingEventQueue_) {
            Iterator< Map.Entry<String, MasterSpecificEvent > > it = pendingEventQueue_.entrySet().iterator();
            Map.Entry< String, MasterSpecificEvent > entry;
            while( it.hasNext() ) {
                entry = it.next();
                copy.put( entry.getKey(), entry.getValue() );
            }

            return copy;
        }
    }
    
    /**
     * Test Method - Used for testing Entry Contexts with a standard DummyEvent
     * The method will setEntryContext(), post A Dummy Event and then endTheEntryContext(),
     * which should post the Event when the last thread is done accessing this object.
     */
    protected void entryContextsUtilityTest() {
        
        setEntryContext();
        
        postToEventQueue( new DummyEvent( this ) );
        
        endEntryContext();
        
    }
    
     /**
     * Test Method - Used for testing Multiple Levels of EntryContext in recursion.
     * The method will setEntryContext(), post A Dummy Event, and then iterate back into itself,
     * before endTheEntryContext() of each method.  The last exit should post the Dummy Event
     * with a number of concatenations equal to numRecursions.
     * 
     * @param numRecursions - the number of recursions into this function
     */
    protected void recursiveEntryContextsUtilityTest( int numRecursions ) {
        setEntryContext();
        
        postToEventQueue( new DummyEvent( this ) );
        
        if( numRecursions > 0  ) {
            recursiveEntryContextsUtilityTest( --numRecursions );
        }
        
        endEntryContext();
        
    }
    
    /**
     * Private Method where EventBus is setup.  Due to the nature of the super constructors
     * We also, clear the pendingEventQueue_ for normal service.
     */
    private void setupBusInstance() {
        bus_ = new EventBus( "PropertyNotifyingImagePlus" + busUniquenessIdx_ );
        //Clear the queue without Dispatching
        synchronized(pendingEventQueue_) {
            pendingEventQueue_.clear();
        }
        //Increment the Uniqueness Index for the next EventBus
        busUniquenessIdx_++;
    }
    
    
     /**
     * Dispatches all events in the EventQueue.  This is meant to be called only 
     * by endEntryContext() or any other functions that verify no other manipulation of the internal object.
     * <p>
     * Only 1 Type of Event is will be Dispatched thanks to Event concatenation in postToEventQueue().
     * Event Dispatch order is: least recent to most recent
     * 
     * @see #endEntryContext() 
     */
    private void dispatchEventQueue() {
        
        synchronized (pendingEventQueue_) {
            //LinkedHashMap AccessOrder Guarantees first is the least recent Event, etc.
            for (MasterSpecificEvent e : pendingEventQueue_.values()) {
                bus_.post(e);
            }
            //Clear The pendingEventQueue_ (memory should depend on if Bus has sticky Posts)
            pendingEventQueue_.clear();
        }
    }
    
    /**
     * This is a precautionary method. It is meant to be used to behave like a Reentrant Lock.
     * This method registers the calling context that first called it from a thread.
     * If the same method is called again, then it increments the numberOfMethodCalls.
     * This method is meant to be used in conjunction with endEntryContext to signal
     * that a method is no longer calling other linked methods in tandem.
     * <p>
     * Motivation: On examining ImagePlus, this needs to be done, since there are multiple redundant calls
     * to the same function, which means events will be triggered too much.  This is the
     * unfortunate requirement of bad encapsulation in ImageJ1.x.  This puts some requirements on
     * the Events that are called.  They are going to be evaluated and finally passed after a function call
     * if finished.
     * 
     * @return <code>true</code> if the context is used as the entry point to other methods. 
     *         <code>false</code> if the context is already registered
     */
    private boolean setEntryContext() {
        
        //Workaround for Constructors that call this method through other set methods
        if (currentCallingThreadIds_ != null) {
            //Get the Thread Id and name of the calling method one above this one.
            Long callThreadId = Thread.currentThread().getId();
            StackTraceElement[] sTraceElems = Thread.currentThread().getStackTrace();
            //Get Trace just above this function context
            String callingMethodName = sTraceElems[2].getMethodName();
            
            synchronized (currentCallingThreadIds_) {
                CallingMethodRecord methodRecord = currentCallingThreadIds_.get(callThreadId);
                if (methodRecord == null) {
                    //If we have not already logged this Thread as currently executing a method
                    currentCallingThreadIds_.put(callThreadId, new CallingMethodRecord(callingMethodName));
                    return true;
                } else {
                    //If this is the Calling Thread, check to see if the method name is the same
                    if (callingMethodName.equals(methodRecord.getMethodName())) {
                        methodRecord.incNumMethodCall(); //Increase our count like on Reentrant Lock
                    }
                    return false;
                }
            }
        }
        return false;
    }
    
   /**
    * Used in Conjunction with setEntryContext().  Assumes that a setEntryContext is paired 
    * with a an endEntryContext in the same method.  This should be wrapped in a finally,
    * just like with a Reentrant lock.  If multiple entrances into the entry method have been
    * logged, it will decrement the numberOfMethodCalls until it is 0.  When the Context is 0,
    * the Entry context is unregistered with the object.
    * <p>
    * This function is responsible for Calling the DispatchEventQueue() method, when 
    * there are no other EntryPoints still modifying.
    * 
    * @return <code>true</code> if the EntryContext number of MethodCalls is 0 and has been removed
    *         from the list for that Thread. <code>false</code> if the EntryContext is not from the
    *         calling method, or if the numberOfMethodCalls is still non-zero.
    * 
    * 
    */
    private boolean endEntryContext() {
        
        //Workaround for super.Constructors that call these methods as well
        if (currentCallingThreadIds_ != null) {
            //Get the Thread Id and name of the calling method one above this one.
            Long callThreadId = Thread.currentThread().getId();
            StackTraceElement[] sTraceElems = Thread.currentThread().getStackTrace();
            //Get Trace just above this function context
            String callingMethodName = sTraceElems[2].getMethodName();

            synchronized (currentCallingThreadIds_) {
                //System.out.println("should get for key: " + callThreadId + " and Calling method Name " + callingMethodName);
                CallingMethodRecord methodRecord = currentCallingThreadIds_.get(callThreadId);
                //System.out.println("Keys are: ");
                for (Long key : currentCallingThreadIds_.keySet()) {
                //    System.out.println(key + " CallingMethodName is: " + currentCallingThreadIds_.get(key).getMethodName());
                }
                //System.out.println("should have gotten the methodRecord");
                //If the current Calling Thread and callingMethodName have a Record
                if (methodRecord != null && callingMethodName.equals(methodRecord.getMethodName())) {
                    methodRecord.decNumMethodCall();  //Decrement the number of calls recorded
                    if (methodRecord.getNumCalls() == 0) {
                        currentCallingThreadIds_.remove(callThreadId);
                        //If the Calling Thread Ids is empty, then dispatch the Event Queue
                        if (currentCallingThreadIds_.isEmpty()) {
                            dispatchEventQueue();
                        }
                        return true;
                    }
                }
            }
        }
        return false;
        
    }
    
    /**
     * Set Current Overlay (Override for Event Dispatch)
     * <p>
     * Note:  All Other Overloads, wrap to this method.  Unfortunately, 
     * due to the poor nature of Overlay Class, there is no easy equality to be 
     * found.  Therefore, it is the implementers choice to validate it.  At either 
     * rate the image canvas is updated, and the reference may be the same but with different
     * properties.
     * @see ImagePlus#setOverlay(ij.gui.Overlay)  
     */
    @Override
    public void setOverlay(Overlay ov){

        setEntryContext();
                
        Overlay prevOv = getOverlay();
        super.setOverlay(ov);
        
        //Post The previous and current Set Ov as an Event
        postToEventQueue( new OverlayChangedEvent( this, prevOv, getOverlay() ) );
        
        endEntryContext(); //Dispatch Event Queue if this is the Entry Context
    }
    
     /**
     * Set Current Overlay Visibility (Override for Event Dispatch: OverlayVisibilityChangedEvent )
     * <p>
     * Note: This method sets the imageCanvas to visible.  Events May be of use to programs
     *       Wanting to track what the user sees in a window.  Redundancy may actually 
     *       cyclically toggle on-off-on the canvas, and so by the time an event is 
     *       queued, the subscriber should be aware that a Changed Event should have
     *       its current change evaluated.
     * 
     * @see ImagePlus#setOverlay(ij.gui.Overlay)  
     */
    @Override
    public void setHideOverlay(boolean hide){

        setEntryContext();
                
        boolean prevIsVisible = getHideOverlay();
        super.setHideOverlay( hide );
        
        //Check to make sure we changed the Visibility in this one call
        if( prevIsVisible != getHideOverlay() ) {
            postToEventQueue( new OverlayVisibilityChangedEvent( this, prevIsVisible, !prevIsVisible ) );
        }
  
        endEntryContext();
    }
    
     /**
     * Set Current Position (Override for Event Dispatch: StackPositionChangedEvent)
     * <p>
     * Note1: Event Will be fired if a change is detected.  It is the burden of the subscriber to check 
     * and see if this is the case.  This also means Single Images Also report a 
     * PositionChanged if there is a transition from a stack to a single Image.
     * <p>
     * Potential problem:  Cyclic calls to UpdatePosition exposing Multiple Events 
     * at once. - Fixed by Concatenation in postToEventQueue()
     * 
     * @see ImagePlus#setOverlay(ij.gui.Overlay)  
     */
    @Override
    public void updatePosition(int c, int z, int t){
        
        setEntryContext();
        
        int[] prevCVT = new int[] {getC(),getZ(),getT()};
        ImageProcessor prevIp = getProcessor(); //Potentially throws a changed ImageProcessor Event Exception
        super.updatePosition(c, z, t);
        
        ImageProcessor curIp = getProcessor(); //Potentially throws a changed ImageProcessor Event Exception
        
        //Ensure a Change in the Position Occurred
        if( prevCVT[0] != getC() || prevCVT[1] != getZ() || prevCVT[2] != getT() ) {
            //Post The previous and current Set Ov as an Event
            postToEventQueue( new StackPositionChangedEvent( this, prevCVT, prevIp, new int[]{getC(),getZ(),getT()}, curIp ) );            
        }
        
        endEntryContext();
    }
    
     /**
     * Set StackPosition (Override for Event Dispatch: StackPositionDimensionChangedEvent)
     * <p>
     * Note1: This event is an extension of a PositionChanged Event, since relative to the Stack, 
     *      A change in dimensions necessarily means a change in its position.  It is suggested that
     *      A subscriber listen for a StackPositionChangeUpdate and then perform instanceof
     *      detection of the Dimension Event.  Otherwise, one might listen for both isolated Incidents
     * 
     * @see ImagePlus#setDimensions(int, int, int) 
     */
    public void setDimensions( int nChannels, int nSlices, int nFrames ) {
        
        setEntryContext();
        
        int prevNChannels = getNChannels(), prevNSlices = getNSlices(), prevNFrames = getNFrames();
        int prevC = getC(), prevZ = getZ(), prevT = getT();
        ImageProcessor prevIp = getProcessor();
        
        System.out.println( "The Channels before set is: " + prevNChannels + "," + prevNSlices + "," + prevNFrames );
        System.out.println( "The personal Channels before set is: " + this.nChannels + "," + this.nSlices + "," + this.nFrames );
        
        super.setDimensions( nChannels, nSlices, nFrames );
        
        int curNChannels = getNChannels(), curNSlices = getNSlices(), curNFrames = getNFrames();
        int curC = getC(), curZ = getZ(), curT = getT();
        ImageProcessor curIp = getProcessor();
        
        if( prevNChannels != curNChannels || prevNSlices != curNSlices || prevNFrames != curNFrames ) {
            //For organization, JVM should optimize
            int[] prevDimCZT = new int[]{prevNChannels, prevNSlices, prevNFrames};
            int[] curDimCZT = new int[]{ curNChannels, curNSlices, curNFrames };
            int[] prevCZT = new int[]{ prevC, prevZ, prevT };
            int[] curCZT = new int[]{ curC, curZ, curT };
                        
            postToEventQueue( new StackDimensionsChangedEvent( this, prevDimCZT, prevCZT, prevIp, curDimCZT, curCZT, curIp ) );
        }
        
        endEntryContext();
        
    }
    
    
    /**
     * Get Number of Slices in Stack (Override for Event Dispatch: StackPositionDimensionChangedEvent)
     * <p>
     * Note1: Due to Poor Getters, the getNSlices method will readjust the Stack dimensions if there are discrepancies.
     *        This Method may then throw an event exception to indicate that the Dimenions have changed (if indeed they have).
     *        When using this method, keep in mind that the Event may be triggered before the return.
     * 
     * @see ImagePlus#getNSlices() 
     */
    @Override
    public int getNSlices() {
        
        setEntryContext();
        
        int prevNChannels = this.nChannels, prevNSlices = this.nSlices, prevNFrames = this.nFrames;
        int prevC = getC(), prevZ = getZ(), prevT = getT();
        ImageProcessor prevIp = getProcessor();
        
        int curNSlices = super.getNSlices( );
        
        int curNChannels = this.nChannels, curNFrames = this.nFrames;
        int curC = getC(), curZ = getZ(), curT = getT();
        ImageProcessor curIp = getProcessor();
        
        //If there was an actual modification (because there can be in getters) send out an event
        if( prevNChannels != curNChannels || prevNSlices != curNSlices || prevNFrames != curNFrames ) {
            //For organization, JVM should optimize
            int[] prevDimCZT = new int[]{prevNChannels, prevNSlices, prevNFrames};
            int[] curDimCZT = new int[]{ curNChannels, curNSlices, curNFrames };
            int[] prevCZT = new int[]{ prevC, prevZ, prevT };
            int[] curCZT = new int[]{ curC, curZ, curT };
            postToEventQueue( new StackDimensionsChangedEvent( this, prevDimCZT, prevCZT, prevIp, curDimCZT, curCZT, curIp ) );
        }
        
        endEntryContext();
        
        return curNSlices;  //Return the number of Slices
        
    }
    
     /**
     * Get Number of Channels in Stack (Override for Event Dispatch: StackPositionDimensionChangedEvent)
     * <p>
     * Note1: Due to Poor Getters, the getNSlices method will readjust the Stack dimensions if there are discrepancies.
     *        This Method may then throw an event exception to indicate that the Dimenions have changed (if indeed they have).
     *        When using this method, keep in mind that the Event may be triggered before the return.
     * 
     * @see ImagePlus#getNChannels() 
     */
    @Override
    public int getNChannels() {
        
        setEntryContext();
        
        int prevNChannels = this.nChannels, prevNSlices = this.nSlices, prevNFrames = this.nFrames;
        int prevC = getC(), prevZ = getZ(), prevT = getT();
        ImageProcessor prevIp = getProcessor();
        
        int curNChannels = super.getNChannels( );
        
        int curNSlices = this.nSlices, curNFrames = this.nFrames;
        int curC = getC(), curZ = getZ(), curT = getT();
        ImageProcessor curIp = getProcessor();
        
        //If there was an actual modification (because there can be in getters) send out an event
        if( prevNChannels != curNChannels || prevNSlices != curNSlices || prevNFrames != curNFrames ) {
            //For organization, JVM should optimize
            int[] prevDimCZT = new int[]{prevNChannels, prevNSlices, prevNFrames};
            int[] curDimCZT = new int[]{ curNChannels, curNSlices, curNFrames };
            int[] prevCZT = new int[]{ prevC, prevZ, prevT };
            int[] curCZT = new int[]{ curC, curZ, curT };
            postToEventQueue( new StackDimensionsChangedEvent( this, prevDimCZT, prevCZT, prevIp, curDimCZT, curCZT, curIp ) );
        }
        
        endEntryContext();
        
        return curNChannels;  //Return the number of Slices
        
    }
    
    /**
     * Get Number of Frames in Stack (Override for Event Dispatch: StackPositionDimensionChangedEvent)
     * <p>
     * Note1: Due to Poor Getters, the getNSlices method will readjust the Stack dimensions if there are discrepancies.
     *        This Method may then throw an event exception to indicate that the Dimenions have changed (if indeed they have).
     *        When using this method, keep in mind that the Event may be triggered before the return.
     * 
     * @see ImagePlus#getNFrames() 
     */
    @Override
    public int getNFrames() {
        
        setEntryContext();
        
        int prevNChannels = this.nChannels, prevNSlices = this.nSlices, prevNFrames = this.nFrames;
        int prevC = getC(), prevZ = getZ(), prevT = getT();
        ImageProcessor prevIp = getProcessor();
        
        int curNFrames = super.getNFrames( );
        
        int curNSlices = this.nSlices, curNChannels = this.nChannels;
        int curC = getC(), curZ = getZ(), curT = getT();
        ImageProcessor curIp = getProcessor();
        
        //If there was an actual modification (because there can be in getters) send out an event
        if( prevNChannels != curNChannels || prevNSlices != curNSlices || prevNFrames != curNFrames ) {
            //For organization, JVM should optimize
            int[] prevDimCZT = new int[]{prevNChannels, prevNSlices, prevNFrames};
            int[] curDimCZT = new int[]{ curNChannels, curNSlices, curNFrames };
            int[] prevCZT = new int[]{ prevC, prevZ, prevT };
            int[] curCZT = new int[]{ curC, curZ, curT };
            postToEventQueue( new StackDimensionsChangedEvent( this, prevDimCZT, prevCZT, prevIp, curDimCZT, curCZT, curIp ) );
        }
        
        endEntryContext();
        
        return curNFrames;  //Return the number of Slices
        
    }
    
    /**
     * Get the Dimensions of the stack (Override for Event Dispatch: StackPositionDimensionChangedEvent)
     * <p>
     * Note1: Due to Poor Getters, the getDimensions method will readjust the Stack dimensions if there are discrepancies and verify is <code>true</code>.
     *        This Method may then throw an event exception to indicate that the Dimenions have changed (if indeed they have).
     *        When using this method, keep in mind that the Event may be triggered before the return.
     * 
     * @see ImagePlus#getNFrames() 
     */
    @Override
    public int[] getDimensions( boolean verify ) {
        
        //Only verification causes an event to be triggered
        int prevNChannels = 0, prevNSlices = 0, prevNFrames = 0, prevC = 0, prevZ = 0, prevT = 0;  //Initialization due to split verify Evaluation
        ImageProcessor prevIp = null;
        if( verify ) {
            setEntryContext();
        
            
            prevNChannels = this.nChannels;
            prevNSlices = this.nSlices;
            prevNFrames = this.nFrames;
            prevC = getC();
            prevZ = getZ();
            prevT = getT();
            prevIp = getProcessor();
        }
        
        int[] dims = super.getDimensions( verify );
        
        if( verify ) {
            int curNChannels = this.nChannels, curNSlices = this.nSlices, curNFrames = this.nFrames;
            int curC = getC(), curZ = getZ(), curT = getT();
            ImageProcessor curIp = getProcessor();

            //If there was an actual modification (because there can be in getters) send out an event
            if (prevNChannels != curNChannels || prevNSlices != curNSlices || prevNFrames != curNFrames) {
                //For organization, JVM should optimize
                int[] prevDimCZT = new int[]{prevNChannels, prevNSlices, prevNFrames};
                int[] curDimCZT = new int[]{curNChannels, curNSlices, curNFrames};
                int[] prevCZT = new int[]{prevC, prevZ, prevT};
                int[] curCZT = new int[]{curC, curZ, curT};
                
                postToEventQueue(new StackDimensionsChangedEvent(this, prevDimCZT, prevCZT, prevIp, curDimCZT, curCZT, curIp));
            }

            endEntryContext();
        }
        
        return dims;  //Return the number of Slices
        
    }
    
    
     /**
     * Set the Image stored in the ImagePlus and the imageProcessor (Override for Event Dispatch: ImageProcessorChangeEvent, ImageChangeEvent)
     * <p>
     * Note: This method is essentially a wrapper for setProcessor, but then it also unfortunately 
     *       reimplements the methods of setProcessor if the Image is not a BufferedImage.  Therefore,
     *       This method must also check for events to the processor as in setProcessor.  These will be
     *       concatenated or discarded in the EventQueue.  
     * 
     * @see ImagePlus#setImage(java.awt.Image)    
     */
    @Override
    public void setImage(Image image) {
        
        setEntryContext();
        
        ImageProcessor prevIp = getProcessor();  //May present Event
        Image prevImg = getImage();  //GetImage is the equivalent here
        int prevW = getWidth();
        int prevH = getHeight();
        //Channel and Frame Information due to potential changes
        int prevNumChannels = this.nChannels, prevNumSlices = this.nSlices, prevNumFrames = this.nFrames;
        int prevC = getC(), prevZ = getZ(), prevT = getT();
        
        super.setImage( image );
        
        ImageProcessor curIp = this.ip;  //get new imageProcessor Potentially
        
        //This method actually resets the stack... if not a BufferedImage
        //To test if there was a Dimension Change, we simply use the getters because they do too much.
        int curNumChannels = getNChannels(), curNumSlices = getNSlices(), curNumFrames = getNFrames();
        if( curNumChannels != prevNumChannels || curNumSlices != prevNumSlices || curNumFrames != prevNumFrames ) {
            //For organization, JVM should optimize
            int[] prevDimCZT = new int[]{prevNumChannels, prevNumSlices, prevNumFrames};
            int[] curDimCZT = new int[]{curNumChannels, curNumSlices, curNumFrames};
            int[] prevCZT = new int[]{prevC, prevZ, prevT};
            int[] curCZT = new int[]{getC(), getZ(), getT() };
            postToEventQueue(new StackDimensionsChangedEvent(this, prevDimCZT, prevCZT, prevIp, curDimCZT, curCZT, curIp));
        }
        
        
        //Detect any event changes corresponding to potentially calling setupProcessor()
        int evtFlags = 0;
        
        if( prevIp != curIp ) {
            evtFlags |= ImageProcessorChangeEvent.REF_CHANGED;
        }
        //Make sure that we didn't get a null object, otherwise the reference changed is enough
        if( prevIp != null && curIp != null ) {
            if( prevIp.getCalibrationTable() != curIp.getCalibrationTable() ) {
                evtFlags |= ImageProcessorChangeEvent.CALIBRATION_CHANGED;
            }
            if( curIp.getClass() != prevIp.getClass() ) {
                evtFlags |= ImageProcessorChangeEvent.TYPE_CHANGED;
            }
            //getRoi returns a Rectangle always not same reference
            if( !curIp.getRoi().equals( prevIp.getRoi() ) ) {
                evtFlags |= ImageProcessorChangeEvent.ROI_CHANGED;
            }
            if( curIp.getMask() != prevIp.getMask() ) {
                evtFlags |= ImageProcessorChangeEvent.MASK_CHANGED;
            }
            //This only guarantees references have changed, no guarantees about properties
            if( prevIp.getLut() !=  curIp.getLut() ) {
                evtFlags |= ImageProcessorChangeEvent.LUT_CHANGED;
            }
        } 

       //Assume a change in width/height member means a change in ImageProcessor as well
        if( prevW != getWidth() || prevH != getHeight()) {
            postToEventQueue( new ImageDimensionsChangedEvent( this, prevW, prevH, getWidth(), getHeight() ) );
            evtFlags |= ImageProcessorChangeEvent.XYDIM_CHANGED;
        }
         
        //Compare for null image changes
        Image curImg = getImage();
        if( (prevImg != null && !prevImg.equals( curImg )) || curImg != prevImg ) {
            System.out.println("The Current Image is: " + curImg );
            postToEventQueue( new ImageChangedEvent( this, prevImg, curImg ) );  //ImagePlus Image Has changed Event
            evtFlags |= ImageProcessorChangeEvent.PIXELS_CHANGED;
        }
        
        if( evtFlags != 0 ) {
            postToEventQueue( new ImageProcessorChangeEvent( this, prevIp, curIp, evtFlags ) );
        }
        
        endEntryContext();
    }
    
     /**
     * Set Current Processor (Override for Event Dispatch: ImageProcessorReferenceChangeEvent)
     * <p>
     * Note: All Overloads wrap to this setProcessor method.  This method will be
     * called for multiple reasons (redundantly and cyclicly).  Because of this, 
     * This Override only dispatches a reference event change event if there is an actual 
     * difference in the ImageProcessor Reference being set.  This avoids extra logic
     * and also guarantees the event is triggered on reference changes only.
     * <p>
     * Note 2:  The super method must still be called however because painting 
     * is linked to it...  as well as title changes.  These are separate events 
     * and will be handled in their respective underlying method calls, but this will
     * need to be debugged due to the possibility of multiple cycled calls... ugh.
     * 
     * @see ImagePlus#setProcessor(java.lang.String, ij.process.ImageProcessor )   
     */
    @Override
    public void setProcessor( String title, ImageProcessor ip ){
        
        setEntryContext();

        ImageProcessor prevIp = getProcessor();//This Also Queues it's own change events and puts the original prevIp in its place in Event
        int prevW = getWidth();
        int prevH = getHeight();
        
        //Since this does painting of windows as well we have to call.......
        super.setProcessor(title, ip);
        
        ImageProcessor curIp = getProcessor(); //This will queue its own change events
        //Detect any event changes
        int evtFlags = 0;
        if( curIp != prevIp ) {
            evtFlags |= ImageProcessorChangeEvent.REF_CHANGED;
        }
        
        //Make sure that we didn't get a null object, otherwise the reference changed is enough
        //Since this comparison is different than that of getProcessor, we have to check all the same and more
        if( prevIp != null && curIp != null ) {
            if( prevIp.getCalibrationTable() != curIp.getCalibrationTable() ) {
                evtFlags |= ImageProcessorChangeEvent.CALIBRATION_CHANGED;
            }
            if( curIp.getLineWidth() != prevIp.getLineWidth() ) {
                evtFlags |= ImageProcessorChangeEvent.LINEWIDTH_CHANGED;
            }
            if( curIp.getClass() != prevIp.getClass() ) {
                evtFlags |= ImageProcessorChangeEvent.TYPE_CHANGED;
                
            }
            //getRoi returns a Rectangle always not same reference
            if( !curIp.getRoi().equals( prevIp.getRoi() ) ) {
                evtFlags |= ImageProcessorChangeEvent.ROI_CHANGED;
            }
            if( curIp.getMask() != prevIp.getMask() ) {
                evtFlags |= ImageProcessorChangeEvent.MASK_CHANGED;
            }
        } 
        
        //Assume a change in member means a change in ImageProcessor as well
        if( prevW != getWidth() || prevH != getHeight()) {
            postToEventQueue( new ImageDimensionsChangedEvent( this, prevW, prevH, getWidth(), getHeight() ) );
            evtFlags |= ImageProcessorChangeEvent.XYDIM_CHANGED;
        }
        
        //If there is an evt flag, post an event to the queue
        if( evtFlags != 0 ) {
            postToEventQueue(new ImageProcessorChangeEvent( this, prevIp, getProcessor(), evtFlags ) );
        }
        
        endEntryContext();
    }
    
     /**
     * Set Current Stack (Override for Event Dispatch: ImageProcessorReferenceChangeEvent, ImageStackDimensionsChangedEvent)
     * <p>
     * Note: Unfortunately this function needs its own wrapping because it sets the dimensions
     *       of the stack without using the getter and setter.
     * 
     * @see ImagePlus#setStack(ij.ImageStack, int, int, int) 
     */
    @Override
    public void setStack(ImageStack newStack, int channels, int slices, int frames){
        
        setEntryContext();

        ImageProcessor prevIp = getProcessor();//This Also Queues it's own change events and puts the original prevIp in its place in Event
        int prevW = getWidth();
        int prevH = getHeight();
        
        //In this case, we just check to see if modifications are done and dispatch early to the EventQueue
        if( channels != this.nChannels || slices != this.nSlices || frames != this.nFrames ) {
            //For organization, JVM should optimize
            int[] prevDimCZT = new int[]{this.nChannels, this.nSlices, this.nFrames};
            int[] curDimCZT = new int[]{channels, slices, frames};
            //We are going to place hold this because later down the pipeline will concatenate this with slice settings
            int[] prevCZT = new int[]{getC(), getZ(), getT() };
            int[] curCZT = new int[]{getC(), getZ(), getT() };
            postToEventQueue(new StackDimensionsChangedEvent(this, prevDimCZT, prevCZT, prevIp, curDimCZT, curCZT, prevIp ));
        }
        
        super.setStack(newStack, channels, slices, frames);
        
        ImageProcessor curIp = getProcessor(); //This will queue its own change events
        //Detect any event changes
        int evtFlags = 0;
        if( curIp != prevIp ) {
            evtFlags |= ImageProcessorChangeEvent.REF_CHANGED;
        }
        
        //Make sure that we didn't get a null object, otherwise the reference changed is enough
        //Since this comparison is different than that of getProcessor, we have to check all the same and more
        if( prevIp != null && curIp != null ) {
            if( prevIp.getCalibrationTable() != curIp.getCalibrationTable() ) {
                evtFlags |= ImageProcessorChangeEvent.CALIBRATION_CHANGED;
            }
            if( curIp.getLineWidth() != prevIp.getLineWidth() ) {
                evtFlags |= ImageProcessorChangeEvent.LINEWIDTH_CHANGED;
            }
            if( curIp.getClass() != prevIp.getClass() ) {
                evtFlags |= ImageProcessorChangeEvent.TYPE_CHANGED;
                
            }
            //getRoi returns a Rectangle always not same reference
            if( !curIp.getRoi().equals( prevIp.getRoi() ) ) {
                evtFlags |= ImageProcessorChangeEvent.ROI_CHANGED;
            }
            if( curIp.getMask() != prevIp.getMask() ) {
                evtFlags |= ImageProcessorChangeEvent.MASK_CHANGED;
            }
        } 
        
        //Assume a change in member means a change in ImageProcessor as well
        if( prevW != getWidth() || prevH != getHeight()) {
            postToEventQueue( new ImageDimensionsChangedEvent( this, prevW, prevH, getWidth(), getHeight() ) );
            evtFlags |= ImageProcessorChangeEvent.XYDIM_CHANGED;
        }
        
        //If there is an evt flag, post an event to the queue
        if( evtFlags != 0 ) {
            postToEventQueue(new ImageProcessorChangeEvent( this, prevIp, curIp, evtFlags ) );
        }
        
        endEntryContext();
    }
    
    /**
     * Update The FileInfo object of the ImagePlus (Override for Event Dispatch: FileInfoChangedEvent )
     * <p>
     * Note:  dispatches an event regardless of difference in the object and what was already stored
     * 
     * @see ImagePlus#setFileInfo(ij.io.FileInfo)   
     */
    @Override
    public void setFileInfo( FileInfo fi ) {
        
        setEntryContext();
        
        FileInfo prevFi = getOriginalFileInfo();  //This returns the internal Object
        
        super.setFileInfo(fi);
        
        //Send
        postToEventQueue( new FileInfoChangedEvent(this, prevFi, getOriginalFileInfo() ) );
        
        endEntryContext();
        
    }
    
     /**
     * Trims the Processor by removing the snapShotPixels (Override for Event Dispatch: ImageProcessorChangeEvent )
     * <p>
     * Note:  dispatches an event regardless of difference in the object and what was already stored
     * 
     * @see ImagePlus#trimProcessor()
     */
    @Override
    public void trimProcessor( ) {
        
        setEntryContext();
        
        ImageProcessor prevIp = this.ip; //Access protected member avoid getProcessor() events, since actual method doesn't set the processor
        Object prevSnapPix;
        if( prevIp != null ) {
            prevSnapPix = prevIp.getSnapshotPixels(); 
        } else {
            prevSnapPix = null;
        }
        
        super.trimProcessor();
        
        ImageProcessor curIp = this.ip;
        Object curSnapPix;
        if( curIp != null ) {
            curSnapPix = curIp.getSnapshotPixels();
        } else {
            curSnapPix = null;
        }
        
        //If there's a difference Dispatch an event
        if( curSnapPix != prevSnapPix ) {
            postToEventQueue( new ImageProcessorChangeEvent(this, prevIp, curIp, ImageProcessorChangeEvent.SNAPSHOT_CHANGED  ) );
        }
        
        endEntryContext();
        
    }
    
    /**
     * Signals a change in the Roi since a residual mask might apply or reset the ImageProcessor (Override for Event Dispatch: ImageProcessorChangeEvent )
     * <p>
     * May change Mask or Roi of ImageProcessor based on ImagePlus state
     * 
     * @see ImagePlus#trimProcessor()
     */
    @Override
    public ImageProcessor getMask( ) {
        
        setEntryContext();
        
        ImageProcessor prevIp = this.ip; //Access protected member avoid getProcessor() events, since actual method doesn't set the processor
        ImageProcessor prevMask;
        Rectangle prevRoi;
        if( prevIp != null ) {
            prevMask = prevIp.getMask(); 
            prevRoi = prevIp.getRoi();
        } else {
            prevMask = null;
            prevRoi = null;
        }
        
        ImageProcessor mask = super.getMask();
        
        ImageProcessor curIp = this.ip;
        ImageProcessor curMask;
        Rectangle curRoi;
        if( curIp != null ) {
            curMask = prevIp.getMask(); 
            curRoi = prevIp.getRoi();
        } else {
            curMask = null;
            curRoi = null;            
        }
        
        int evtFlags = 0;
        //If Mask is the same
        if( curMask != null && !curMask.equals( prevMask ) ) {
            evtFlags |= ImageProcessorChangeEvent.MASK_CHANGED;
        }
        if( curRoi != null && !curRoi.equals( prevRoi ) ) {
            evtFlags |= ImageProcessorChangeEvent.ROI_CHANGED;
        }
        //If there's a difference Dispatch an event
        if( evtFlags != 0 ) {
            postToEventQueue( new ImageProcessorChangeEvent(this, prevIp, curIp, evtFlags  ) );
        }
        
        endEntryContext();
        
        return mask;
        
    }
    
    /**
     * Update The Image stored in the ImagePlus and potentially the imageProcessor (Override for Event Dispatch: ImageProcessorChangeEvent, ImageChangeEvent)
     * <p>
     * Note: This method essentially will create new Pixels in the img and ImageProcessor.img
     * members if the other methods have changed properties in the ImageProcessor.  This method
     * will throw two events potentially, the ImageChangeEvent and ImageProcessorChangeEvent with pixels changed event.
     * 
     * @see ImagePlus#updateImage(java.lang.String, ij.process.ImageProcessor )   
     */
    @Override
    public void updateImage() {
        
        setEntryContext();
        
        ImageProcessor prevIp = ip;  //Get protected imagePlus to ensure no changing imageprocessor in get method
        Image prevImg = this.img;  //Get protected image as well
        
        super.updateImage();
        
        //Compare for null image changes
        if( (prevImg != null && !prevImg.equals( this.img )) || this.img != prevImg ) {
            postToEventQueue( new ImageProcessorChangeEvent( this, prevIp, ip, ImageProcessorChangeEvent.PIXELS_CHANGED ));
            postToEventQueue( new ImageChangedEvent( this, prevImg, this.img ) );
        }
        
        endEntryContext();
    }
    
    /**
     * Change The ImageType of the ImagePlus (Override for Event Dispatch: ImageTypeChangedEvent)
     * <p>
     * Note: This method does not necessarily modify the ImageProcessor Immediately and unfortunately.  Therefore, this event dispatches 
     *       an ImageType change, due to the fact that this behavior may be expected in any implementing code.
     * 
     * @see ImagePlus#setType(int)    
     */
    @Override
    public void setType( int type ) {
        
        setEntryContext();
        
        int prevType = getType();
        
        super.setType( type );
        
        int curType = getType();
        //Compare for null image changes
        if( prevType != curType ) {
            postToEventQueue( new ImageTypeChangedEvent( this, prevType, curType ));
        }
        
        endEntryContext();
    }
    
     /**
     * Sets the Slice of the ImagePlus and window (Override for Event Dispatch: ImageChangedEvent )
     * <p>
     * Note: While most of the methods handle Event Dispatch, the image stored in the ImagePlus may be manipulated
     *       directly and therefore this also must test for the ImageChangedEvent
     * 
     * @see ImagePlus#setSlice(int) 
     */
    @Override
    public void setSlice( int slice ){
        
        setEntryContext();

        Image prevImg = this.img;

        super.setSlice( slice );
        
        Image curImg = this.img;
        
        if( (curImg != null && !curImg.equals(prevImg)) || (prevImg != null && !prevImg.equals( curImg ) ) ) {
            postToEventQueue(new ImageChangedEvent( this, prevImg, curImg ) );
        }
        
        endEntryContext();
    }
    
    
     /**
     * Get Current Processor (Override for Event Dispatch: ImageProcessorChangeEvent)
     * <p>
     * Note: This function, although implied to simply get the Processor, may actually
     *       modify a null or conflicting type (type was changed internally but no in the processor)
     *       Processor and return a new reference and Processor.  For this reason, this
     *       Method also dispatches an ImageProcessorChangeEvent that will update the currentProcessor
     *       In the event Queue.
     * 
     * @see ImagePlus#getProcessor()   
     */
    @Override
    public ImageProcessor getProcessor() {
        
        setEntryContext();
        
        ImageProcessor prevIp = ip;  //We Pull the protected IP for this comparison, but compare it with the getter output
        ImageProcessor curIp = super.getProcessor();
        
        //Detect any event changes
        int evtFlags = 0;
        
        if( prevIp != curIp ) {
            evtFlags |= ImageProcessorChangeEvent.REF_CHANGED;
        }
        //Make sure that we didn't get a null object, otherwise the reference changed is enough
        if( prevIp != null && curIp != null ) {
            if( prevIp.getCalibrationTable() != curIp.getCalibrationTable() ) {
                evtFlags |= ImageProcessorChangeEvent.CALIBRATION_CHANGED;
            }
            if( curIp.getLineWidth() != prevIp.getLineWidth() ) {
                evtFlags |= ImageProcessorChangeEvent.LINEWIDTH_CHANGED;
            }
            if( curIp.getClass() != prevIp.getClass() ) {
                evtFlags |= ImageProcessorChangeEvent.TYPE_CHANGED;
            }
            //getRoi returns a Rectangle always not same reference
            if( !curIp.getRoi().equals( prevIp.getRoi() ) ) {
                evtFlags |= ImageProcessorChangeEvent.ROI_CHANGED;
            }
            if( curIp.getMask() != prevIp.getMask() ) {
                evtFlags |= ImageProcessorChangeEvent.MASK_CHANGED;
            }
        } 

        //If there was a changed Event Flag, post it to queue for concatenation
        if( evtFlags!= 0 ) {
            postToEventQueue( new ImageProcessorChangeEvent( this, prevIp, curIp, evtFlags ) );
        }
        
        endEntryContext();  //Dispatch Event Queue if this was the entry context
        
        return curIp;  //Post the curIp we have been testing 
    }
    
     /**
     * Set Current Overlay (Override for Event Dispatch: TitleChangedEvent )
     * <p>
     * Note:  Several Methods May Trigger This Underlying function.  The Title of 
     *       The image is associated to many other ImageJ Window references and can
     *       be Used Accordingly.  The TitleChangedEvent Is truly triggered by a difference
     *       in titles.  Subscribers Can be assured of this.
     * 
     * @see ImagePlus#setTitle(java.lang.String)   
     */
    @Override
    public void setTitle(String title){

        setEntryContext();
        
        String prevTitle = getTitle();
        super.setTitle(title);
        
        //Only Queue if there's an acutal Change
        if( !prevTitle.equals(getTitle()) ) {
            postToEventQueue( new TitleChangedEvent( this, prevTitle, getTitle() ) );
        }
        
        endEntryContext();  //If this was Entry Context, Will Send All Queued Events
    }
    
     /**
     * Set Current Roi (Override for Event Dispatch: RoiChangedEvent and ImageProcessorChangeEvent )
     * 
     * @see ImagePlus#setRoi(ij.gui.Roi, boolean)  
     */
    @Override
    public void setRoi(Roi newRoi, boolean updateDisplay ){

        setEntryContext();
        
        Roi prevRoi = getRoi();
        ImageProcessor prevIp = getProcessor();
        
        super.setRoi( newRoi, updateDisplay );
        
        Roi curRoi = getRoi();
        ImageProcessor curIp = getProcessor();
        
        //Only Queue if there's an actual Change
        if( (curRoi != null && !curRoi.equals(prevRoi)) || (prevRoi != null && !prevRoi.equals(curRoi)) ) {
            postToEventQueue( new ImageProcessorChangeEvent( this, prevIp, curIp, ImageProcessorChangeEvent.ROI_CHANGED ) );
            postToEventQueue( new RoiChangeEvent(this, prevRoi, curRoi ) );
        }
        
        endEntryContext();  //If this was Entry Context, Will Send All Queued Events
    }
    
     /**
     * Deletes the Current Roi in the ImagePlus and ImageProcessor(Override for Event Dispatch: RoiChangedEvent and ImageProcessorChangeEvent )
     * 
     * @see ImagePlus#deleteRoi() 
     */
    @Override
    public void deleteRoi( ){

        setEntryContext();
        
        Roi prevRoi = getRoi();
        ImageProcessor prevIp = getProcessor();
        
        super.deleteRoi( );
        
        //Only Queue if there's an acutal Change to null
        if( prevRoi != null ) {
            postToEventQueue( new ImageProcessorChangeEvent( this, prevIp, prevIp, ImageProcessorChangeEvent.ROI_CHANGED ) );
            postToEventQueue( new RoiChangeEvent(this, prevRoi, null ) );
        }
        
        endEntryContext();  //If this was Entry Context, Will Send All Queued Events
    }
    
     /**
     * Creates a New Roi in the ImagePlus (Override for Event Dispatch: RoiChangedEvent )
     * 
     * @see ImagePlus#createNewRoi(int, int) 
     */
    @Override
    public void createNewRoi( int sx, int sy ){

        setEntryContext();
        
        Roi prevRoi = getRoi();
        
        super.deleteRoi( );
        
        Roi curRoi = getRoi();
        
        //Only Queue if there's an actual Change
        if( (curRoi != null && !curRoi.equals(prevRoi)) || (prevRoi != null && !prevRoi.equals(curRoi)) ) {
            postToEventQueue( new RoiChangeEvent(this, prevRoi, curRoi ) );
        }
        
        endEntryContext();  //If this was Entry Context, Will Send All Queued Events
    }
    
     /**
     * Restores the previous Roi to the ImagePlus (Override for Event Dispatch: RoiChangedEvent )
     * <p>
     * Note:  This does not apply a change to the ImageProcessor, but does change the Roi on the imageplus and update the canvas.
     * 
     * @see ImagePlus#restoreRoi() 
     */
    @Override
    public void restoreRoi( ){

        setEntryContext();
        
        Roi prevRoi = getRoi();
        
        super.restoreRoi();
        
        Roi curRoi = getRoi();
        
        //Only Queue if there's an actual Change
        if( (curRoi != null && !curRoi.equals(prevRoi)) || (prevRoi != null && !prevRoi.equals(curRoi)) ) {
            postToEventQueue( new RoiChangeEvent(this, prevRoi, curRoi ) );
        }
        
        endEntryContext();  //If this was Entry Context, Will Send All Queued Events
    }
    
     /**
     * Flushes the ImageProcessor, Roi, Image, and stack to null for the ImagePlus 
     * (Override for Event Dispatch: RoiChangedEvent, ImageChangedEvent, ImageProcessorChangeEvent, OverlayChangedEvent, StackDimensionChangeEvent )
     * <p>
     * Note:  This does not apply a change to the ImageProcessor, but does change the Roi on the imageplus and update the canvas.
     * 
     * @see ImagePlus#flush() 
     */
    @Override
    public void flush( ){

        setEntryContext();
        
        Roi prevRoi = getRoi();
        Overlay prevOv = getOverlay();
        ImageProcessor prevIp = this.ip;
        int[] prevDimCZT = new int[]{ getNChannels(), this.nSlices, this.nFrames }; //Verify
        int prevC = getC(), prevZ = getZ(), prevT = getT();
        Image prevImg = this.img;
        
        super.flush();
        
        int[] curDimCZT = new int[]{ getNChannels(), this.nSlices, this.nFrames }; //Verify the dimensions in method for NChannels, so that they are revised
        int curC = 1, curZ = 1, curT = 1; //Set everything to 1

        //Compare Against null values
        if( prevRoi != null ) {
            postToEventQueue( new RoiChangeEvent(this, prevRoi, null ) );
        }
        if( prevOv != null ) {
            postToEventQueue( new OverlayChangedEvent(this, prevOv, null ) );
        }
        if( prevIp != null ) {
            postToEventQueue( new ImageProcessorChangeEvent(this, prevIp, null, ImageProcessorChangeEvent.REF_CHANGED ) );
        }
        if( prevDimCZT[0] != 1 || prevDimCZT[1] != 1 || prevDimCZT[2] != 1 ) {
                //For organization, JVM should optimize
                int[] prevCZT = new int[]{prevC, prevZ, prevT};
                int[] curCZT = new int[]{curC, curZ, curT};
                postToEventQueue(new StackDimensionsChangedEvent(this, prevDimCZT, prevCZT, prevIp, curDimCZT, curCZT, null));
        }
        if( prevImg != null ) {
            postToEventQueue( new ImageChangedEvent(this, prevImg, null ) );
        }
        
        endEntryContext();  //If this was Entry Context, Will Send All Queued Events
    }
    
     /**
     * Copies the current Roi on the ImagePlus. (Override for Event Dispatch: RoiChangedEvent, ImageProcessorChangeEvent )
     * <p>
     * Note:  This may set the Roi to null if it is not an Area Roi, and only 
     * modifies the ImageProcessor if cut is <code>true</code>
     * 
     * @see ImagePlus#copy(boolean) 
     */
    @Override
    public void copy( boolean cut ) {
        
        setEntryContext();
        
        ImageProcessor prevIp = getProcessor();  //Since this will involve the current Roi make sure its applied
        Roi prevRoi = getRoi();
        
        super.copy( cut );
        
        ImageProcessor curIp = this.ip;  //Avoid another call to getProcessor() since should be the same
        
        int evtFlags = 0;
        //If there's an Roi change, do that
        if( getRoi() == null && prevRoi != null ){
            postToEventQueue( new RoiChangeEvent( this, prevRoi, null ) );
            evtFlags |= ImageProcessorChangeEvent.ROI_CHANGED;
        }
        //Any cut means pixels changed
        if( cut ) {
            evtFlags |= ImageProcessorChangeEvent.PIXELS_CHANGED;
            evtFlags |= ImageProcessorChangeEvent.SNAPSHOT_CHANGED;
        }
        
        if( evtFlags != 0 ) {
            postToEventQueue( new ImageProcessorChangeEvent( this, prevIp, curIp, evtFlags) );
        }
        
        endEntryContext();
        
    }
    
     /**
     * Pastes the current Roi on the ImagePlus. (Override for Event Dispatch: RoiChangedEvent, ImageProcessorChangeEvent )
     * <p>
     * Note: This Will produce a centered Roi if there is a discrepancy in canvas and Paste Rois.
     * Addtionally, we assume Pixels must have changed, without verifying actual Pixel changes.
     * 
     * @see ImagePlus#paste() 
     */
    @Override
    public void paste( ) {
        
        setEntryContext();
        
        //This is a public member... Which is not good, but we'll use it for less computation
        this.changes = false; //reset
        
        ImageProcessor prevIp = getProcessor();  //Since this will involve the current Roi make sure its applied
        Roi prevRoi = getRoi();
        
        super.paste( );
        
        //Make sure paste actually occurred
        if (this.changes == true) {

            ImageProcessor curIp = getProcessor();  //Due to chaos of Roi changes, call for good measure
            Roi curRoi = getRoi();
            int evtFlags = 0;
            //If there's an Roi change, do that
            if ((prevRoi != null && !prevRoi.equals(curRoi)) || (curRoi != null && !curRoi.equals(prevRoi))) {
                postToEventQueue(new RoiChangeEvent(this, prevRoi, curRoi));
                evtFlags |= ImageProcessorChangeEvent.ROI_CHANGED;
            }

            //Assume pixels have to change
            evtFlags |= ImageProcessorChangeEvent.PIXELS_CHANGED;
            evtFlags |= ImageProcessorChangeEvent.SNAPSHOT_CHANGED;
            postToEventQueue(new ImageProcessorChangeEvent(this, prevIp, curIp, evtFlags));

        }
        
        endEntryContext();
        
    }
    
     /**
     * Sets the Display Range Values (min and max) of the imageProcessor (Override for Event Dispatch: ImageProcessorChangeEvent )
     * <p>
     * Note:  This has to do with display mostly.  Getters: ImageProcessor.getMin(), ImageProcessor.getMax()
     * 
     * @see ImagePlus#setDisplayRange(double, double)  
     */
    @Override
    public void setDisplayRange(double min, double max) {

        setEntryContext();
        
        ImageProcessor prevIp = getProcessor();  //This is because the getters from ImagePlus don't verify there's an imagePlus, so We'll set one
        double prevMin = getDisplayRangeMin(), prevMax = getDisplayRangeMax();
        
        super.setDisplayRange( min, max );
        
        double curMin = getDisplayRangeMin(), curMax = getDisplayRangeMax();
        
        //Only Queue if there's an actual Change
        if( curMin != prevMin || curMax != prevMax ) {
            postToEventQueue( new ImageProcessorChangeEvent(this, prevIp, prevIp, ImageProcessorChangeEvent.DISPLAYRANGE_CHANGED ) );
        }
        
        endEntryContext();  //If this was Entry Context, Will Send All Queued Events
    }
    
     /**
     * Sets the Display Range Values (min and max) for Specific Channels of the imageProcessor (Override for Event Dispatch: ImageProcessorChangeEvent )
     * <p>
     * Note:  This has to do with display mostly.  Getters: ImageProcessor.getMin(), ImageProcessor.getMax()
     * 
     * @see ImagePlus#setDisplayRange(double, double, int)   
     */
    @Override
    public void setDisplayRange(double min, double max, int channels) {

        setEntryContext();
        
        ImageProcessor prevIp = getProcessor();  //This is because the getters from ImagePlus don't verify there's an imagePlus, so We'll set one
        double prevMin = getDisplayRangeMin(), prevMax = getDisplayRangeMax();
        
        super.setDisplayRange( min, max, channels );
        
        double curMin = getDisplayRangeMin(), curMax = getDisplayRangeMax();
        
        //Only Queue if there's an actual Change
        if( curMin != prevMin || curMax != prevMax ) {
            postToEventQueue( new ImageProcessorChangeEvent(this, prevIp, prevIp, ImageProcessorChangeEvent.DISPLAYRANGE_CHANGED ) );
        }
        
        endEntryContext();  //If this was Entry Context, Will Send All Queued Events
    }
    
     /**
     * Resets the Display Range Values (min and max) of the imageProcessor (Override for Event Dispatch: ImageProcessorChangeEvent )
     * <p>
     * Note:  This has to do with display mostly.  Getters: ImageProcessor.getMin(), ImageProcessor.getMax()
     * 
     * @see ImagePlus#resetDisplayRange()   
     */
    @Override
    public void resetDisplayRange() {

        setEntryContext();
        
        ImageProcessor prevIp = getProcessor();  //This is because the getters from ImagePlus don't verify there's an imagePlus, so We'll set one
        double prevMin = getDisplayRangeMin(), prevMax = getDisplayRangeMax();
        
        super.resetDisplayRange( );
        
        double curMin = getDisplayRangeMin(), curMax = getDisplayRangeMax();
        
        //Only Queue if there's an actual Change
        if( curMin != prevMin || curMax != prevMax ) {
            postToEventQueue( new ImageProcessorChangeEvent(this, prevIp, prevIp, ImageProcessorChangeEvent.DISPLAYRANGE_CHANGED ) );
        }
        
        endEntryContext();  //If this was Entry Context, Will Send All Queued Events
    }
    
    //Part of Later ImageJ source that is not yet part of Micromanager Source
     /**
     * Set Current LUT (Override for Event Dispatch)
     * <p>
     * Note:  This method will not change LUT if an ImageProcessor is null or
     *        the LUT is null.  The Code for the Overridden Method redundantly 
     *        resets the ImageProcessor, even though the reference is the same.
     *        The Event Triggered Specifically references the LUTs only. Accessing 
     *        the ImageProcessor is the burden of the stored originator.getProcessor()
     * @param lut
     * @see ImagePlus#setOverlay(ij.gui.Overlay)  
     */
    /*@Override
    public void setLut(LUT lut){
        //Only Called if a meaningful change can be made (no null LUT or the processor is null
        if (lut != null && getProcessor() != null) {
            LUTChangedEvent evt = new LUTChangedEvent(this, getProcessor().getLut(), lut);
            super.setLut(lut);

            //Post The previous and current Set Ov as an Event
            bus_.post(new OverlayChangedEvent(this, prevOv, ov));
        }
    }*/
    
}
