/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

/**
 *  Interface for Registering Objects to listen for Updates from a model that extends
 *  the ModelUpdateDispatcher class.  This Interface is specifically designed for 
 *  JavaSwing classes that tend to roll controllers and views together.  Since the 
 *  implication is then that several Graphical components may be encapsulated in one class
 *  (i.e. a form/jFrame with all it's elements implementing listeners), guiEnable() 
 *  is used to display the entire object visually to the user.  Additionally, to avoid
 *  initialization order, the callback onRegisteredToModel() passes the model that would traditionally
 *  be stored in a controller.  It is recommended that with GUI (View-controller) listeners
 *  that the guiEnable() method perform a check for a non-null reference to the model
 *  if there is an anticipation of repeating registering and unregistering.  In this way,
 *  the user should not be presented a GUI unless it has been appropriately registered to a model.
 * <p>
 *  Provides four methods for the model to call:
 *  An update method with the ability for passing integer specified events, a
 *  guiEnable method for toggling the View's GUI elements from the model or another
 *  context, and two callback methods for (un)registering a reference to any object the implementing class is registered to.
 * 
 * @see edu.hope.superresolution.models.ModelUpdateDispatcher
 * 
 * @author Justin Hanselman
 */
public interface ModelUpdateListener {
    
    //This event should be part of the Model Constants
    /**
     *  Method Called By Any ModelUpdateDispatcher This listener is registered to, to update it.
     *  Each Object should have its own set of public static final event values that can 
     *  be used to make decisions about updating.  It is the burden of the listener to 
     *  expect and decide on the events.
     * 
     * @param caller The Caller ModelUpdateDispatcher that dispatched the event.  This should be used to 
     *               differentiate between different ModelUpdateDispatcher classes that may 
     *               have overlapping integer constants.
     * <pre>
     *      if( caller instanceof ExpectedInstance) {
     *          switch( event ) {
     *              case ExpectedInstance.publicEvtkey1:
     *                  //do a method here
     *                  break
     *          }
     *      } else if ( caller instanceof OtherExpectedInstance ) {
     *         //same switch code but with OtherExpectedInstance
     *      }
     * </pre>
     * @param event An integer that is defined by the model that this is registered to
     *              that is public static final.
     * 
     * @see edu.hope.superresolution.models.ModelUpdateDispatcher#dispatch(int) 
     */
    public void update( ModelUpdateDispatcher caller, int event );
    
        
    /**
     * This is a callback that is called when the implementating class is registered to
     * to a modelUpdateDispatcher through that dispatchers registerModelUpdateListener method.
     * Use this method to circumvent initialization issues of registering an object in the constructor.
     * <p>
     * It is recommended that this callback is implemented as in the update(), with 
     * an anticipation of the models to be registered and a qualifying of their reference
     * before deciding what to do with the reference. 
     * <p>
     * Not thread safe, so please anticipate testing for an object reference in a reliable manner
     * if multiple registrations and order is important to the implementing class.
     * <p>
     *  Note: It is not recommended that this callback be called outside of the context of 
     *  ModelUpdateDispatcher.
     * 
     * @param registeredModel The object instance whose registerModelUpdateListener() method this
     *                        implementing class was registered through.
     * 
     * @see edu.hope.superresolution.models.ModelUpdateDispatcher#registerModelListener(edu.hope.superresolution.models.ModelUpdateListener) 
     */
    public void onRegisteredToModel( ModelUpdateDispatcher registeredModel );
    
    /**
     *  Same motivation as onRegisteredToModel().  This callback is called to clean up any models
     *  that this implementing class is unregistered to by unregisterModelUpdateListener.
     * <p>
     *  This callback is expected to clean up model references.  There may be instances where the 
     *  reference to a model is still desired, despite no longer listening to its update dispatches.
     *  In this case, anticipate having an empty implementation.
     * <p>
     *  Note: It is not recommended that this callback be called outside of the context of 
     *  ModelUpdateDispatcher.
     * 
     * @param unregisteredModel the object instance whose unregisterModelUpdateListener() method
     *                          this implementing class was successfully unregistered through.
     * 
     * @see edu.hope.superresolution.models.ModelUpdateDispatcher#unregisterModelListener(edu.hope.superresolution.models.ModelUpdateListener) 
     */
    public void onUnregisteredToModel( ModelUpdateDispatcher unregisteredModel );
    
     /**
     *  Enables the Visual Elements of this object.  This is meant to be called by 
     *  any ModelUpdateDispatcher this is registered to, or the construction context
     *  so that construction and display can be separated or called in more complicated
     *  structures.
     * <p>
     *  Given the nature of this interface, it is recommended to exclusively use this function to display
     *  the graphical components of these sub-classes.  Since this is designed for Swing classes
     *  which are notoriously mixed View-Controllers, any model to be referenced in the GUI should be checked 
     *  to see if they are stored.  If a class makes use of multiple models and some are
     *  not necessary, this function should perform select guiEnabling.  The models that are referenced
     *  are intended to be retrieved by onRegisteredToModel() and therefore, may not be initialized
     *  for a sub class if there is an expectation of multiple registration and unregistration.
     * <p>
     *  For non-graphical listeners, it is expected to not throw exceptions.
     * 
     * @param enable <code>true</code> enables the gui elements associated with the listener
     *               <code>false</code> disables the gui elements associated with the listener
     * 
     * @see edu.hope.superresolution.models.ModelUpdateDispatcher#enableListenerGUI(edu.hope.superresolution.models.ModelUpdateListener, boolean) 
     * @see edu.hope.superresolution.models.ModelUpdateDispatcher#enableAllListenerGUIs(boolean) 
     */
    public void guiEnable( boolean enable );

}
