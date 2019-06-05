FROM maven:3.6.1-jdk-8

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
ADD . /usr/src/app
RUN rm -rf data
RUN mvn package

CMD ["java", "-jar", "target/dependency/jetty-runner.jar", "target/simpleAnnotationStore.war"]
