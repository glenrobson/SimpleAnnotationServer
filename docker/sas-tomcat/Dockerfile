# build stage
FROM maven:3-jdk-11 AS buildstage
WORKDIR /usr/src/sas
COPY . /usr/src/sas
ARG MVN_ARGS="-DskipTests"
# build SAS using maven
RUN mvn $MVN_ARGS package

# runnable container stage
FROM tomcat:9-jre11 AS runstage
# remove tomcat default webapps and create data directory
RUN rm -r /usr/local/tomcat/webapps/* && \
  mkdir /annotation-data
# copy SAS from build image
COPY --from=buildstage /usr/src/sas/target/simpleAnnotationStore /usr/local/tomcat/webapps/sas
# copy properties
COPY docker/sas-tomcat/sas.properties /usr/local/tomcat/webapps/sas/WEB-INF

# use default port and entrypoint
