# Development Tips & Tricks


## Jetty auto deploy changes

If you make any changes to the HTML, Javascript, css (anything apart from the Java files) Jetty will automatically pick up the changes so a browser refresh should get the new content. There is no need to restart jetty. If you change any Java files running mvn compile while also running mvn jetty:run jetty will ensure jetty picks up any changes

## Updating Mirador

Mirador is included in the SimpleAnnotationServer to aid quick deployment. If you would like to update Mirador follow the instructions on the [Mirador Site](https://github.com/IIIF/mirador) and copy everything from mirador/build/mirador/* to [src/main/webapp/mirador-2.6.1](../src/main/webapp/mirador-2.6.1).

## Overloading config using Environment variables

It is possible to overload any of the configuration specified in [src/main/webapp/WEB-INF/sas.properties](../src/main/webapp/WEB-INF/sas.properties) using environmental variables by prepending `SAS_` to any of the properties in sas.properties e.g:

```
export SAS_store="solr"
```

This is useful to configure the backend properties while deploying docker instances to AWS.

## Docker

The SimpleAnnotationServer supports multiple backends and configuration and to aid development and deployment there are a number of docker files in the [docker](../docker) directory. These docker directories are explained in detail below but to simplify running these docker scripts there is a [runDocker](../runDocker.sh) script which takes an argument of either `Jena`, `Solr` or `Cloud` and will start a docker instance with that configuration. An example is below:

```
./runDocker.sh Jena
```

### [sas-tomcat](../docker/sas-tomcat)

This is a simple docker with Jena as the backend and SAS running in tomcat. To access the SAS service you can build and run this docker using:

```
docker build -t sas:latest -f docker/sas-tomcat/Dockerfile . && docker run -v /tmp/data:/usr/src/app/data --rm -p 8888:8080 --name sas sas:latest
```

Then go to:

[http://localhost:8888/sas/](http://0.0.0.0:8888/sas/)

### [sas-solr](../docker/sas-solr)

This is a docker with SOLR as the backend and SAS running in tomcat. It uses docker-compose to start SOLR then starts a second docker with SAS and tomcat that connects to it. To run this instance:

```
docker-compose -f docker/sas-solr/docker-compose.yml --project-directory . up
```

If you make changes to the [Dockerfile](../docker/sas-solr/Dockerfile) you can rebuild it using:

```
docker-compose -f docker/sas-solr/docker-compose.yml --project-directory . build
```

For development it can also be useful to start SOLR and connect to it with the version of SAS that runs with `mvn jetty:run`. To start just the SOLR docker run:

```
docker-compose -f docker/sas-solr/docker-compose.yml --project-directory . up solr
```

Then edit the [sas.properties](../src/main/webapp/WEB-INF/sas.properties) and uncomment the solr store:

```
# store=solr
# solr_connection=http://solr-host/solr/annotations
```

Note two cores will be created `annotations` and `test-annotations` for unit tests. To access SOLR once its running with docker-compose go to:

[http://localhost:8983/](http://0.0.0.0:8983/)


## Creating releases

This uses jetty-runner to create a single line command to run SAS. To generate the zip file to add to the release run the following:

```
./scripts/mkRelease.sh
```

This will generated a zip file containing the jetty-runner jar and SAS jar and you can then run SAS like:

```
java -jar dependency/jetty-runner.jar --port 8888 simpleAnnotationStore.war
```

