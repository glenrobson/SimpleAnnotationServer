## Running SAS on Windows

Download and install java: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
Use the default install location `C:\Program Files\Java\jre1.8.0_181`

Download maven https://maven.apache.org/download.cgi
Select the latest .zip file
Open zip file and copy apache-maven-x.x.x directory to somewhere useful. I'm using  `C:\Users\user\Documents\apache-maven-3.5.4`.

Open up the command prompt and check java and mvn are installed:
```
C:\Users\Administrator\Documents\apache-maven-3.5.4>java -version
java version "1.8.0_181"
Java(TM) SE Runtime Environment (build 1.8.0_181-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.181-b13, mixed mode)

C:\Users\Administrator\Documents\apache-maven-3.5.4>mvn -version
'mvn' is not recognized as an internal or external command,
operable program or batch file.

C:\Users\Administrator\Documents\apache-maven-3.5.4>javac -version
'javac' is not recognized as an internal or external command,
operable program or batch file.
```

From the command above you can see java is installed but `mvn` and `javac` is not found on the path. To update your path you need to find your `System Properties` depending on which version of Windows you are using will depend how you do this. On Windows Server 2016 I did the following:

 * Right clicked on the windows dock item on the bottom left.
 * Selected `System`
 * Clicked on `Advanced System Settings`
 * Then clicked `Environment Settings` which was a button at the bottom of the screen.
 * This gave me a screen which allowed me to edit `PATH`, `TMP` and `TEMP`
 * I then added the following to the PATH: `C:\Users\Administrator\Documents\apache-maven-3.5.4\bin`
 * and also the following to find javac `c:\Program Files\Java\jdk1.8.0_181\bin`

Now close the cmd prompt and restart it.  Now if I check if I have maven installed I get:

```
C:\Users\Administrator>mvn -version
Apache Maven 3.5.4 (1edded0938998edf8bf061f1ceb3cfdeccf443fe; 2018-06-17T18:33:14Z)
Maven home: C:\Users\Administrator\Documents\apache-maven-3.5.4\bin\..
Java version: 1.8.0_181, vendor: Oracle Corporation, runtime: C:\Program Files\Java\jre1.8.0_181
Default locale: en_US, platform encoding: Cp1252
OS name: "windows server 2016", version: "10.0", arch: "amd64", family: "windows"
```

Download zip version of SAS.
Extract to a folder.
open cmd
cd into that folder

run the following so maven knows where to find java:

```
set JAVA_HOME=c:\Progra~1\Java\jdk1.8.0_181
``

then run:

```
mvn jetty:run
```

This should download the libraries required and start mirador connected to the annotation store at:

http://localhost:8888
