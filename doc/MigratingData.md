## Migrating and backing up data from SimpleAnnotationServer

To swap between back ends for the SimpleAnnotationServer or to save the annotations locally you can use the SAS export functionality. To download the annotations in SAS navigate to http://localhost:8888/annotation/, and download the file json file containing the annotations. This can be automated and used as a backup strategy using the following command:

```
curl "http://localhost:8888/annotation/" > /tmp/bor_annotations.json
```

### To swap backends in SAS

Ensure any required encoders/decoders are included in the sas.properties then:
 * Stop SimpleAnnotationServer
 * Configure to new backend
 * Start SimpleAnnotationServer
 * Post downloaded file to ```populate``` to load all of the annotation:
```
curl -v -X POST --data-binary @/tmp/test.json http://localhost:8888/annotation/populate -H "Content-type: application/json"
```
Note this will only migrate the annotations, you will have to re-load any manifests you have loaded.
