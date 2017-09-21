# FiducialLiveTracking_MavenFriendly
Maven Structured Project for Continued Development of Tracking Fiducial Plugin and autofocusing mechanisms

Installation:

  This Project was produced within the NetBeans IDE and as such, will be explained 
how to open the Project in regards to that IDE for new contributors.

      -The Project should be created as a Maven Project and then linked to the Remote Repository of this README.

      -Due to Third Party jars that are not part of the Maven Repository, dependencies on 
       the Micromanager Source Code must be manually added in.  

      -In Netbeans This can be accomplished by Right-clicking A missing Dependency, 
       and then clicking "manually install Artifact".  Find the jar in your System folders.
       (Note: To develop for the current downloaded version, find it in your installation folder)

            List of Current Dependency Jar Relationships:

            MMJ_-1.4.22 - {Micromanager1.4.22 Installation Dir}\plugins\MicroManager\MMJ_.jar

            gaussianfit-1.4.22 - {Micromanager1.4.22 Installation Dir}\mmplugins\Acquisition_Tools\Gaussian.jar

            mmcorej-1.4.22 - {Micromanager1.4.22 Installation Dir}\plugins\MicroManager\MMCoreJ.jar

      -For the Previous Step, if you are not Using NetBeans, make sure to use the maven-install-plugin:install-file
       and then list your dependency according to whatever Group-name, artifact-id, and Version you see fit.

      -Additionally, There is a TODO to compile the source and javadoc files as well and provide the already included dependencies
       in a repository folder for easier access.

      - Finally, to run and Debug Directly from NetBeans, modify the UserSystem.properties file
        so that MMRunEnvironmentDir = {Absolute Path to Micromanager Installation).  This will copy any builds
        to the directory.  IMPORTANT NOTE: Escape BackSlashes (i.e. \ is denoted by \\)

      -NetBeans Property Settings under Project Properties-> Run, may have Working Directory Set to the same directory (via Browse)
       and then Main Class set to ij.ImageJ.  If the Previous Step has been finished, every run call after a build, 
       whether opening the program or calling it from NetBeans, will have the most recent Build.


