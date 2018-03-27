# FiducialLiveTracking_MavenFriendly
Maven Structured Project for Continued Development of Tracking Fiducial Plugin and autofocusing mechanisms

## Installation:

  This Project was produced within the NetBeans IDE and as such, will contain caveats about 
how to open the Project in regards to that IDE for new contributors.

* Due to Third Party jars that are not part of the Maven Repository, dependencies on 
the Micromanager Source Code must be manually added in.  These dependencies should be listed in
the src/3rdpartylib/ directory.

* ### INSTALLING THE 3RD PARTY JARS
   If in Netbeans this can be accomplished by expanding the dependencies, right-clicking a missing Dependency, 
       and then clicking "manually install Artifact".  Find the jar in the 3rdpartylib/ directory.
   List of Current Dependency Jar Relationships:

       * MMJ_-1.4.22 - Micromanager1.4.22 Installation Dir}\plugins\MicroManager\MMJ_.jar

       * gaussianfit-1.4.22 - Micromanager1.4.22 Installation Dir}\mmplugins\Acquisition_Tools\Gaussian.jar

       * mmcorej-1.4.22 - Micromanager1.4.22 Installation Dir}\plugins\MicroManager\MMCoreJ.jar

       If you are not Using NetBeans, make sure to use the maven-install-plugin:install-file to install 
       the jar to your local repository and use the groupId, artifactId, and version already listed in the pom.

       Additionally, There is a TODO to compile the source and javadoc files as well and provide them in the 3rdpartylib
       folder as well.  This has not been done.

* ### BUILD OPTIONS

    The file UserSystem.properties in the project directory is meant to be set 
        for use with your local installation of micromanager:

    **For Testing as Micro-manager Plugin**

       MMRunEnvironmentDir: The path of the directory for Micro-Manager.  Typically (C:\\Program Files\\Micro-Manager-1.4)

    **For Testing as ImageJ Plugin]**

       IJRunEnvironmentDir: The Directory for a non-Micro-Manager installation of ImageJ 
       
       IJApp: The name of the ImageJ jar source (this will be located in IJRunEnvironmentDir/jar) and
                   of the format ij-*.jar

       debug.address:  Port Address on which to listen for the remote debugger.  Whatever it's set to
                            should be the address the debugger is attached to after running debug.

## RUNNING AND DEBUGGING THE PLUGIN THROUGH CURRENT MICRO-MANAGER INSTALLATION

###   Two Build Profiles Exist for this purpose:

      MM-debug-attach-remote: Compiles and Builds the current code into the target directory and
                                    the micro-manager installation and waits for a remote debugger to be attached to start

      MM-run-plugin:  Compiles and Builds your current code into into the target directory and the
                            micro-manager installation and starts it.

## RUNNING AND DEBUGGING THE PLUGIN THROUGH CURRENT IMAGEJ INSTALLATION

###   Two Build Profiles Exist for this purpose:

      ImageJ-run-plugin: Compiles and Builds the Micromanager code and a ImageJ plugin variant
                               into the target directories and the ImageJ variant into the specified installation of ImageJ
                               and starts it.

      ImageJ-debug-attach-remote: Compiles and Builds the Micromanager code and a ImageJ plugin variant
                               into the target directories and the ImageJ variant into the specified installation of ImageJ
                               and waits for a remote debugger to be attached to start

## EXAMPLES OF SETTING BUILD PROFILES FOR TESTING

   * If in Netbeans, right click project->properties. Select the Actions Tab and select debug and run.

        In each action tab set 
                    Execute Goals: process-classes install
                    Activate Profiles: {corresponding profile from above}
                    (IMPORTANT) Set Properties: Delete all text, if not, runtime classpath loading will throw errors for almost all micro-manager plugins

        From NetBeans, when you select debug, micro-manager will wait until you select Debug->attach Debugger (set the port to the 
           debug.address value you specified in UserSystem.properties)

   * **For Ease of Use in Netbeans** Using more than the Default Run and Debug Actions will require you to invoke custom actions from submenus.
        You can alternatively assign a custom button to another custom action that will invoke the above profile from the toolbar.
        Please see this [Stackoverflow Link](https://stackoverflow.com/questions/9458928/invoking-actions-other-than-build-and-clean-build) 

   * If not in Netbeans, the command: mvn install -P {specific profile} should do either a debug or run operation.
        Recall that you must attach the debugger to the jvm you're running with the debug profile, since micro-manager
        will block on a listener.




