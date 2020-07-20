## Storing annotations in Sesame rather than Jena

**Deprecation Warning**: Sesame support will be removed in a future version. See [this issue](https://github.com/glenrobson/SimpleAnnotationServer/issues/73) for a discussion on the reasons why. 

Accessing annotations in Jena can be a bit difficult as the database is embedded and located in the data directory (location configured in the [sas.properties](../src/main/webapp/WEB-INF/sas.properties). Jena also doesn't allow two Java processes to access the same data directory so you have to stop the web server before looking at the Jena database (or extend the servlet functionality to give access to the DB).

One solution to this is to use the [Sesame](http://rdf4j.org/) RDF store which has a web front end which allows you to view the data. To do this configure the [sas.properties](../src/main/webapp/WEB-INF/sas.properties) and uncomment the following:

```
# Uncomment the following if you want to use Sesame
# store=sesame
# repo_url=http://localhost:8080/openrdf-sesame/repositories/test-anno
```

Change the repo_url value to the location of your Sesame database. You will also need to comment out the Jena database config:

```
# Uncomment this if you would like to use Jena as a backend
# store=jena
# data_dir=data
```
