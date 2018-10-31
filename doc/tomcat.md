# Deploying to Tomcat

Edit the location of the Jena data dir as by default it will create it in `$TOMCAT_HOME/data`. Edit [sas.properties](../src/main/webapp/WEB-INF/sas.properties) and change the following:

```
# Uncomment this if you would like to use Jena as a backend
store=jena
data_dir=data
```
Change the value of `data_dir` to something like `/usr/local/annotation-data`.

Create a war file:

```
mvn package
```

Copy `SimpleAnnotationServer/target/simpleAnnotationStore.war` to your Tomcat webapps directory. You should now be able to access it at:

http://host:port/simpleAnnotationStore/index.html
