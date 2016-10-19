# SimpleAnnotationServer
This is an Annotation Server which is compatible with [IIIF](http://iiif.io) and [Mirador](https://github.com/IIIF/mirador). This Annotation Server includes
a copy of Mirador so you can get started creating annotations straight away. The annotations are stored as linked data in an [Apache Jena](https://jena.apache.org/) triple store by default. It is also possible to store the annotations in [Sesame](doc/Sesame.md) or [SOLR](doc/Solr.md). 

**Now supports IIIF Search API**

For details see [IIIF Search](doc/IIIFSearch.md)

## Getting Started
**Requires:**
 * Java 1.8
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

For further details on the SimpleAnnotationServer see:

 * [Adding your own Manifests](doc/NewManifests.md) 
 * [Populating the Annotation Store with IIIF Annotation List](doc/PopulatingAnnotations.md)
 * [Developing Mirador with SimpleAnnotationServer](doc/DevGuide.md)
 * [Deploying to tomcat](doc/tomcat.md)
 * [Using the Sesame RDF store](doc/Sesame.md)
 * [Remote Annotation Store](doc/RemoteStore.md)
 * [Migrating annotations from one backend to another.](doc/MigratingData.md)

## Roadmap

Note this project doesn't currently contain Authentication althought it is possible to secure the SAS web application with a single username and password using Apache forwarding. Plans for future developments include:

 * Authentication – Shibboleth, Facebook/Google
 * Annotation versioning
 * Web annotations

Please add an issue if there are other enhancements which would be useful. 

## Other Annotations stores

Other annotation stores that work with Mirador include [Triannon from Stanford](https://github.com/sul-dlss/triannon) or [Catch from Harvard](https://github.com/annotationsatharvard/catcha). 

## Thanks

Thanks to:

 * [azaroth42](https://github.com/azaroth42) for help with JsonLd framing and other useful tips. 
 * [Illtud](https://github.com/illtud) and [Paul](https://twitter.com/sankesolutions) for help with testing and fixing build problems. 
 * [Dan](https://twitter.com/Surfrdan) for introducing me to Apache Jena and SOLR documentation.
 * [regisrob](https://github.com/regisrob) for help with the Mirador within code.

and finally thanks to the IIIF and Mirador communities which make all this cool stuff possible.
