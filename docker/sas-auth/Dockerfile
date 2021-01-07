# build stage
FROM maven:3-jdk-11 AS buildstage
WORKDIR /usr/src/sas
COPY . /usr/src/sas
ARG MVN_ARGS="-DskipTests"
# build SAS using maven
RUN mvn $MVN_ARGS package

# runnable container stage
FROM tomcat:9-jre11 AS runstage
ARG AWS_DEFAULT_REGION
ARG AWS_CONTAINER_CREDENTIALS_RELATIVE_URI
ARG AUTH_JSON_LOCATION
# remove tomcat default webapps and create data directory
RUN rm -r /usr/local/tomcat/webapps/* 
# copy SAS from build image
COPY --from=buildstage /usr/src/sas/target/simpleAnnotationStore /usr/local/tomcat/webapps/ROOT
# copy properties
COPY docker/sas-auth/sas.properties /usr/local/tomcat/webapps/ROOT/WEB-INF

# Download auth config
# Install the AWS CLI
RUN apt-get update && \
        apt-get -y install awscli 
RUN aws --region eu-west-2 s3 cp $AUTH_JSON_LOCATION /usr/local/tomcat/webapps/ROOT/WEB-INF/
# For testing locally:
#COPY docker/sas-auth/auth.json /usr/local/tomcat/webapps/ROOT/WEB-INF/
# use default port and entrypoint
