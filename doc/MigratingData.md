

need to get existing annotations by navitaging to http://localhost:8888/annotation/, download file.


``` 
curl "http://localhost:8888/annotation/" > /tmp/bor_annotations.json
```

ensure any required encoders/decoders are included in the web.xml
stop simpleannotation server
configre to new back end
start simple annotation server
post it to populate to load all of the annos:

curl -v -X POST --data-binary @/tmp/test.json http://localhost:8888/annotation/populate -H "Content-type: application/json"

Note this will only migrate the annotations, you will have to re-load the manifests. 
