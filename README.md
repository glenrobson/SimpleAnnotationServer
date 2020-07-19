[![Build Status](https://travis-ci.org/glenrobson/SimpleAnnotationServer.svg?branch=master)](https://travis-ci.org/glenrobson/SimpleAnnotationServer)

# SimpleAnnotationServer
This is an Annotation Server which is compatible with [IIIF](http://iiif.io) and [Mirador](https://github.com/IIIF/mirador). This Annotation Server includes
a copy of Mirador so you can get started creating annotations straight away. The annotations are stored as linked data in an [Apache Jena](https://jena.apache.org/) triple store by default. It is also possible to store the annotations in [SOLR](doc/Solr.md).

**Now supports IIIF Search API in both the [Universal Viewer](http://universalviewer.io/) and [Mirador](http://projectmirador.org/)**

For details see [IIIF Search](doc/IIIFSearch.md)

## Getting Started
**Requires:**
 * [Java 11](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html)

To verify you have the correct package installed, you can run the following command from a terminal or command prompt:
```
$ java -version
# java version "1.11.0_102"
``` 
You should see version `1.11.x`. For more information on the install options see:

### Step 1: Download

Download the pre-built SimpleAnnotationStore by going to the Releases page:

 * https://github.com/glenrobson/SimpleAnnotationServer/releases

Download the latest `sas.zip` which might be hidden under the **Assets** drop down.  

### Step 2: Extract Zip file

Extract the zip file, on a Mac double clicking on the file will extract it. **On windows** make sure the zip file is extracted by right clicking on the zip file and selecting uncompress.

### Step 3: Run the SimpleAnnotationServer

Open up a terminal or command prompt and do the following:

```
cd extracted_sas_directory
java -jar dependency/jetty-runner.jar --port 8888 simpleAnnotationStore.war
```

You should now be able to use the SimpleAnnotationServer by going to:

[http://0.0.0.0:8888/index.html](http://0.0.0.0:8888/index.html)


## Further guides

SimpleAnnotationServer or SAS aims to fulfil two main use cases; an easy to install and get going annotation server to work with Mirador and also allow it to be deployed to production. The guides below are split into user guides for those that want to use some of the more advanced features of SAS and guides for developers to set it up for production:

### User guides

 * [Installing SAS on Windows](doc/Windows.md)
 * [Adding your own Manifests](doc/NewManifests.md)
 * [Populating the Annotation Store with IIIF Annotation List](doc/PopulatingAnnotations.md)
 * [Using the IIIF Search API with SAS](doc/IIIFSearch.md)
 * [Downloading annotations and linking to static Manifests](doc/DownloadAnnotations.md)

## Local Development 
**Requires:**
 * Java 11 with JDK (Java Development Kit, not just the Runtime)
 * [maven](https://maven.apache.org/)

To begin working with Mirador and the Simple Annotation Server do the following:

 * Download code

```git clone https://github.com/glenrobson/SimpleAnnotationServer.git```

 * Move into the SimpleAnnotationServer directory.

```cd SimpleAnnotationServer```

 * Start the jetty http server

```mvn jetty:run```

 * Start Annotating

Navigate to [http://localhost:8888/index.html](http://localhost:8888/index.html)

You should now see Mirador with the default example objects. You can choose any manifest to start annotating.

## Docker installs

There are a number of docker files in [docker](docker/) with different backend configurations. For details see [Docker dev details](doc/DevGuide.md#Docker) but to run the basic SAS instance with a Jena database you can run the following script:

```
./runDocker.sh Jena
```

which will use this Dockerfile [docker/sas-tomcat/Dockerfile](docker/sas-tomcat/Dockerfile)

### Developer guides

 * [Developing with SimpleAnnotationServer](doc/DevGuide.md) (Developing locally, Docker installs, deployment tricks and Jetty config)
 * [Connecting Fuseki with Jena to view triple store](doc/FusekiJena.md)
 * [Deploying to tomcat](doc/tomcat.md)
 * [Using the Sesame RDF store](doc/Sesame.md) **DEPERICATED**
 * [Remote Annotation Store](doc/RemoteStore.md)
 * [Migrating annotations from one backend to another.](doc/MigratingData.md)

## Roadmap

Note this project doesn't currently contain Authentication although it is possible to secure the SAS web application with a single username and password using Apache forwarding. Plans for future developments include:

 * Add ElasticSearch as a backend.
 * Easy Deployment on AWS with ElasticSearch
 * Web annotations
 * Removing support for Sesame 
 * Support for Mirador 3
 * Authentication – Shibboleth, Facebook/Google
 * Annotation versioning

Please add an issue if there are other enhancements which would be useful.

## Thanks

Thanks to:

 * [azaroth42](https://github.com/azaroth42) for help with JsonLd framing and other useful tips.
 * [Illtud](https://github.com/illtud) and [Paul](https://twitter.com/sankesolutions) for help with testing and fixing build problems.
 * [Dan](https://twitter.com/Surfrdan) for introducing me to Apache Jena and SOLR documentation.
 * [regisrob](https://github.com/regisrob) for help with the Mirador within code.

and finally thanks to the IIIF and Mirador communities which make all this cool stuff possible.
