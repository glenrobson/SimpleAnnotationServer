#!/bin/bash
# follow startup log using: docker exec -it sas journalctl -fu setup.service
echo "Running Script `date`"
# Wait for SOLR to start correctly
while [ true ];
do
    echo "Checking if solr is running `date`"
    curl "http://localhost:8983/solr/test/admin/ping" 2> /dev/null
    exitValue=$?
    if [ $exitValue == 0 ]; then
        break;
    fi
    echo "Exit value was $exitValue sleeping"
    sleep 5s
done
# load test annotations
echo "Loading test annotaitons"
while [ true ];
do
    response=`curl -vX POST "http://localhost:8080/sas/annotation/populate?uri=http://localhost/annotations.json" 2>&1 |grep '< HTTP'|grep -o [0-9][0-9][0-9]`
    if [ $response == 201 ]; then
        echo "Succesfully loaded annotations"
        break;
    fi
    echo "Load returned $response retrying"
    sleep 5s
done
# SOLR now ready

response=`curl -vX POST "http://localhost:8080/sas/manifests?uri=https://damsssl.llgc.org.uk/iiif/2.0/4389767/manifest.json" 2>&1 |grep '< HTTP'|grep -o [0-9][0-9][0-9]`
echo "Loaded mainfest $response"
echo "DONE"
