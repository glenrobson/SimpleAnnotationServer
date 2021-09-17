# build stage
FROM maven:3-jdk-11 AS buildstage
WORKDIR /usr/src/sas
COPY . /usr/src/sas
ARG MVN_ARGS="-DskipTests -q"
# build SAS using maven
RUN mvn $MVN_ARGS package

# runnable container stage
FROM tomcat:9-jre11 AS runstage
# remove tomcat default webapps and create data directory
RUN rm -rf /usr/local/tomcat/webapps/*
# copy SAS from build image
COPY --from=buildstage /usr/src/sas/target/simpleAnnotationStore /usr/local/tomcat/webapps/ROOT
# copy properties
COPY docker/sas-solr-cloud/sas.properties /usr/local/tomcat/webapps/ROOT/WEB-INF

# Used for testing:
COPY . /usr/src/sas
RUN apt-get update && apt-get -y install maven
COPY docker/sas-solr-cloud/sas.properties /usr/src/sas/src/test/resources/test.properties

# use default port and entrypoint
