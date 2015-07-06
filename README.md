# SimpleAnnotationServer
This is an Annotation Server which is compatible with [IIIF](http://iiif.io) and [Mirador](https://github.com/IIIF/mirador). This Annotation Server includes
a copy of Mirador so you can get started creating annotations straight away. The annotations are stored as linked data in an [Apache Jena](https://jena.apache.org/) triple store. 

## Getting Started
**Requires:**
 * Java 1.7
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

## Missing Functions

Note this project doesn't currently contain Authentication or Versioning of Annotations. If your looking for these functions have a look at [Triannon from Stanford](https://github.com/sul-dlss/triannon) or [Catch from Harvard](https://github.com/annotationsatharvard/catcha). 

## Thanks

Thanks to:

 * [azaroth42](https://github.com/azaroth42) for help with JsonLd framing and other useful tips. 
 * [Illtud](https://github.com/illtud) and [Paul](https://twitter.com/sankesolutions) for help with testing and fixing build problems. 
 * [Dan](https://twitter.com/Surfrdan) for introducing me to Apache Jena

and finally thanks to the IIIF and Mirador communities which make all this cool stuff possible.
