# Audit log proposal

Currently the Simple Annotation Server (SAS) is reliant on a single backend. This can be problomatic with unreliable back ends like Elastic search or SOLR which  have a tendency to become corrupted. Elastic search recomends having mupltiple backends to avoid this problem but this can get expensive with [AWS](https://aws.amazon.com/opensearch-service/pricing/). Simliar issues have also been raised about [SOLR](https://github.com/glenrobson/SimpleAnnotationServer/issues/30). 

This proposal is to create a audit log which SAS can use to rebuild a corrupted backend. It should support the following screnarios:

 * Backend is empty (for example Elastic search will create an empty instance if the lastone was corrupt)
 * Backend is behind the current transaction
 * Multiple frontends should not conflict during the rebuild process
 * Audit log should be as cheap as possible and not rely on AWS services
 
## File based audit log

To keep the costs down and to not replicate to another database I propose to use a file based audit log. For every transaction made to the annotation store a timestamped log file will be created. This will contain enough information for the transaction to be replayed in the event of a recovery situation.

A transaction id will be kept in the backend database and also in the audit log. When SAS starts it will check the backend transaciton id with the id in the audit log. If they don't match SAS will go back through the audit log until it finds the matching transactions and replay them. If it doesn't find the matching id then it will replace all of the transactions. 

While SAS is checking the last transaction id and during any rebuild it will move into a paused state and will responed with an appropreate error. The first SAS instance to start the rebuild will write a lock file to the audit directory so that any other SAS instances will wait until it is complete. 

The structure of a audit log will be as follows:

 * $AUDIT_HOME/2020/09/02/18-33-00-00.json
 
The lock file will be:
 * $AUDIT_HOME/rebuild.lock

The lock file will contain the IP-address of the SAS instance that is running the rebuild. 

By using a file based audit log this can be stored on AWS EBS storage and shared between multiple SAS instances. It can also be backedup if required. 

## IIIF Discovery ActivityStream

The audit files will be in a JSON format simliar to the [IIIF Activity streams](https://iiif.io/api/discovery/1.0/) but contain annotations e.g:

```
{
  "id": "https://sas_server/audit/<transaction_id>",
  "type": "Create",
  "startTime": "2017-09-20T23:58:00Z",
  "endTime": "2017-09-20T00:00:00Z"
  "object": {
    "{
      "@context" : "file:///Users/gmr/development/mvn/SAS/master/SimpleAnnotationServer/src/main/webapp/contexts/iiif-2.0.json",
      "id": http://dev.llgc.org.uk/anno/1",
      "type" : "oa:Annotation",
      "motivation" : [ "oa:commenting" ],
      "resource" : {
        "@type" : "dctypes:Text",
        "format" : "text/html",
        "chars" : "<p>Bob Smith</p>"
      },
      "on" : {
        "@type" : "oa:SpecificResource",
        "full" : "http://dev.llgc.org.uk/iiif/examples/photos/canvas/3891216.json",
        "selector" : {
          "@type" : "oa:FragmentSelector",
          "value" : "xywh=5626,1853,298,355"
        }
      }
    }
  }
}
```

This will also allow a web based version of the activity stream which could be used to keep a third party database updated. For example a triple store or a S3 sync script using a Lambda function which polls for changes. 
