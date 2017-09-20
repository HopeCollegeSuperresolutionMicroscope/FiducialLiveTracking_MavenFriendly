/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.ImageJmodifieds;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import edu.hope.superresolution.genericstructures.MasterSpecificEvent;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Map;
import org.testng.Assert;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 *
 * Unit tests For Event Bus Dispatch Of Methods
 * 
 * @author Desig
 */
public class PropertyNotifiyingImagePlusNGTest {
    
    //Test Class For Subscription to the Event Bus that fails for all non-expected methods
    //Override For expected behaviors
    private class EvtRecieverEval implements IPropertyNotifierImagePlusEventListener {

        EvtRecieverEval( ) {
            
        }
        
        @Override
        public boolean onImageProcessorChangeEvent(PropertyNotifyingImagePlus.ImageProcessorChangeEvent evt) {
           
            fail( "Recieved ImageProcessor Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onCalibrationChangedEvent(PropertyNotifyingImagePlus.CalibrationChangedEvent evt) {
            fail( "Recieved Calibration Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onFileInfoChangeEvent(PropertyNotifyingImagePlus.FileInfoChangedEvent evt) {
            fail( "Recieved FileInfo Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onImageChangedEvent(PropertyNotifyingImagePlus.ImageChangedEvent evt) {
            fail( "Recieved Image Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onImageDimensionsChangedEvent(PropertyNotifyingImagePlus.ImageDimensionsChangedEvent evt) {
            fail( "Recieved ImageDimension Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onImageTypeChangedEvent(PropertyNotifyingImagePlus.ImageTypeChangedEvent evt) {
            fail( "Recieved ImageType Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onOverlayChangedEvent(PropertyNotifyingImagePlus.OverlayChangedEvent evt) {
           fail( "Recieved Overlay Change Event when none was expected");
           return false;
        }

        @Override
        public boolean onOverlayVisibilityChangedEvent(PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt) {
            fail( "Recieved OverlayVisibility Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onRoiChangeEvent(PropertyNotifyingImagePlus.RoiChangeEvent evt) {
            fail( "Recieved Roi Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onStackDimensionsChangedEvent(PropertyNotifyingImagePlus.StackDimensionsChangedEvent evt) {
            fail( "Recieved Stack Dimension Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onStackPositionChangedEvent(PropertyNotifyingImagePlus.StackPositionChangedEvent evt) {
            fail( "Recieved Stack Position Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onTitleChangedEvent(PropertyNotifyingImagePlus.TitleChangedEvent evt) {
            fail( "Recieved Title Change Event when none was expected");
            return false;
        }

        @Override
        public boolean onDummyEvent(PropertyNotifyingImagePlus.DummyEvent evt) {
            fail( "Recieved Dummy Event when none was expected");
            return false;
        }
          
    }
    
    //Static Constants used for Event Appending ( first, second, third
    public static final String STR_VAL_FIRST = "FIRST";
    public static final String STR_VAL_SECOND = "SECOND";
    public static final String STR_VAL_THIRD = "THIRD";
    public static final int INT_VAL_FIRST = 100;
    public static final int INT_VAL_SECOND = 200;
    public static final int INT_VAL_THIRD = 300;
    public static final int EVTFLAG_VAL_FIRST = PropertyNotifyingImagePlus.ImageProcessorChangeEvent.CALIBRATION_CHANGED 
                                                       | PropertyNotifyingImagePlus.ImageProcessorChangeEvent.ROI_CHANGED | PropertyNotifyingImagePlus.ImageProcessorChangeEvent.STACKPOSTION_CHANGED;
    public static final int EVTFLAG_VAL_SECOND = PropertyNotifyingImagePlus.ImageProcessorChangeEvent.REF_CHANGED
                                                       | PropertyNotifyingImagePlus.ImageProcessorChangeEvent.MASK_CHANGED | PropertyNotifyingImagePlus.ImageProcessorChangeEvent.PIXELS_CHANGED;
    public static final int EVTFLAG_VAL_THIRD = PropertyNotifyingImagePlus.ImageProcessorChangeEvent.LUT_CHANGED
                                                       | PropertyNotifyingImagePlus.ImageProcessorChangeEvent.TYPE_CHANGED | PropertyNotifyingImagePlus.ImageProcessorChangeEvent.XYDIM_CHANGED;    
    
    private static String constKey_;  //Key for selecting constructors
    private PropertyNotifyingImagePlus propImPlus_ = null;
    
    public PropertyNotifiyingImagePlusNGTest() {
    }

     /**
     * Called To Setup Constructors.  This ensures a fresh blank instance so that subscribers do not stay registered on failed tests
     * Parameter Selects the constructor to save for Dummy Tests (only) the testPropImplus
     * <p>
     * propImplus_ can be overwritten for specific instances in test cases.
     * <pre>
     * 1 - PropertyNotifyingImagePlus( String, Image)
     * 2 - PropertyNotifyingImagePlus( String, ImageProcessor )
     * 3 - PropertyNotifyingImagePlus( String, ImageStack )
     * </pre>
     */
    @Parameters( { "selectConstructorKey"} )
    @BeforeClass
    public static void setUpClass( @Optional("1") String constKey ) throws Exception {
        constKey_ = constKey;
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod( ) throws Exception {
    }

    /**
     * 
     * @throws Exception 
     */
    @AfterMethod
    public void tearDownMethod() throws Exception {
    }    
    
    /**
     * Test Constructors - Add any other implementations to test
     */
    @Test( groups = {"init"} )
    public void testConstructors(   ) {
        
        System.out.println("Testing Constructors" );
        

        PropertyNotifyingImagePlus imgInstance = null, shortProcInstance = null, imageStackInstance = null;
        try {
            imgInstance = createTestImageConstructedInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Image Constructor Exception: " + ex.toString());
        }
        ImageProcessor basicProc = new ShortProcessor(200, 200);
        try {
            shortProcInstance = createTestShortProcConstructedInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("ShortProcessor Constructor Exception: " + ex.toString());
        }
        System.out.println("Create ImageStack Instance " );
        ImageStack iStack =  ImageStack.create(200, 200, 5, 16);
        try {
            imageStackInstance = createTestImageStackConstructedInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("ImageStack Constructor Exception: " + ex.toString());
        }

    }
    
    //Private Methods To Produce simple PropertyNotifyingImagePlus Instance
    /**
     * Method for Producing An empty PropertyNotifyingImagePlus that should correlate
     * to selectConstructorKey Parameter in the a Test.  See @BeforeMethod method for the options.
     * @param selectKey
     * @return 
     */
    private PropertyNotifyingImagePlus produceTestImagePlusInstance( String selectKey) {
            PropertyNotifyingImagePlus implus = null;
            switch( Integer.parseInt(selectKey) ) {
            case 1: 
                implus = createTestImageConstructedInstance();
                break;
            case 2: 
                implus = createTestShortProcConstructedInstance();
                break;
            case 3:
                implus = createTestImageStackConstructedInstance();
                break;
            default:
                System.out.println( "unverifiedKey");
                Assert.fail("Selection Key does not match any storage protocol: no PropertyNotifiyingImagePlus to use for other tests  ");
        }
            
        return implus;
    }
    
    private PropertyNotifyingImagePlus createTestImageConstructedInstance( ) {
        return new PropertyNotifyingImagePlus("All White Image Mock ImagePlus", new BufferedImage(200, 200, BufferedImage.TYPE_BYTE_GRAY));
    }

    private PropertyNotifyingImagePlus createTestShortProcConstructedInstance( ) {
        ImageProcessor basicProc = new ShortProcessor(200, 200);
        return new PropertyNotifyingImagePlus("ImageProcessor Black Mock ImagePlus", basicProc);
    }
    
    private PropertyNotifyingImagePlus createTestImageStackConstructedInstance() {
        ImageStack iStack = ImageStack.create(200, 200, 5, 16);
        return new PropertyNotifyingImagePlus("Black ImageStack Mock ImagePlus", iStack);
    }
    
    /**
     * Test of RegisterToEventBus method, of class PropertyNotifyingImagePlus.
     * 
     * Should only fail if there is an issue with the testInstance
     */
    @Test( dependsOnMethods = {"testConstructors"}, groups ={ "init" } )
    public void testRegisterToEventBus( ) {
        propImPlus_ = produceTestImagePlusInstance( constKey_ ); //Make Specific Blank ImPlus
        
        EvtRecieverEval reciever = new EvtRecieverEval();
        System.out.println("RegisterToEventBus");
        propImPlus_.RegisterToEventBus(reciever);
        //If there was no failure at this point, unregister the reciever
        propImPlus_.UnregisterFromEventBus(reciever);
    }

    /**
     * Produces a dummy event for each PropertyNotifyingImagePlus.event.  The Events follow 
     * Constant values as defined in the class with _FIRST being the previous value and _SECOND 
     * being the current.  If appending another mock event, use _THIRD.  This class produces the first order.
     * @return - { { MasterSpecificEvent subclass } }
     */
    @DataProvider( name = "DummyFirstEventTest" )
    public Object[][] produceSingleTestEvents() {
        
        propImPlus_ = produceTestImagePlusInstance( constKey_ ); //Make Specific Blank ImPlus
        
        MasterSpecificEvent calEvt = produceCalibrationEventDummy( STR_VAL_FIRST, STR_VAL_SECOND );
        
        //File Info Change Event
        MasterSpecificEvent fInfoEvt = produceFileInfoEventDummy( STR_VAL_FIRST, STR_VAL_SECOND );
        
        //ImageChanged in ImagePlus Event
        MasterSpecificEvent imgEvt = produceImageChangedEventDummy( INT_VAL_FIRST, INT_VAL_SECOND );
        
        //ImageDimensions Changed Event
        MasterSpecificEvent dimEvt =produceImageDimensionsEventDummy( INT_VAL_FIRST, INT_VAL_SECOND);
        
        //ImageProcessorChange Event
        MasterSpecificEvent ipEvt = produceImageProcessorEventDummy( INT_VAL_FIRST, INT_VAL_SECOND, EVTFLAG_VAL_FIRST );
        
        //ImageType Changed Event
        MasterSpecificEvent typeEvt = produceImageTypeEventDummy( INT_VAL_FIRST, INT_VAL_SECOND  );       
        
        //Overlay Changed Event
        MasterSpecificEvent ovEvt = produceOverlayChangeEventDummy( INT_VAL_FIRST, INT_VAL_SECOND );       
        
        //Overlay Visibility Changed Event     
        MasterSpecificEvent ovVisEvt = produceOverlayVisibilityEventDummy( false, true );
        
        //Roi Changed Event
        MasterSpecificEvent roiEvt = produceRoiChangeEventDummy( INT_VAL_FIRST, INT_VAL_SECOND );
        
        //Stack Dimensions and Position Changed Event
        MasterSpecificEvent stackDimEvt = produceStackDimensionEventDummy( INT_VAL_FIRST, INT_VAL_SECOND );
        //Position Change
        MasterSpecificEvent stackPosEvt = produceStackPositionEventDummy( INT_VAL_FIRST, INT_VAL_SECOND );
        //Title Change
        MasterSpecificEvent titleEvt = produceTitleChangeEventDummy( STR_VAL_FIRST, STR_VAL_SECOND );
        
        return new Object[][]{ {calEvt}, {fInfoEvt}, {imgEvt}, {dimEvt}, {ipEvt}, {typeEvt}, {ovEvt}, {ovVisEvt}, {roiEvt}, {stackDimEvt}, {stackPosEvt}, {titleEvt} };
        
    }
    
    
    /**
     * Test of postToEventQueue method, with specfic mock class from each PropertyNotifyingImagePlus.Event  
 Ensures that Event Queue is added to and that the values that were appended have not changed from the expectation
 as set up in the dataProvider.
     */
    @Test(dataProvider = "DummyFirstEventTest",  dependsOnGroups = {"init"} )
    public void testPostToEventQueue_SingleEvent( MasterSpecificEvent specEvent ) {
    
        System.out.println("postToEventQueue_SingleEvent for Event Class: " + specEvent.getClass().getName() );
        propImPlus_.postToEventQueue( specEvent );
        
        //Note, Registration will be evaluated by the Test Object
        Map< String, MasterSpecificEvent> evtQueue = propImPlus_.getCopyOfEventQueue();
        
        //We Call the Test Contexts Utility to Clear the Event Queue
        propImPlus_.entryContextsUtilityTest();
        
        //Make sure it was appended and the reference is the same
        Assert.assertEquals(evtQueue.size(), 1);
        //Get the reference from the queue (only first)
        MasterSpecificEvent queuedEvt = null;
        for( MasterSpecificEvent evt : evtQueue.values() ) {
            queuedEvt = evt;
            if( queuedEvt != null ) {
                break;
            }
        }
        Assert.assertTrue( queuedEvt.sameOriginator(propImPlus_), "The Originator was not detected to the be same as the stored reference.");
        
        if( queuedEvt instanceof PropertyNotifyingImagePlus.CalibrationChangedEvent ) {
            PropertyNotifyingImagePlus.CalibrationChangedEvent upCast = (PropertyNotifyingImagePlus.CalibrationChangedEvent) specEvent;
            previousEventStateAssert( upCast.getPreviousCalibration().getUnit(), STR_VAL_FIRST);
            currentEventStateAssert( upCast.getCurrentCalibration().getUnit(), STR_VAL_SECOND);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.FileInfoChangedEvent ) {
            PropertyNotifyingImagePlus.FileInfoChangedEvent upCast = (PropertyNotifyingImagePlus.FileInfoChangedEvent) specEvent;
            previousEventStateAssert( upCast.getPreviousFileInfo().directory, STR_VAL_FIRST);
            currentEventStateAssert( upCast.getCurrentFileInfo().directory, STR_VAL_SECOND);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.ImageChangedEvent ) {
            PropertyNotifyingImagePlus.ImageChangedEvent upCast = (PropertyNotifyingImagePlus.ImageChangedEvent) specEvent;
            previousEventStateAssert( ((BufferedImage)upCast.getPreviousImage()).getWidth(), INT_VAL_FIRST);
            previousEventStateAssert( ((BufferedImage)upCast.getPreviousImage()).getHeight(), INT_VAL_FIRST);
            currentEventStateAssert( ((BufferedImage)upCast.getCurrentImage()).getHeight(), INT_VAL_SECOND);
            currentEventStateAssert( ((BufferedImage)upCast.getCurrentImage()).getWidth(), INT_VAL_SECOND);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.ImageDimensionsChangedEvent ) {
            PropertyNotifyingImagePlus.ImageDimensionsChangedEvent upCast = (PropertyNotifyingImagePlus.ImageDimensionsChangedEvent) specEvent;
            previousEventStateAssert( upCast.getPreviousWidth(), INT_VAL_FIRST);
            previousEventStateAssert( upCast.getPreviousHeight(), INT_VAL_FIRST);
            currentEventStateAssert( upCast.getCurrentWidth(), INT_VAL_SECOND);
            currentEventStateAssert( upCast.getCurrentHeight(), INT_VAL_SECOND);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.ImageProcessorChangeEvent ) {
            PropertyNotifyingImagePlus.ImageProcessorChangeEvent upCast = (PropertyNotifyingImagePlus.ImageProcessorChangeEvent) specEvent;
            labeledEventStateAssert( upCast.getEventFlags(), EVTFLAG_VAL_FIRST, "ImageProcessor Event Flags");
            previousEventStateAssert( upCast.getPreviousImageProcessorReference().getWidth(), INT_VAL_FIRST);
            previousEventStateAssert( upCast.getPreviousImageProcessorReference().getHeight(), INT_VAL_FIRST);
            currentEventStateAssert( upCast.getCurrentImageProcessorReference().getWidth(), INT_VAL_SECOND);
            currentEventStateAssert( upCast.getCurrentImageProcessorReference().getHeight(), INT_VAL_SECOND);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.ImageTypeChangedEvent ) {
            PropertyNotifyingImagePlus.ImageTypeChangedEvent upCast = (PropertyNotifyingImagePlus.ImageTypeChangedEvent) specEvent;
            int prevType = upCast.getPreviousImageType(), curType = upCast.getCurrentImageType();
            previousEventStateAssert( prevType, INT_VAL_FIRST);
            currentEventStateAssert( curType, INT_VAL_SECOND );
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.OverlayChangedEvent ) {
            PropertyNotifyingImagePlus.OverlayChangedEvent upCast = (PropertyNotifyingImagePlus.OverlayChangedEvent) specEvent;
            Rectangle prevBoundsForRoi = upCast.getPreviousOverlay().get(0).getBounds();
            Rectangle curBoundsForRoi = upCast.getCurrentOverlay().get(0).getBounds();
            previousEventStateAssert( prevBoundsForRoi.height, INT_VAL_FIRST );
            currentEventStateAssert( curBoundsForRoi.height, INT_VAL_SECOND);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent ) {
            PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent upCast = (PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent) specEvent;
            previousEventStateAssert( upCast.wasPreviouslyVisible(), false);
            currentEventStateAssert( upCast.isCurrentlyVisible(), true );
            labeledEventStateAssert( upCast.getNumberOfVisibilityChanges(), 1, "Visibility Count");
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.RoiChangeEvent ) {
            PropertyNotifyingImagePlus.RoiChangeEvent upCast = (PropertyNotifyingImagePlus.RoiChangeEvent) specEvent;
            Rectangle prevBoundsForRoi = upCast.getPreviousRoi().getBounds();
            Rectangle curBoundsForRoi = upCast.getCurrentRoi().getBounds();
            previousEventStateAssert( prevBoundsForRoi.height, INT_VAL_FIRST );
            currentEventStateAssert( curBoundsForRoi.height, INT_VAL_SECOND);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.StackDimensionsChangedEvent ) {
            PropertyNotifyingImagePlus.StackDimensionsChangedEvent upCast = (PropertyNotifyingImagePlus.StackDimensionsChangedEvent) specEvent;
            previousEventStateAssert( upCast.getPreviousNumChannels(), INT_VAL_FIRST );
            previousEventStateAssert( upCast.getPreviousNumFrames(), INT_VAL_FIRST );
            previousEventStateAssert( upCast.getPreviousNumSlices(), INT_VAL_FIRST );
            currentEventStateAssert( upCast.getCurrentNumChannels(), INT_VAL_SECOND);
            currentEventStateAssert( upCast.getCurrentNumFrames(), INT_VAL_SECOND);
            currentEventStateAssert( upCast.getCurrentNumSlices(), INT_VAL_SECOND);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.StackPositionChangedEvent ) {
            PropertyNotifyingImagePlus.StackPositionChangedEvent upCast = (PropertyNotifyingImagePlus.StackPositionChangedEvent) specEvent;
            previousEventStateAssert( upCast.getPreviousChannel(), INT_VAL_FIRST );
            previousEventStateAssert( upCast.getPreviousFrame(), INT_VAL_FIRST );
            previousEventStateAssert( upCast.getPreviousSlice(), INT_VAL_FIRST );
            currentEventStateAssert( upCast.getCurrentChannel(), INT_VAL_SECOND);
            currentEventStateAssert( upCast.getCurrentFrame(), INT_VAL_SECOND);
            currentEventStateAssert( upCast.getCurrentSlice(), INT_VAL_SECOND);
            //Should we test ImageProcessor?
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.TitleChangedEvent ) {
            PropertyNotifyingImagePlus.TitleChangedEvent upCast = (PropertyNotifyingImagePlus.TitleChangedEvent) specEvent;
            previousEventStateAssert( upCast.getPreviousTitle(), STR_VAL_FIRST );
            currentEventStateAssert( upCast.getCurrentTitle(), STR_VAL_SECOND);
        }
        
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.CalibrationChangedEvent
     * <p>
     * Parameters are differentiators
     * @param previousUnit - The Previous String for the unit
     * @param currentUnit - The Current String for the unit
     * @return 
     */
    private MasterSpecificEvent produceCalibrationEventDummy( String previousUnit, String currentUnit ) {
            //Calibration Event Dummy
        Calibration prevCal = new Calibration(), curCal = new Calibration();
        //Set differentiating values
        prevCal.setUnit( previousUnit );
        curCal.setUnit( currentUnit );
        return new PropertyNotifyingImagePlus.CalibrationChangedEvent( propImPlus_, prevCal, curCal);
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.FileInfoChangedEvent
     * <p>
     * Parameters are differentiators
     * @param previousUnit - The Previous String for the fileInfo.directory
     * @param currentUnit - The Current String for the fileInfo.directory
     * @return 
     */
    private MasterSpecificEvent produceFileInfoEventDummy( String previousDir, String currentDir ) {
        //File Info Change Event
        FileInfo prevFi = new FileInfo(), curFi = new FileInfo();
        //Set differentiating values
        prevFi.directory = previousDir;
        curFi.directory = currentDir;
        return new PropertyNotifyingImagePlus.FileInfoChangedEvent( propImPlus_, prevFi, curFi );
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.ImageChangedEvent
     * <p>
     * Parameters are differentiators
     * @param prevImageHeightWidth - The Previous pixel number for square height and width
     * @param curImageHeightWidth - The Current pixel number for square height and width
     * @return 
     */
    private MasterSpecificEvent produceImageChangedEventDummy( int prevImageHeightWidth, int curImageHeightWidth ) {
        //ImageChanged in ImagePlus Event
        //Differentiating value is height and width
        Image prevImg = new BufferedImage( prevImageHeightWidth, prevImageHeightWidth, BufferedImage.TYPE_BYTE_GRAY );
        Image curImg = new BufferedImage( curImageHeightWidth, curImageHeightWidth, BufferedImage.TYPE_BYTE_GRAY );
        return new PropertyNotifyingImagePlus.ImageChangedEvent( propImPlus_ , prevImg, curImg);
    }
    
        
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.ImageDimensionsChangedEvent
     * <p>
     * Parameters are differentiators
     * @param prevImageHeightWidth - The Previous pixel number for square height and width
     * @param curImageHeightWidth - The Current pixel number for square height and width
     * @return 
     */
    private MasterSpecificEvent produceImageDimensionsEventDummy( int prevImageHeightWidth, int curImageHeightWidth ) {
        //ImageDimensions Changed Event
        return new PropertyNotifyingImagePlus.ImageDimensionsChangedEvent(propImPlus_, prevImageHeightWidth, prevImageHeightWidth, curImageHeightWidth, curImageHeightWidth);
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.ImageProcessorChangeEvent
     * <p>
     * Parameters are differentiators
     * @param prevImageHeightWidth - The Previous pixel number for square height and width of the processor
     * @param curImageHeightWidth - The Current pixel number for square height and width of the processor
     * @param evtFlags - The eventFlags to populate the Event with
     * @return 
     */
    private MasterSpecificEvent produceImageProcessorEventDummy( int prevImageHeightWidth, int curImageHeightWidth, int evtFlags ) {
        //ImageProcessorChange Event
        ShortProcessor prevIp = new ShortProcessor( prevImageHeightWidth, prevImageHeightWidth ), curIp = new ShortProcessor( curImageHeightWidth, curImageHeightWidth );
        return new PropertyNotifyingImagePlus.ImageProcessorChangeEvent(propImPlus_, prevIp, curIp,  evtFlags );    
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.ImageTypeChangedEvent
     * <p>
     * Parameters are differentiators
     * @param prevType - The Previous ImageType as defined by constants in ImagePlus (ideally)
     * @param currentType - The Current ImageType as defined by constants in ImagePlus (ideally)
     * @return 
     */
    private MasterSpecificEvent produceImageTypeEventDummy( int prevType, int currentType ) {
        //ImageType Changed Event
        return new PropertyNotifyingImagePlus.ImageTypeChangedEvent(propImPlus_, prevType, currentType); 
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.OverlayChangedEvent
     * <p>
     * Parameters are differentiators
     * @param prevRoiHeightWidth - The Previous square Height and Width plus corner coords of the Roi in the Overlay
     * @param curRoiHeightWidth - The Current square Height and Width plus corner coords of the Roi in the Overlay
     * @return 
     */
    private MasterSpecificEvent produceOverlayChangeEventDummy( int prevRoiHeightWidth, int curRoiHeightWidth ) {
        //Overlay Changed Event
        //Differentiators is ROI widths and locations
        Roi prevRoi = new Roi( prevRoiHeightWidth, prevRoiHeightWidth, prevRoiHeightWidth, prevRoiHeightWidth);
        Roi curRoi = new Roi( curRoiHeightWidth, curRoiHeightWidth, curRoiHeightWidth, curRoiHeightWidth);
        Overlay prevOv = new Overlay( prevRoi );
        Overlay curOv = new Overlay( curRoi );
        return new PropertyNotifyingImagePlus.OverlayChangedEvent(propImPlus_, prevOv, curOv);    
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent
     * <p>
     * Parameters are differentiators
     * @param prevState - The Previous boolean state of the Overlay
     * @param curState - The Current boolean state of the Overlay
     * @return 
     */
    private MasterSpecificEvent produceOverlayVisibilityEventDummy( boolean prevState, boolean curState ) {
        //Overlay Visibility Changed Event
        return new PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent(propImPlus_, prevState, curState);   
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.OverlayChangedEvent
     * <p>
     * Parameters are differentiators
     * @param prevRoiHeightWidth - The Previous square Height and Width plus corner coords of the Roi in the Overlay
     * @param curRoiHeightWidth - The Current square Height and Width plus corner coords of the Roi in the Overlay
     * @return 
     */
    private MasterSpecificEvent produceRoiChangeEventDummy( int prevRoiHeightWidth, int curRoiHeightWidth ) {
        //Roi Changed Event
        Roi prevRoi = new Roi( prevRoiHeightWidth, prevRoiHeightWidth, prevRoiHeightWidth, prevRoiHeightWidth);
        Roi curRoi = new Roi( curRoiHeightWidth, curRoiHeightWidth, curRoiHeightWidth, curRoiHeightWidth);
        return new PropertyNotifyingImagePlus.RoiChangeEvent(propImPlus_, prevRoi, curRoi); 
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.StackDimensionsChangedEvent
     * <p>
     * Parameters are differentiators.  
     * Position Parameters will also create two ImageProcessors of the same width and height as the current and previous value.
     * 
     * @param prevNumDimensions - The Previous Dimensions and position - Will be set for Channel, Slice, and Frame
     * @param curNumDimensions - The Current Dimensions and position - Will be set for Channel, Slice, and Frame
     * @return 
     */
    private MasterSpecificEvent produceStackDimensionEventDummy( int prevNumDimensions, int curNumDimensions ) {
        //Stack Dimensions and Position Changed Events
        ShortProcessor prevIp = new ShortProcessor( prevNumDimensions, prevNumDimensions ), curIp = new ShortProcessor( curNumDimensions, curNumDimensions );
        int[] prevCZT = new int[]{ prevNumDimensions, prevNumDimensions, prevNumDimensions};
        int[] curCZT = new int[]{ curNumDimensions, curNumDimensions, curNumDimensions };
        int[] prevDimCZT = new int[]{ prevNumDimensions, prevNumDimensions, prevNumDimensions};
        int[] curDimCZT = new int[]{ curNumDimensions, curNumDimensions, curNumDimensions };
        return new PropertyNotifyingImagePlus.StackDimensionsChangedEvent(propImPlus_, prevDimCZT, prevCZT, prevIp,
                                                                                                         curDimCZT, curCZT, curIp ); 
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.StackPositionChangedEvent
     * <p>
     * Parameters are differentiators.  
     * Position Parameters will also create two ImageProcessors of the same width and height as the current and previous value.
     * 
     * @param prevPosition - The Previous position - Will be set for Channel, Slice, and Frame
     * @param curPosition - The Current position - Will be set for Channel, Slice, and Frame
     * @return 
     */
    private MasterSpecificEvent produceStackPositionEventDummy( int prevPosition, int curPosition ) {
        //Stack Dimensions and Position Changed Events
        ShortProcessor prevIp = new ShortProcessor( prevPosition, prevPosition ), curIp = new ShortProcessor( curPosition, curPosition );
        int[] prevCZT = new int[]{ prevPosition, prevPosition, prevPosition};
        int[] curCZT = new int[]{ curPosition, curPosition, curPosition };
        return new PropertyNotifyingImagePlus.StackPositionChangedEvent(propImPlus_, prevCZT, prevIp, curCZT, curIp);
    }
    
    /**
     * Creates a Dummy Event of the Type PropertyNotifyingImagePlus.TitleChangedEvent
     * <p>
     * Parameters are differentiators.  
     * 
     * @param prevTitle - The Previous title
     * @param curTitle - The Current title
     * @return 
     */
    private MasterSpecificEvent produceTitleChangeEventDummy( String prevTitle, String curTitle ) {
        //title Change Event
        return new PropertyNotifyingImagePlus.TitleChangedEvent(propImPlus_, prevTitle, curTitle);
    }
    
    
    
    /**
     * Produces an array of two dummy events (same type) for each PropertyNotifyingImagePlus.event.  The Events follow 
     * Constant values as defined in the class with _FIRST being the previous value and _SECOND 
     * being the current. The second Event is previous _SECOND and current value, _THIRD. 
     * <p>
     * Calls produceSingleTestEvents() to expand on the output
     * @return - { { MasterSpecificEvent subclass, SAME MasterSpecificEvent subclass } }
     */
    @DataProvider( name = "DummyFirstAndSecondEventTest" )
    public Object[][] produceTwoTestEvents() {
        
        //Base This off of the single Event since we're extending it's values
        Object[][] singleEvents = produceSingleTestEvents();
        Object[][] twoEvents = new Object[singleEvents.length][2];
        for ( int i = 0; i < singleEvents.length; ++i) {
            Object evt = singleEvents[i][0];
            //Could use reflection, but due to nature of Subscriber generality, we'll just hardcode
            if ( evt instanceof PropertyNotifyingImagePlus.CalibrationChangedEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceCalibrationEventDummy(STR_VAL_SECOND, STR_VAL_THIRD );
            }
            if ( evt instanceof PropertyNotifyingImagePlus.FileInfoChangedEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceFileInfoEventDummy( STR_VAL_SECOND, STR_VAL_THIRD );
            }
            if ( evt instanceof PropertyNotifyingImagePlus.ImageChangedEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceImageChangedEventDummy( INT_VAL_SECOND, INT_VAL_THIRD );
            }
            if ( evt instanceof PropertyNotifyingImagePlus.ImageDimensionsChangedEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceImageDimensionsEventDummy( INT_VAL_SECOND, INT_VAL_THIRD );
            }
            if ( evt instanceof PropertyNotifyingImagePlus.ImageProcessorChangeEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceImageProcessorEventDummy( INT_VAL_SECOND, INT_VAL_THIRD , EVTFLAG_VAL_SECOND );
            }
            if ( evt instanceof PropertyNotifyingImagePlus.ImageTypeChangedEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceImageTypeEventDummy( INT_VAL_SECOND, INT_VAL_THIRD   );  
            }
            if ( evt instanceof PropertyNotifyingImagePlus.OverlayChangedEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceOverlayChangeEventDummy( INT_VAL_SECOND, INT_VAL_THIRD );
            }
            if ( evt instanceof PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceOverlayVisibilityEventDummy( true, false );
            }
            if ( evt instanceof PropertyNotifyingImagePlus.RoiChangeEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] =  produceRoiChangeEventDummy( INT_VAL_SECOND, INT_VAL_THIRD );
            }
            //Since Subclassed, only create one Event
            if ( evt instanceof PropertyNotifyingImagePlus.StackDimensionsChangedEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceStackDimensionEventDummy( INT_VAL_SECOND, INT_VAL_THIRD );
            } else if ( evt instanceof PropertyNotifyingImagePlus.StackPositionChangedEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceStackPositionEventDummy( INT_VAL_SECOND, INT_VAL_THIRD );
            }
            
            if ( evt instanceof PropertyNotifyingImagePlus.TitleChangedEvent) {
                twoEvents[i][0] = evt;
                twoEvents[i][1] = produceTitleChangeEventDummy( STR_VAL_SECOND, STR_VAL_THIRD );
            }
        }      
        
        return twoEvents;
    }
    
    /**
     * Test of postToEventQueue method, with multiple posts of the same mock class from each PropertyNotifyingImagePlus.Event  
     * Ensures that Event Queue is added to, that only the multiple events are concatenated, and that the values that were appended have not changed from the expectation
     * as set up in the dataProvider.
     */
    @Test(dataProvider = "DummyFirstAndSecondEventTest",  dependsOnGroups = {"init"} )
    public void testPostToEventQueue_DoubleEvents( MasterSpecificEvent specEvent1, MasterSpecificEvent specEvent2 ) {
        System.out.println("postToEventQueue_DoubleEvents for Event Class: " + specEvent1.getClass().getName() );
        
        propImPlus_.postToEventQueue( specEvent1 );
        propImPlus_.postToEventQueue( specEvent2 );
        
        //Note, Registration will be evaluated by the Test Object
        Map< String, MasterSpecificEvent> evtQueue = propImPlus_.getCopyOfEventQueue();
        //We Call the Test Contexts Utility to Clear the Event Queue
        propImPlus_.entryContextsUtilityTest();
        
        //Make sure only 1 event exists instead of two (i.e it was concatenated)
        Assert.assertEquals(evtQueue.size(), 1);
        //Get the reference from the queue (only first)
        MasterSpecificEvent queuedEvt = null;
        for( MasterSpecificEvent evt : evtQueue.values() ) {
            queuedEvt = evt;
            if( queuedEvt != null ) {
                break;
            }
        }
        Assert.assertTrue( queuedEvt.sameOriginator(propImPlus_), "The Originator was not detected to the be same as the stored reference.");
        
        if( queuedEvt instanceof PropertyNotifyingImagePlus.CalibrationChangedEvent ) {
            PropertyNotifyingImagePlus.CalibrationChangedEvent upCast = (PropertyNotifyingImagePlus.CalibrationChangedEvent) queuedEvt;
            previousEventStateAssert( upCast.getPreviousCalibration().getUnit(), STR_VAL_FIRST);
            currentEventStateAssert( upCast.getCurrentCalibration().getUnit(), STR_VAL_THIRD);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.FileInfoChangedEvent ) {
            PropertyNotifyingImagePlus.FileInfoChangedEvent upCast = (PropertyNotifyingImagePlus.FileInfoChangedEvent) queuedEvt;
            previousEventStateAssert( upCast.getPreviousFileInfo().directory, STR_VAL_FIRST);
            currentEventStateAssert( upCast.getCurrentFileInfo().directory, STR_VAL_THIRD);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.ImageChangedEvent ) {
            PropertyNotifyingImagePlus.ImageChangedEvent upCast = (PropertyNotifyingImagePlus.ImageChangedEvent) queuedEvt;
            previousEventStateAssert( ((BufferedImage)upCast.getPreviousImage()).getWidth(), INT_VAL_FIRST);
            previousEventStateAssert( ((BufferedImage)upCast.getPreviousImage()).getHeight(), INT_VAL_FIRST);
            currentEventStateAssert( ((BufferedImage)upCast.getCurrentImage()).getHeight(), INT_VAL_THIRD);
            currentEventStateAssert( ((BufferedImage)upCast.getCurrentImage()).getWidth(), INT_VAL_THIRD);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.ImageDimensionsChangedEvent ) {
            PropertyNotifyingImagePlus.ImageDimensionsChangedEvent upCast = (PropertyNotifyingImagePlus.ImageDimensionsChangedEvent) queuedEvt;
            previousEventStateAssert( upCast.getPreviousWidth(), INT_VAL_FIRST);
            previousEventStateAssert( upCast.getPreviousHeight(), INT_VAL_FIRST);
            currentEventStateAssert( upCast.getCurrentWidth(), INT_VAL_THIRD);
            currentEventStateAssert( upCast.getCurrentHeight(), INT_VAL_THIRD);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.ImageProcessorChangeEvent ) {
            PropertyNotifyingImagePlus.ImageProcessorChangeEvent upCast = (PropertyNotifyingImagePlus.ImageProcessorChangeEvent) queuedEvt;
            labeledEventStateAssert( upCast.getEventFlags(), EVTFLAG_VAL_FIRST | EVTFLAG_VAL_SECOND, "ImageProcessor Event Flags");
            previousEventStateAssert( upCast.getPreviousImageProcessorReference().getWidth(), INT_VAL_FIRST);
            previousEventStateAssert( upCast.getPreviousImageProcessorReference().getHeight(), INT_VAL_FIRST);
            currentEventStateAssert( upCast.getCurrentImageProcessorReference().getWidth(), INT_VAL_THIRD);
            currentEventStateAssert( upCast.getCurrentImageProcessorReference().getHeight(), INT_VAL_THIRD);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.ImageTypeChangedEvent ) {
            PropertyNotifyingImagePlus.ImageTypeChangedEvent upCast = (PropertyNotifyingImagePlus.ImageTypeChangedEvent) queuedEvt;
            int prevType = upCast.getPreviousImageType(), curType = upCast.getCurrentImageType();
            previousEventStateAssert( prevType, INT_VAL_FIRST);
            currentEventStateAssert( curType, INT_VAL_THIRD );
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.OverlayChangedEvent ) {
            PropertyNotifyingImagePlus.OverlayChangedEvent upCast = (PropertyNotifyingImagePlus.OverlayChangedEvent) queuedEvt;
            Rectangle prevBoundsForRoi = upCast.getPreviousOverlay().get(0).getBounds();
            Rectangle curBoundsForRoi = upCast.getCurrentOverlay().get(0).getBounds();
            previousEventStateAssert( prevBoundsForRoi.height, INT_VAL_FIRST );
            currentEventStateAssert( curBoundsForRoi.height, INT_VAL_THIRD);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent ) {
            PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent upCast = (PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent) queuedEvt;
            previousEventStateAssert( upCast.wasPreviouslyVisible(), false);
            currentEventStateAssert( upCast.isCurrentlyVisible(), false );
            labeledEventStateAssert( upCast.getNumberOfVisibilityChanges(), 2, "Visibility Count");
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.RoiChangeEvent ) {
            PropertyNotifyingImagePlus.RoiChangeEvent upCast = (PropertyNotifyingImagePlus.RoiChangeEvent) queuedEvt;
            Rectangle prevBoundsForRoi = upCast.getPreviousRoi().getBounds();
            Rectangle curBoundsForRoi = upCast.getCurrentRoi().getBounds();
            previousEventStateAssert( prevBoundsForRoi.height, INT_VAL_FIRST );
            currentEventStateAssert( curBoundsForRoi.height, INT_VAL_THIRD);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.StackDimensionsChangedEvent ) {
            PropertyNotifyingImagePlus.StackDimensionsChangedEvent upCast = (PropertyNotifyingImagePlus.StackDimensionsChangedEvent) queuedEvt;
            previousEventStateAssert( upCast.getPreviousNumChannels(), INT_VAL_FIRST );
            previousEventStateAssert( upCast.getPreviousNumFrames(), INT_VAL_FIRST );
            previousEventStateAssert( upCast.getPreviousNumSlices(), INT_VAL_FIRST );
            currentEventStateAssert( upCast.getCurrentNumChannels(), INT_VAL_THIRD);
            currentEventStateAssert( upCast.getCurrentNumFrames(), INT_VAL_THIRD);
            currentEventStateAssert( upCast.getCurrentNumSlices(), INT_VAL_THIRD);
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.StackPositionChangedEvent ) {
            PropertyNotifyingImagePlus.StackPositionChangedEvent upCast = (PropertyNotifyingImagePlus.StackPositionChangedEvent) queuedEvt;
            previousEventStateAssert( upCast.getPreviousChannel(), INT_VAL_FIRST );
            previousEventStateAssert( upCast.getPreviousFrame(), INT_VAL_FIRST );
            previousEventStateAssert( upCast.getPreviousSlice(), INT_VAL_FIRST );
            currentEventStateAssert( upCast.getCurrentChannel(), INT_VAL_THIRD);
            currentEventStateAssert( upCast.getCurrentFrame(), INT_VAL_THIRD);
            currentEventStateAssert( upCast.getCurrentSlice(), INT_VAL_THIRD);
            //Should we test ImageProcessor?
        }
        if( queuedEvt instanceof PropertyNotifyingImagePlus.TitleChangedEvent ) {
            PropertyNotifyingImagePlus.TitleChangedEvent upCast = (PropertyNotifyingImagePlus.TitleChangedEvent) queuedEvt;
            previousEventStateAssert( upCast.getPreviousTitle(), STR_VAL_FIRST );
            currentEventStateAssert( upCast.getCurrentTitle(), STR_VAL_THIRD);
        }
        
    }
    
    /**
     * Produces an array of two dummy events (different types) for PropertyNotifyingImagePlus.events.  The Values
     * are of the order, _First and _Second and A string is appended to record A heading for the test.  
     * (i.e. subclass test)
     * <p>
     * @return - { { MasterSpecificEvent subclass, Different MasterSpecificEvent subclass, String AddedInfo } }
     */
    @DataProvider( name = "DummyTwoDifferentEventTest" )
    public Object[][] produceTwoDifferentTestEvents() {
        
        propImPlus_ = produceTestImagePlusInstance( constKey_ ); //Make Specific Blank ImPlus
        
        MasterSpecificEvent calEvt = produceCalibrationEventDummy( STR_VAL_FIRST, STR_VAL_SECOND );
        
        //File Info Change Event
        MasterSpecificEvent fInfoEvt = produceFileInfoEventDummy( STR_VAL_FIRST, STR_VAL_SECOND );
        
        //ImageChanged in ImagePlus Event
        MasterSpecificEvent imgEvt = produceImageChangedEventDummy( INT_VAL_FIRST, INT_VAL_SECOND );
        
        //ImageDimensions Changed Event
        MasterSpecificEvent dimEvt =produceImageDimensionsEventDummy( INT_VAL_FIRST, INT_VAL_SECOND);
        
        //ImageProcessorChange Event
        MasterSpecificEvent ipEvt = produceImageProcessorEventDummy( INT_VAL_FIRST, INT_VAL_SECOND, EVTFLAG_VAL_FIRST );
        
        //ImageType Changed Event
        MasterSpecificEvent typeEvt = produceImageTypeEventDummy( INT_VAL_FIRST, INT_VAL_SECOND  );       
        
        //Overlay Changed Event
        MasterSpecificEvent ovEvt = produceOverlayChangeEventDummy( INT_VAL_FIRST, INT_VAL_SECOND );       
        
        //Overlay Visibility Changed Event     
        MasterSpecificEvent ovVisEvt = produceOverlayVisibilityEventDummy( false, true );
        
        //Roi Changed Event
        MasterSpecificEvent roiEvt = produceRoiChangeEventDummy( INT_VAL_FIRST, INT_VAL_SECOND );
        
        //Stack Dimensions and Position Changed Event
        MasterSpecificEvent stackDimEvt = produceStackDimensionEventDummy( INT_VAL_FIRST, INT_VAL_SECOND );
        //Position Change
        MasterSpecificEvent stackPosEvt = produceStackPositionEventDummy( INT_VAL_FIRST, INT_VAL_SECOND );
        //Title Change
        MasterSpecificEvent titleEvt = produceTitleChangeEventDummy( STR_VAL_FIRST, STR_VAL_SECOND );
        
        return new Object[][]{ {stackDimEvt, stackPosEvt, "Stack Dimension and Stack Position subClass Test"},
                                { stackPosEvt, stackDimEvt, "Stack Position and Stack Dimension subClass Test" },
                                { ovVisEvt, ipEvt, "OverlayVisibility and ImageProcessor separate Events Test"}
                                };
        
    }
    
    
    /**
     * Test of postToEventQueue method, with multiple posts of different mock class from each PropertyNotifyingImagePlus.Event  
 Ensures that Event Queue is added to, that only the multiple events of the ultimate subclass are concatenated.  Checking of 
     * values is done in testPostToEventQueue_<Single/Double>Events methods
     * @param specEvent1 - The Specific Event to be posted to the queue first
     * @param specEvent2 - The Specific Event to be posted to the queue second
     * @param descriptor - A Descriptor of the Events being posted for Reading purposes
     */
    @Test(dataProvider = "DummyTwoDifferentEventTest", dependsOnGroups = {"init"} )
    public void testPostToEventQueue_DifferentEvents( MasterSpecificEvent specEvent1, MasterSpecificEvent specEvent2, String descriptor ) {
        System.out.println("postToEventQueue_DifferentEvents for: " + descriptor );
        propImPlus_.postToEventQueue( specEvent1 );
        propImPlus_.postToEventQueue( specEvent2 );
        
        //Note, Registration will be evaluated by the Test Object
        Map< String, MasterSpecificEvent> evtQueue = propImPlus_.getCopyOfEventQueue();
        //We Call the Test Contexts Utility to Clear the Event Queue
        propImPlus_.entryContextsUtilityTest();
        
        //Make sure 2 events exist
        Assert.assertEquals(evtQueue.size(), 2);

        //Currently no need to test for sameness of data given DoubleEvent and Single Event Analysis
    }
    
    
    
    /**
     * Simplified custom assertion for testing a Previous Property of an Event
     * @param <T>
     * @param actual - the actual event property
     * @param expected - the expected event property value
     */
    private <T> void previousEventStateAssert( T actual, T expected ) {
         Assert.assertEquals( actual, expected, "Failed Previous Expectation: " + actual + " should be " + expected );
    }
    
    /**
     * Simplified custom assertion for testing a Current Property of an Event
     * @param <T>
     * @param actual - the actual event property
     * @param expected - the expected event property value
     */
    private <T> void currentEventStateAssert( T actual, T expected ) {
         Assert.assertEquals( actual, expected, "Failed Current Expectation: " + actual + " should be " + expected );
    }
    
    /**
     * Simplified custom assertion for testing a custom Property of an Event.  This is
     * properties like EvtFlags that have a meaningful value that is not necessarily 
     * the same or a previous, current relationship.  
     * <p>
     * The output is of the form Failed <code>stateLable</code> Expectation: <code>actual</code> should be <code>expected</code>
     * 
     * @param <T>
     * @param actual - the actual event property
     * @param expected - the expected event property value
     * @param stateLabel - The name of the property being compared on the Event
     */
    private <T> void labeledEventStateAssert( T actual, T expected, String stateLabel ) {
         Assert.assertEquals( actual, expected, "Failed " +  stateLabel + " Expectation: " + actual + " should be " + expected );
    }
    
    /**
     * Test the setEntryContext of a single Entry and Exit with DummyEvent
     */
    @Test( dependsOnGroups = "init" )
    public void testEntryContextsUtilityTest( ) {
        
        propImPlus_ = produceTestImagePlusInstance( constKey_ ); //Make Specific Blank ImPlus
        
        System.out.println( "testEntryContextsUtilityTest" );
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onDummyEvent( PropertyNotifyingImagePlus.DummyEvent evt ) {
                System.out.println( "Event was recieved" );
                Assert.assertEquals( evt.getConcatCount(), 0);  //Single Recursion, no concatenation
                return true;
            }
            
        };
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.entryContextsUtilityTest();  //The actualy test to pass to the reciever
        
        //Since our method is not asynchronous, we can check in line if the queue has been purged and remove the reciever
        propImPlus_.UnregisterFromEventBus(reciever);
        Map<String, MasterSpecificEvent> queue = propImPlus_.getCopyOfEventQueue();
        Assert.assertTrue( queue.isEmpty(), "Event Queue was not purged.  Contains: " + queue.size() + " items");
        
    }
    
    /**
     * Test nested setEntryContext of a single Entry and Exit with DummyEvent
     */
    @Test( dependsOnGroups = "init" )
    @Parameters( { "numDummyRecursions" } )
    public void testRecursiveEntryContextsUtilityTest( String numRec ) {
        
        propImPlus_ = produceTestImagePlusInstance( constKey_ ); //Make Specific Blank ImPlus
        
        System.out.println( "EntryContextUtility nested recursion: " + numRec );
        final int numRecInt = Integer.parseInt(numRec);
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onDummyEvent( PropertyNotifyingImagePlus.DummyEvent evt ) {
                System.out.println("Dummy Event was recieved");
                Assert.assertEquals( evt.getConcatCount(), numRecInt);  //Single Recursion, no concatenation
                return true;
            }
            
        };
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.recursiveEntryContextsUtilityTest( numRecInt );  //The actualy test to pass to the reciever
        
        //Since our method is not asynchronous, we can check in line if the queue has been purged
        Map<String, MasterSpecificEvent> queue = propImPlus_.getCopyOfEventQueue();
        Assert.assertTrue( queue.isEmpty(), "Event Queue was not purged.  Contains: " + queue.size() + " items");
        //Since this is synchronous no fear of not calling callbacks
        propImPlus_.UnregisterFromEventBus(reciever);
        
    }
    
    /**
     * Test nested setEntryContext of a single Entry and Exit with Multiple Threads accessing
     * Non-Thread Safe Nature Does not contribute to any type of actual test.  To be implemented later if
     * Thread safety is desired.
     */
    /*@Test( dependsOnGroups = "init", parameters = { "numRecursions" } )
    public void testRecursiveEntryContextsUtilityTest_2ThreadAccess( final int numRecursions ) {
        System.out.println( "testEntryContextsUtilityTest" );
        
        //Construct Runnable
        Runnable proc = new Runnable() {
            @Override
            public void run() {
                propImPlus_.recursiveEntryContextsUtilityTest(numRecursions);
            }
            
        };
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onDummyEvent( PropertyNotifyingImagePlus.DummyEvent evt ) {
                Assert.assertEquals( evt.getConcatCount(), numRecursions);  //Single Recursion, no concatenation
                return true;
            }
            
        };
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.recursiveEntryContextsUtilityTest( numRecursions );  //The actualy test to pass to the reciever
        
        //Since our method is not asynchronous, we can check in line if the queue has been purged
        Map<String, MasterSpecificEvent> queue = propImPlus_.getCopyOfEventQueue();
        Assert.assertTrue( queue.isEmpty(), "Event Queue was not purged.  Contains: " + queue.size() + " items");
        
    }*/
    
    /**
     * Test of setOverlay method, of class PropertyNotifyingImagePlus.
     */
     @Test( dependsOnGroups = "init" )
    public void testSetOverlay() {
        System.out.println("setOverlay");
        
        propImPlus_ = produceTestImagePlusInstance( constKey_ ); //Make Specific Blank ImPlus
        
        final Overlay ov = new Overlay();
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onOverlayChangedEvent( PropertyNotifyingImagePlus.OverlayChangedEvent evt ) {
                System.out.println("Overlay Changed Event was recieved");
                
                Assert.assertEquals( evt.getCurrentOverlay(), ov );  //Single Recursion, no concatenation
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onOverlayVisibilityChangedEvent( PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt ) {
                System.out.println("Overlay Visibility Even was recieved");
                
                String prevVis = evt.wasPreviouslyVisible() ? "true" : "false";
                String curVis = evt.isCurrentlyVisible() ? "true" : "false";
                
                System.out.println("The Visibility changed from: " + prevVis + " to " + curVis);
                return true;
            }
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setOverlay(ov);
        //Since this is synchronous no fear of not calling callbacks
        propImPlus_.UnregisterFromEventBus(reciever);        

    }

    /**
     * Test of setHideOverlay method, of class PropertyNotifyingImagePlus.
     */
     @Test( dependsOnGroups = "init" )
    public void testSetHideOverlay() {
        System.out.println("setHideOverlay");
        
        propImPlus_ = produceTestImagePlusInstance( constKey_ ); //Make Specific Blank ImPlus
        
        final boolean visible = true;
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onOverlayVisibilityChangedEvent( PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt ) {
                System.out.println("Overlay Visibility Changed Event was recieved");
                
                Assert.assertEquals( evt.isCurrentlyVisible(), visible );  //Single Recursion, no concatenation
                return true;
            }
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setHideOverlay(visible);
        //Since this is synchronous no fear of not calling callbacks
        propImPlus_.UnregisterFromEventBus(reciever);
    }

    /**
     * Test of updatePosition method, of class PropertyNotifyingImagePlus.
     */
     @Test( dependsOnGroups = "init" )
    public void testSetSlice_UpdatePositionWrap() {
        System.out.println("updatePosition");
        
        final int slice = 4;
        
        //Establish an ImagePlus with a stack so we can test it
        ImageStack iStack =  ImageStack.create(200, 200, 5, 16);
        propImPlus_ = new PropertyNotifyingImagePlus( "Black ImageStack Mock ImagePlus", iStack );
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onStackPositionChangedEvent( PropertyNotifyingImagePlus.StackPositionChangedEvent evt ) {
                System.out.println("StackPosition Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousSlice(), 1);
                Assert.assertEquals( evt.getCurrentSlice(), slice );  //Single Recursion, no concatenation
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onOverlayVisibilityChangedEvent( PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt ) {
                System.out.println("Overlay Visibility Even was recieved");
                
                String prevVis = evt.wasPreviouslyVisible() ? "true" : "false";
                String curVis = evt.isCurrentlyVisible() ? "true" : "false";
                
                System.out.println("The Visibility changed from: " + prevVis + " to " + curVis);
                return true;
            }
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setSlice(4);
        //Since this is synchronous no fear of not calling callbacks
        propImPlus_.UnregisterFromEventBus(reciever);
    }

    /**
     * Test of setDimensions method, of class PropertyNotifyingImagePlus.
     */
     @Test( dependsOnGroups = {"init"}, dependsOnMethods = {"testGetNSlices"} )
    public void testSetDimensions() {
        System.out.println("setDimensions");
        
        final int nFrames = 5;
        
        //Establish an ImagePlus with a stack so we can test it
        ImageStack iStack =  ImageStack.create(200, 200, 5, 16);
        propImPlus_ = new PropertyNotifyingImagePlus( "Black ImageStack Mock ImagePlus", iStack );
        
        System.out.println("Stack Dimensions Event was recieved" + " " + propImPlus_.getNChannels() + "," + propImPlus_.getNSlices() + "," + propImPlus_.getNFrames()  );
        
        System.out.println("The ImageStack size is " + propImPlus_.getImageStackSize() );
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onStackPositionChangedEvent( PropertyNotifyingImagePlus.StackPositionChangedEvent evt ) {
                System.out.println("StackPosition Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousFrame(), 1);
                Assert.assertEquals( evt.getCurrentFrame(), propImPlus_.getT() );  //Single Recursion, no concatenation
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onOverlayVisibilityChangedEvent( PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt ) {
                System.out.println("Overlay Visibility Even was recieved");
                
                String prevVis = evt.wasPreviouslyVisible() ? "true" : "false";
                String curVis = evt.isCurrentlyVisible() ? "true" : "false";
                
                System.out.println("The Visibility changed from: " + prevVis + " to " + curVis);
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onStackDimensionsChangedEvent( PropertyNotifyingImagePlus.StackDimensionsChangedEvent evt ) {
                System.out.println("Stack Dimensions Event was recieved" + " " + propImPlus_.getNChannels() + "," + propImPlus_.getNSlices() + "," + propImPlus_.getNFrames()  );
                
                Assert.assertEquals( evt.getPreviousNumFrames(), 1);
                Assert.assertEquals( evt.getCurrentNumFrames(), nFrames );  //Single Recursion, no concatenation                
                return true;
            }
            
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setDimensions(1, 1, nFrames);

    }

    /**
     * Test of getNSlices method, of class PropertyNotifyingImagePlus.
     */
    @Test( dependsOnGroups = "init" )
    public void testGetNSlices() {
        System.out.println("getNSlices");
                
        //Establish an ImagePlus with a stack so we can test it
        ImageStack iStack =  ImageStack.create(200, 200, 6, 16); //5 Deep Stack
        propImPlus_ = new PropertyNotifyingImagePlus( "Black ImageStack Mock ImagePlus", iStack );

        System.out.println( propImPlus_.getNChannels() + "," + propImPlus_.getNSlices()+ "," + propImPlus_.getNFrames() );
        
        //This is because they want slice*ch*frame to equal stack size...
        final int nSlices = 2;
        final int nChannels = 3;
        final int nFrames = 1;
        
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onStackPositionChangedEvent( PropertyNotifyingImagePlus.StackPositionChangedEvent evt ) {
                System.out.println("StackPosition Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousSlice(), 1);
                Assert.assertEquals( evt.getCurrentSlice(), propImPlus_.getZ() );  //Single Recursion, no concatenation
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onOverlayVisibilityChangedEvent( PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt ) {
                System.out.println("Overlay Visibility Even was recieved");
                
                String prevVis = evt.wasPreviouslyVisible() ? "true" : "false";
                String curVis = evt.isCurrentlyVisible() ? "true" : "false";
                
                System.out.println("The Visibility changed from: " + prevVis + " to " + curVis);
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onStackDimensionsChangedEvent( PropertyNotifyingImagePlus.StackDimensionsChangedEvent evt ) {
                System.out.println("Stack Dimensions Event was recieved");
                System.out.println("Prev " + evt.getPreviousNumChannels() + "," + evt.getPreviousNumSlices() + "," + evt.getPreviousNumFrames() );
                System.out.println("Prev " + evt.getCurrentNumChannels() + "," + evt.getCurrentNumSlices() + "," + evt.getCurrentNumFrames() );
                Assert.assertEquals( evt.getPreviousNumSlices(), 6);
                Assert.assertEquals( evt.getPreviousNumChannels(), 1);
                Assert.assertEquals( evt.getPreviousNumFrames(), 1);
                Assert.assertEquals( evt.getCurrentNumSlices(), nSlices );  //Single Recursion, no concatenation                
                Assert.assertEquals( evt.getCurrentNumFrames(), nFrames);
                Assert.assertEquals( evt.getCurrentNumChannels(), nChannels );
                return true;
            }
            
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setDimensions(nChannels, nSlices, nFrames);
        int result = propImPlus_.getNSlices();

        assertEquals(result, nSlices);
    }

    /**
     * Test of getNChannels method, of class PropertyNotifyingImagePlus.
     */
     @Test( dependsOnGroups = "init" )
    public void testGetNChannels() {
        System.out.println("getNChannels");
                
        //Establish an ImagePlus with a stack so we can test it
        ImageStack iStack =  ImageStack.create(200, 200, 6, 16); //5 Deep Stack
        propImPlus_ = new PropertyNotifyingImagePlus( "Black ImageStack Mock ImagePlus", iStack );

        System.out.println( propImPlus_.getNChannels() + "," + propImPlus_.getNSlices()+ "," + propImPlus_.getNFrames() );
        
        //This is because they want slice*ch*frame to equal stack size...
        final int nSlices = 2;
        final int nChannels = 3;
        final int nFrames = 1;
        
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onStackPositionChangedEvent( PropertyNotifyingImagePlus.StackPositionChangedEvent evt ) {
                System.out.println("StackPosition Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousSlice(), 1);
                Assert.assertEquals( evt.getCurrentSlice(), propImPlus_.getZ() );  //Single Recursion, no concatenation
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onOverlayVisibilityChangedEvent( PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt ) {
                System.out.println("Overlay Visibility Even was recieved");
                
                String prevVis = evt.wasPreviouslyVisible() ? "true" : "false";
                String curVis = evt.isCurrentlyVisible() ? "true" : "false";
                
                System.out.println("The Visibility changed from: " + prevVis + " to " + curVis);
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onStackDimensionsChangedEvent( PropertyNotifyingImagePlus.StackDimensionsChangedEvent evt ) {
                System.out.println("Stack Dimensions Event was recieved");
                System.out.println("Prev " + evt.getPreviousNumChannels() + "," + evt.getPreviousNumSlices() + "," + evt.getPreviousNumFrames() );
                System.out.println("Prev " + evt.getCurrentNumChannels() + "," + evt.getCurrentNumSlices() + "," + evt.getCurrentNumFrames() );
                Assert.assertEquals( evt.getPreviousNumSlices(), 6);
                Assert.assertEquals( evt.getPreviousNumChannels(), 1);
                Assert.assertEquals( evt.getPreviousNumFrames(), 1);
                Assert.assertEquals( evt.getCurrentNumSlices(), nSlices );  //Single Recursion, no concatenation                
                Assert.assertEquals( evt.getCurrentNumFrames(), nFrames);
                Assert.assertEquals( evt.getCurrentNumChannels(), nChannels );
                return true;
            }
            
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setDimensions(nChannels, nSlices, nFrames);
        int result = propImPlus_.getNChannels();

        assertEquals(result, nChannels);
    }

    /**
     * Test of getNFrames method, of class PropertyNotifyingImagePlus.
     */
    @Test( dependsOnGroups = "init" )
    public void testGetNFrames() {
        System.out.println("getNFrames");
                
        //Establish an ImagePlus with a stack so we can test it
        ImageStack iStack =  ImageStack.create(200, 200, 6, 16); //5 Deep Stack
        propImPlus_ = new PropertyNotifyingImagePlus( "Black ImageStack Mock ImagePlus", iStack );
        
        //This is because they want slice*ch*frame to equal stack size...
        final int nSlices = 1;
        final int nChannels = 2;
        final int nFrames = 3;
        
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onStackPositionChangedEvent( PropertyNotifyingImagePlus.StackPositionChangedEvent evt ) {
                System.out.println("StackPosition Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousSlice(), 1);
                Assert.assertEquals( evt.getCurrentSlice(), propImPlus_.getZ() );  //Single Recursion, no concatenation
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onOverlayVisibilityChangedEvent( PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt ) {
                System.out.println("Overlay Visibility Even was recieved");
                
                String prevVis = evt.wasPreviouslyVisible() ? "true" : "false";
                String curVis = evt.isCurrentlyVisible() ? "true" : "false";
                
                System.out.println("The Visibility changed from: " + prevVis + " to " + curVis);
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onStackDimensionsChangedEvent( PropertyNotifyingImagePlus.StackDimensionsChangedEvent evt ) {
                System.out.println("Stack Dimensions Event was recieved");
                Assert.assertEquals( evt.getPreviousNumSlices(), 6);
                Assert.assertEquals( evt.getPreviousNumChannels(), 1);
                Assert.assertEquals( evt.getPreviousNumFrames(), 1);
                Assert.assertEquals( evt.getCurrentNumSlices(), nSlices );  //Single Recursion, no concatenation                
                Assert.assertEquals( evt.getCurrentNumFrames(), nFrames);
                Assert.assertEquals( evt.getCurrentNumChannels(), nChannels );
                return true;
            }
            
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setDimensions(nChannels, nSlices, nFrames);
        int result = propImPlus_.getNFrames();

        assertEquals(result, nFrames);
    }

    /**
     * Test of getDimensions method, of class PropertyNotifyingImagePlus.
     */
    @Test( dependsOnGroups = "init" )
    public void testGetDimensions_Verify() {
        System.out.println("getDimensions");
        
                        
        //Establish an ImagePlus with a stack so we can test it
        ImageStack iStack =  ImageStack.create(200, 200, 6, 16); //5 Deep Stack
        propImPlus_ = new PropertyNotifyingImagePlus( "Black ImageStack Mock ImagePlus", iStack );
        
        //This is because they want slice*ch*frame to equal stack size...
        final int nSlices = 1;
        final int nChannels = 2;
        final int nFrames = 3;
        
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onStackPositionChangedEvent( PropertyNotifyingImagePlus.StackPositionChangedEvent evt ) {
                System.out.println("StackPosition Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousSlice(), 1);
                Assert.assertEquals( evt.getCurrentSlice(), propImPlus_.getZ() );  //Single Recursion, no concatenation
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onOverlayVisibilityChangedEvent( PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt ) {
                System.out.println("Overlay Visibility Even was recieved");
                
                String prevVis = evt.wasPreviouslyVisible() ? "true" : "false";
                String curVis = evt.isCurrentlyVisible() ? "true" : "false";
                
                System.out.println("The Visibility changed from: " + prevVis + " to " + curVis);
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onStackDimensionsChangedEvent( PropertyNotifyingImagePlus.StackDimensionsChangedEvent evt ) {
                System.out.println("Stack Dimensions Event was recieved");
                Assert.assertEquals( evt.getPreviousNumSlices(), 6);
                Assert.assertEquals( evt.getPreviousNumChannels(), 1);
                Assert.assertEquals( evt.getPreviousNumFrames(), 1);
                Assert.assertEquals( evt.getCurrentNumSlices(), nSlices );  //Single Recursion, no concatenation                
                Assert.assertEquals( evt.getCurrentNumFrames(), nFrames);
                Assert.assertEquals( evt.getCurrentNumChannels(), nChannels );
                return true;
            }
            
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setDimensions(nChannels, nSlices, nFrames);
        int[] expResult = new int[]{ propImPlus_.getWidth(), propImPlus_.getHeight(), nChannels, nSlices, nFrames };
        int[] result = propImPlus_.getDimensions( true );
        assertEquals(result, expResult);
    }

    /**
     * Test of setImage method, of class PropertyNotifyingImagePlus.
     */
    @Test( dependsOnGroups = "init" )
    public void testSetImage() {
        System.out.println("setImage");
        
        propImPlus_ = produceTestImagePlusInstance( "1" ); //Make Specific Blank Image ImPlus
        
        final int prevW = propImPlus_.getWidth(), prevH = propImPlus_.getHeight();
        final Image prevImg = propImPlus_.getImage();
        final Image image = new BufferedImage(400, 400, BufferedImage.TYPE_BYTE_GRAY);
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onImageChangedEvent( PropertyNotifyingImagePlus.ImageChangedEvent evt ) {
                System.out.println("Image Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousImage(), prevImg);
                Assert.assertEquals( evt.getCurrentImage(), propImPlus_.getImage() );
                
                //Apparently ImagePlus hates us and makes the image null
                //Assert.assertEquals( evt.getCurrentImage(), image );  //Single Recursion, no concatenation
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onImageDimensionsChangedEvent( PropertyNotifyingImagePlus.ImageDimensionsChangedEvent evt ) {
                System.out.println("Image Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousWidth(), prevW);
                Assert.assertEquals( evt.getPreviousHeight(), prevH);
                Assert.assertEquals( evt.getCurrentHeight(), 400 ); 
                Assert.assertEquals( evt.getCurrentWidth(), 400 );
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onImageProcessorChangeEvent( PropertyNotifyingImagePlus.ImageProcessorChangeEvent evt ) {
                System.out.println("Image Processor Event was recieved");
                
                Assert.assertTrue( (evt.getEventFlags() & PropertyNotifyingImagePlus.ImageProcessorChangeEvent.PIXELS_CHANGED) != 0 );
                return true;
            }
            
            
        };

        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setImage(image);
    }

    /**
     * Test of setProcessor method, of class PropertyNotifyingImagePlus.
     */
    @Test( dependsOnGroups = "init" )
    public void testSetProcessor() {
        System.out.println("setProcessor");
         
        propImPlus_ = produceTestImagePlusInstance( "2" ); //Make Specific ShortProc Image ImPlus
        
        final int prevW = propImPlus_.getWidth(), prevH = propImPlus_.getHeight();
        
        final String prevTitle = propImPlus_.getTitle();
        final String curTitle = ""; 
        final ImageProcessor prevIp = propImPlus_.getProcessor();
        final ImageProcessor curIp = new ShortProcessor( 500, 500 );
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
            
            @Override
            @Subscribe
            public boolean onImageProcessorChangeEvent( PropertyNotifyingImagePlus.ImageProcessorChangeEvent evt ) {
                System.out.println("Image Processor Event was recieved");
                Assert.assertEquals(evt.getPreviousImageProcessorReference(), prevIp);
                Assert.assertEquals(evt.getCurrentImageProcessorReference(), curIp);
                Assert.assertTrue( (evt.getEventFlags() & PropertyNotifyingImagePlus.ImageProcessorChangeEvent.REF_CHANGED) != 0 );
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onTitleChangedEvent( PropertyNotifyingImagePlus.TitleChangedEvent evt ) {
                System.out.println("Image Title Event was recieved from " + evt.getPreviousTitle() + " to " + evt.getCurrentTitle() );
                Assert.assertEquals(evt.getPreviousTitle(), prevTitle);
                Assert.assertEquals(evt.getCurrentTitle(), curTitle);
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onImageDimensionsChangedEvent( PropertyNotifyingImagePlus.ImageDimensionsChangedEvent evt ) {
                System.out.println("Image Dim Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousWidth(), prevW);
                Assert.assertEquals( evt.getPreviousHeight(), prevH);
                Assert.assertEquals( evt.getCurrentHeight(), 500 ); 
                Assert.assertEquals( evt.getCurrentWidth(), 500 );
                return true;
            }
            
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setProcessor(curTitle, curIp);

    }

    /**
     * Test of setStack method, of class PropertyNotifyingImagePlus.
     */
    @Test( dependsOnGroups = "init" )
    public void testSetStack() {
        System.out.println("setStack");
            //Establish an ImagePlus with a stack so we can test it
        ImageStack iStack =  ImageStack.create(200, 200, 4, 16); //5 Deep Stack
        propImPlus_ = new PropertyNotifyingImagePlus( "Black ImageStack Mock ImagePlus", iStack );
        
        final int prevW = propImPlus_.getWidth(), prevH = propImPlus_.getHeight();
        final ImageProcessor prevIp = propImPlus_.getProcessor();
        
        final ImageStack newStack = ImageStack.create( 400, 400, 12, 16);
        final int nSlices = 3;
        final int nChannels = 1;
        final int nFrames = 4;
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
          
            @Override
            @Subscribe
            public boolean onStackPositionChangedEvent( PropertyNotifyingImagePlus.StackPositionChangedEvent evt ) {
                System.out.println("StackPosition Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousSlice(), 1);
                Assert.assertEquals( evt.getCurrentSlice(), propImPlus_.getZ() );  //Single Recursion, no concatenation
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onOverlayVisibilityChangedEvent( PropertyNotifyingImagePlus.OverlayVisibilityChangedEvent evt ) {
                System.out.println("Overlay Visibility Even was recieved");
                
                String prevVis = evt.wasPreviouslyVisible() ? "true" : "false";
                String curVis = evt.isCurrentlyVisible() ? "true" : "false";
                
                System.out.println("The Visibility changed from: " + prevVis + " to " + curVis);
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onStackDimensionsChangedEvent( PropertyNotifyingImagePlus.StackDimensionsChangedEvent evt ) {
                System.out.println("Stack Dimensions Event was recieved");
                Assert.assertEquals( evt.getPreviousNumSlices(), 4);
                Assert.assertEquals( evt.getPreviousNumChannels(), 1);
                Assert.assertEquals( evt.getPreviousNumFrames(), 1);
                Assert.assertEquals( evt.getCurrentNumSlices(), nSlices );  //Single Recursion, no concatenation                
                Assert.assertEquals( evt.getCurrentNumFrames(), nFrames);
                Assert.assertEquals( evt.getCurrentNumChannels(), nChannels );
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onImageDimensionsChangedEvent( PropertyNotifyingImagePlus.ImageDimensionsChangedEvent evt ) {
                System.out.println("Image Dim Changed Event was recieved");
                Assert.assertEquals( evt.getPreviousWidth(), prevW);
                Assert.assertEquals( evt.getPreviousHeight(), prevH);
                Assert.assertEquals( evt.getCurrentHeight(), 400 ); 
                Assert.assertEquals( evt.getCurrentWidth(), 400 );
                return true;
            }
            
            @Override
            @Subscribe
            public boolean onImageProcessorChangeEvent( PropertyNotifyingImagePlus.ImageProcessorChangeEvent evt ) {
                System.out.println("Image Processor Event was recieved");
                Assert.assertEquals(evt.getPreviousImageProcessorReference(), prevIp);
                Assert.assertEquals(evt.getCurrentImageProcessorReference(), propImPlus_.getProcessor());
                Assert.assertTrue( (evt.getEventFlags() & PropertyNotifyingImagePlus.ImageProcessorChangeEvent.REF_CHANGED) != 0 );
                return true;
            }
            
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setStack(newStack, nChannels, nSlices, nFrames);
        
    }


    /**
     * Test of setFileInfo method, of class PropertyNotifyingImagePlus.
     */
    @Test( dependsOnGroups = "init" )
    public void testSetFileInfo() {
        System.out.println("setFileInfo");
        
        propImPlus_ = produceTestImagePlusInstance("1");
        
        final FileInfo prevFi = propImPlus_.getOriginalFileInfo();
        
        final FileInfo curFi = new FileInfo();
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
        
            @Override
            @Subscribe
            public boolean onFileInfoChangeEvent( PropertyNotifyingImagePlus.FileInfoChangedEvent evt ) {
                System.out.println( "FileInfoChange Event Recieved");
                
                Assert.assertEquals(evt.getPreviousFileInfo(), prevFi);
                Assert.assertEquals(evt.getCurrentFileInfo(), curFi);
                return true;
            }
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        propImPlus_.setFileInfo(curFi);
    }

    /**
     * Test of trimProcessor method, of class PropertyNotifyingImagePlus.
     */
    @Test( dependsOnGroups = "init" )
    public void testTrimProcessor() {
        System.out.println("trimProcessor");

                    //Establish an ImagePlus with a stack so we can test it
        ImageStack iStack =  ImageStack.create(200, 200, 4, 16); //5 Deep Stack
        propImPlus_ = new PropertyNotifyingImagePlus( "Black ImageStack Mock ImagePlus", iStack );
        
        final ImageProcessor prevIp = propImPlus_.getProcessor();
        
        EvtRecieverEval reciever = new EvtRecieverEval() { 
            
            @Override
            @Subscribe
            public boolean onImageProcessorChangeEvent( PropertyNotifyingImagePlus.ImageProcessorChangeEvent evt ) {
                System.out.println("Image Processor Event was recieved");
                Assert.assertEquals(evt.getPreviousImageProcessorReference(), prevIp);
                Assert.assertEquals(evt.getCurrentImageProcessorReference(), propImPlus_.getProcessor());
                Assert.assertTrue( (evt.getEventFlags() & PropertyNotifyingImagePlus.ImageProcessorChangeEvent.SNAPSHOT_CHANGED) != 0 );
                return true;
            }
            
            
        };
        
        propImPlus_.RegisterToEventBus(reciever);
        
        propImPlus_.trimProcessor();

    }

    /**
     * Test of getMask method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testGetMask() {
        System.out.println("getMask");
        PropertyNotifyingImagePlus instance = null;
        ImageProcessor expResult = null;
        ImageProcessor result = instance.getMask();
        assertEquals(result, expResult);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of updateImage method, of class PropertyNotifyingImagePlus.  Not Really testable Without Integration
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testUpdateImage() {
        System.out.println("updateImage");
        PropertyNotifyingImagePlus instance = null;
        instance.updateImage();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setType method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testSetType() {
        System.out.println("setType");
        int type = 0;
        PropertyNotifyingImagePlus instance = null;
        instance.setType(type);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSlice method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testSetSlice() {
        System.out.println("setSlice");
        int slice = 0;
        PropertyNotifyingImagePlus instance = null;
        instance.setSlice(slice);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getProcessor method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testGetProcessor() {
        System.out.println("getProcessor");
        PropertyNotifyingImagePlus instance = null;
        ImageProcessor expResult = null;
        ImageProcessor result = instance.getProcessor();
        assertEquals(result, expResult);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setTitle method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testSetTitle() {
        System.out.println("setTitle");
        String title = "";
        PropertyNotifyingImagePlus instance = null;
        instance.setTitle(title);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setRoi method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testSetRoi() {
        System.out.println("setRoi");
        Roi newRoi = null;
        boolean updateDisplay = false;
        PropertyNotifyingImagePlus instance = null;
        instance.setRoi(newRoi, updateDisplay);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of deleteRoi method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testDeleteRoi() {
        System.out.println("deleteRoi");
        PropertyNotifyingImagePlus instance = null;
        instance.deleteRoi();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of createNewRoi method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testCreateNewRoi() {
        System.out.println("createNewRoi");
        int sx = 0;
        int sy = 0;
        PropertyNotifyingImagePlus instance = null;
        instance.createNewRoi(sx, sy);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of restoreRoi method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testRestoreRoi() {
        System.out.println("restoreRoi");
        PropertyNotifyingImagePlus instance = null;
        instance.restoreRoi();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of flush method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testFlush() {
        System.out.println("flush");
        PropertyNotifyingImagePlus instance = null;
        instance.flush();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of copy method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testCopy() {
        System.out.println("copy");
        boolean cut = false;
        PropertyNotifyingImagePlus instance = null;
        instance.copy(cut);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of paste method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testPaste() {
        System.out.println("paste");
        PropertyNotifyingImagePlus instance = null;
        instance.paste();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setDisplayRange method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testSetDisplayRange_double_double() {
        System.out.println("setDisplayRange");
        double min = 0.0;
        double max = 0.0;
        PropertyNotifyingImagePlus instance = null;
        instance.setDisplayRange(min, max);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setDisplayRange method, of class PropertyNotifyingImagePlus.
     */
    @Test( enabled = false, dependsOnGroups = "init" )
    public void testSetDisplayRange_3args() {
        System.out.println("setDisplayRange");
        double min = 0.0;
        double max = 0.0;
        int channels = 0;
        PropertyNotifyingImagePlus instance = null;
        instance.setDisplayRange(min, max, channels);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
