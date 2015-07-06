##Deploying to Tomcat

Edit the location of the Jena data dir as by default it will create it in $TOMCAT_HOME/data. Edit [web.xml](../src/main/webapp/WEB-INF/web.xml) and change the following:

```
<context-param>
	<description>Sets the directory containing TDB RDF database</description>
	<param-name>data_dir</param-name>
	<param-value>data</param-value>
</context-param>
```
change the param-value to something like /usr/local/annotation-data.

Create a war file:

```
mvn package
```

copy SimpleAnnotationServer/target/simpleAnnotationStore.war to your tomcat webapps directory. You should now be able to access it at:

http://host:port/SimpleAnnotationServer/index.html
