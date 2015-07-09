## Storing annotations in Sesame rather than Jena

Accessing annotations in Jena can be a bit difficult as the database is embedded and located in the data directory (location configured in the [web.xml](../src/main/webapp/WEB-INF/web.xml). Jena also doesn't allow two Java processes to access the same data directory so you have to stop the web server before looking at the Jena database (or extend the servlet functionality to give access to the DB).

One solution to this is to use the [Sesame](http://rdf4j.org/) RDF store which has a web front end which allows you to view the data. To do this configure the [web.xml](../src/main/webapp/WEB-INF/web.xml) and uncomment the following:

```
<init-param>
	<param-name>store</param-name>
	<param-value>sesame</param-value>
	<description>RDF Store to use</description>
</init-param>
<init-param>
	<param-name>repo_url</param-name>
	<param-value>http://hostname:port/openrdf-sesame/repositories/annotations</param-value>
	<description>Set the http URL to use to the Sesame Repository</description>
</init-param>
```

Change the repo_url value to the location of your Sesame database. You will also need to comment out the Jena database config:

```
<!--
<init-param>
	<param-name>store</param-name>
	<param-value>jena</param-value>
	<description>RDF Store to use</description>
</init-param>
<init-param>
	<param-name>data_dir</param-name>
	<param-value>data</param-value>
	<description>Sets the directory containing TDB RDF database</description>
</init-param>
-->
```
