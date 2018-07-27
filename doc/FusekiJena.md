# Connecting Fuseki to Jena

For debugging purposes it is sometimes useful to see the triples that are in the Jena database. Jena itself doesn't come with a front end although there are command line applications that can interact with the Jena database. One option is to use Fuseki which is a web front end to Jena. Note you can't run Fuseki and SAS at the same time as Jena only allows one JVM access to the data files. To use Fuseki you can do the following:

 * Download Fuseki from https://jena.apache.org/download/.
 * Unzip and install as specified in the Fuseki instructions.
 * Stop SAS.
 * Either copy the Jena data files (usually in the `data` directory although the location of this file can be configured using the [sas.properties](../src/main/webapp/WEB-INF/sas.properties)) or stop SAS until you have finished using Fuseki.
 * Run Fuseki and connect to SAS Jena directory:

```
java -jar fuseki-server.jar --loc=~/development/SimpleAnnotationServer/data /sas
```

It is also possible to save the Jena database files during the unit testing if they failed by setting `retain_failed_data` to `true` in [test.properties](../src/test/resources/test.properties).
