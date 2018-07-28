# SimpleAnnotationServer
This is an Annotation Server which is compatible with [IIIF](http://iiif.io) and [Mirador](https://github.com/IIIF/mirador). This Annotation Server includes
a copy of Mirador so you can get started creating annotations straight away. The annotations are stored as linked data in an [Apache Jena](https://jena.apache.org/) triple store by default. It is also possible to store the annotations in [Sesame](doc/Sesame.md) or [SOLR](doc/Solr.md).

**Now supports IIIF Search API in both the [Universal Viewer](http://universalviewer.io/) and [Mirador](http://projectmirador.org/)**

For details see [IIIF Search](doc/IIIFSearch.md)

## Getting Started
**Requires:**
 * Java 1.8 (javac also required so ensure to install a JDK (Java Development Kit) not just a Java Run Time)
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

## Further guides

SimpleAnnotationServer or SAS aims to fulfil two main use cases; a easy to install and get going annotation to work with Mirador and also allow it to be deployed to production. The guides below are split into user guides for those that want to use some of the more advanced features of SAS and guides for developers to set it up for production:

### User guides

 * [Installing SAS on Windows](Windows.md)
 * [Adding your own Manifests](doc/NewManifests.md)
 * [Populating the Annotation Store with IIIF Annotation List](doc/PopulatingAnnotations.md)
 * [Using the IIIF Search API with SAS](IIIFSearch.md)
 * [Downloading annotations and linking to static Manifests](DownloadAnnotations.md)

### Developer guides

 * [Developing Mirador with SimpleAnnotationServer](doc/DevGuide.md)
 * [Connecting Fuseki with Jena to view triple store](doc/FusekiJena.md)
 * [Deploying to tomcat](doc/tomcat.md)
 * [Using the Sesame RDF store](doc/Sesame.md)
 * [Remote Annotation Store](doc/RemoteStore.md)
 * [Migrating annotations from one backend to another.](doc/MigratingData.md)

## Docker installs

There is a docker file in [docker/latestMiradorExternal](docker/latestMiradorExternal) and this is intended as a fully installed setup for SAS which includes:
 * External Mirador 2.6.0 hosted on Apache
 * SAS setup to use SOLR cloud
 * Preloaded with a searchable Newspaper

Other out of the box configurations will be added to aid testing of the different possible setups of SAS. To start this docker instance run:

```
cd docker/latestMiradorExternal
./runDocker.sh [dev]
```
Add the `dev` parameter if you want the docker file to pickup the latest changes to SAS.

## Roadmap

Note this project doesn't currently contain Authentication although it is possible to secure the SAS web application with a single username and password using Apache forwarding. Plans for future developments include:

 * Jena as the primary datastore and ElasticSearch as a side car for fast searching.
 * Deployment on AWS
 * Authentication – Shibboleth, Facebook/Google
 * Annotation versioning
 * Web annotations

Please add an issue if there are other enhancements which would be useful.

## Thanks

Thanks to:

 * [azaroth42](https://github.com/azaroth42) for help with JsonLd framing and other useful tips.
 * [Illtud](https://github.com/illtud) and [Paul](https://twitter.com/sankesolutions) for help with testing and fixing build problems.
 * [Dan](https://twitter.com/Surfrdan) for introducing me to Apache Jena and SOLR documentation.
 * [regisrob](https://github.com/regisrob) for help with the Mirador within code.

and finally thanks to the IIIF and Mirador communities which make all this cool stuff possible.
