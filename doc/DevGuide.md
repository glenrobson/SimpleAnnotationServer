## Jetty auto deploy changes

If you make any changes to the HTML, Javascript, css (anything apart from the Java files) Jetty will automatically pick up the changes so a browser refresh should get the new content. There is no need to restart jetty. If you change any Java files running mvn compile while also running mvn jetty:run jetty will ensure jetty picks up any changes

## Updating Mirador

Mirador is included in the SimpleAnnotationServer to aid quick deployment. If you would like to update Mirador follow the instructions on the [Mirador Site](https://github.com/IIIF/mirador) and copy everything from mirador/build/mirador/* to [src/main/webapp/mirador](../src/main/webapp/mirador).

## Overloading config using Environment variables

It is possible to overload any of the configuration specified in [src/main/webapp/WEB-INF/sas.properties](../src/main/webapp/WEB-INF/sas.properties) by prepending `SAS.` to any of the properties in sas.properties e.g:

```
export SAS.store="solr"
```
