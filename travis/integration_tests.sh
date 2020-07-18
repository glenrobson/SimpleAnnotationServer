#!/bin/bash

function waitForPort {
    echo "Waiting $1 to launch on $2 ..."

    while ! nc -z localhost $2; do   
      sleep 0.1 # wait for 1/10 of the second before check again
    done

    echo "$1 launched"
}

failed=0
failures=""

echo "Running integration tests with different back ends"

echo "Testing Jena"
export "config=jena.properties"
mvn test
if [ $? -ne 0 ]; then
    failures="${failures} Failed Jena tests\n"
    failed=1
fi    

echo "Testing solr:"

echo "Starting SOLR:"
docker-compose -f docker/sas-solr/docker-compose.yml --project-directory . up -d solr
waitForPort "SOLR" 8983
sleep 5

export "config=solr.properties"
echo "Running test"
mvn test
if [ $? -ne 0 ]; then
    failures="${failures} Failed SOLR tests\n"
    failed=1
fi    

docker-compose -f docker/sas-solr/docker-compose.yml --project-directory . down 

echo "$failures"
exit $failed
