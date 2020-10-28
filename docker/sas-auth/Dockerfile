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
# remove tomcat default webapps and create data directory
RUN rm -r /usr/local/tomcat/webapps/* 
# copy SAS from build image
COPY --from=buildstage /usr/src/sas/target/simpleAnnotationStore /usr/local/tomcat/webapps/ROOT
# copy properties
COPY docker/sas-auth/sas.properties /usr/local/tomcat/webapps/ROOT/WEB-INF

# Download auth config
# Install the AWS CLI
RUN apt-get update && \
        apt-get -y install python curl unzip && \
        cd /tmp && \
        curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip" && \
        unzip awscli-bundle.zip && \
        ./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws && \
        rm awscli-bundle.zip && rm -rf awscli-bundle
RUN aws s3 cp --region eu-west-2 s3://sasconfig/auth.json /usr/local/tomcat/webapps/ROOT/WEB-INF/
# use default port and entrypoint
