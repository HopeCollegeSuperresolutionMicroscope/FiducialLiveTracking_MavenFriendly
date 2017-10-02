# FiducialLiveTracking_MavenFriendly
Maven Structured Project for Continued Development of Tracking Fiducial Plugin and autofocusing mechanisms

Installation:

  This Project was produced within the NetBeans IDE and as such, will contain caveats about 
how to open the Project in regards to that IDE for new contributors.

      -Due to Third Party jars that are not part of the Maven Repository, dependencies on 
       the Micromanager Source Code must be manually added in.  These dependencies should be listed in
       the src/3rdpartylib/ directory.

      INSTALLING THE 3RD PARTY JARS

      -If in Netbeans this can be accomplished by expanding the dependencies, right-clicking a missing Dependency, 
       and then clicking "manually install Artifact".  Find the jar in the 3rdpartylib/ directory.

            List of Current Dependency Jar Relationships:

            MMJ_-1.4.22 - {Micromanager1.4.22 Installation Dir}\plugins\MicroManager\MMJ_.jar

            gaussianfit-1.4.22 - {Micromanager1.4.22 Installation Dir}\mmplugins\Acquisition_Tools\Gaussian.jar

            mmcorej-1.4.22 - {Micromanager1.4.22 Installation Dir}\plugins\MicroManager\MMCoreJ.jar

      -If you are not Using NetBeans, make sure to use the maven-install-plugin:install-file to install 
       the jar to your local repository and use the groupId, artifactId, and version already listed in the pom.

      -Additionally, There is a TODO to compile the source and javadoc files as well and provide them in the 3rdpartylib
       folder as well.  This has not been done.

      RUNNING AND DEBUGGING THE PLUGIN THROUGH CURRENT MICRO-MANAGER INSTALLATION

      - The file UserSystem.properties in the project directory is meant to be set 
        for use with your local installation of micromanager:

            MMRunEnvironmentDir: The path of the directory for Micro-Manager.  Typically (C:\\Program Files\\Micro-Manager-1.4)

            debug.address:  Port Address on which to listen for the remote debugger.  Whatever it's set to
                            should be the address the debugger is attached to after running debug.

      - Two Build Profiles Exist for this project:

            debug-attach-remote: Compiles and Builds your current code into the micro-manager installation
                                 and waits for a remote debugger to attached.

            run:  Compiles and Builds your current code into micro-manager installation and starts it.

      - If in Netbeans, right click project->properties. Select the Actions Tab and select debug and run.
        In each action tab set 
                    Execute Goals: process-classes install
                    Activate Profiles: [corresponding profile from above]
        From NetBeans, when you select debug, micro-manager will wait until you select Debug->attach Debugger (set the port to the 
           debug.address value you specified in UserSystem.properties)

      -[Alternative Netbeans setup] To use Default Run Tab in Project->Properties, select the micro-manager installation
        directory as the working dir and set the main class to ij.ImageJ.  Under Actions, select Debug and you must 
        explicitly specify the ij.jar path in the micro-manager installation in place of the %classpath argument.    

      - If not in Netbeans, the command: mvn install -P [specific profile] should do either a debug or run operation.
        Recall that you must attach the debugger to the jvm you're running with the debug profile, or micro-manager
        won't start.




