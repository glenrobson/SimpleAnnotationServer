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
docker exec -t simpleannotationserver_solr_1 /opt/docker-solr/scripts/wait-for-solr.sh --max-attempts 10 --wait-seconds 5 --solr-url http://0.0.0.0:8983/

export "config=solr.properties"
echo "Running test"
mvn test
if [ $? -ne 0 ]; then
    failures="${failures} Failed SOLR tests\n"
    failed=1
fi    

docker-compose -f docker/sas-solr/docker-compose.yml --project-directory . down 


echo "Running solr-cloud test"

docker-compose -f docker/sas-solr-cloud/docker-compose.yml --project-directory . up -d 
running=2

# figure some way of waiting until SOLR is up and running..
while [ $running -eq 2 ];
do
    sleep 5
    running=`docker ps --filter "name=create-collection" |wc -l`
done
docker exec -t solr1 /opt/docker-solr/scripts/wait-for-solr.sh --max-attempts 10 --wait-seconds 5 --solr-url http://0.0.0.0:8983/
docker ps
docker logs simpleannotationserver_web_1
# Due to the way docker-compose and SOLR works we can't access the SOLR cloud
# from this machine. Instead we have to run the test within the cluster
docker exec -t --workdir /usr/src/sas simpleannotationserver_web_1 /usr/bin/mvn test
if [ $? -ne 0 ]; then
    failures="${failures} Failed SOLR Cloud tests\n"
    failed=1
fi 

docker-compose -f docker/sas-solr-cloud/docker-compose.yml --project-directory . down 

echo "Testing ElasticSearch:"

echo "Starting ElasticSearch:"
docker-compose -f docker/sas-elastic/docker-compose.yml --project-directory . up -d elastic
http_code=100
while [ "$http_code" != "200" ]
do 
    sleep 5
    http_code=`curl --write-out %{http_code} --silent --output /dev/null "http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=50s"`
    echo "$http_code";
done

export "config=elastic.properties"
echo "Running test"
mvn test
if [ $? -ne 0 ]; then
    failures="${failures} Failed Elastic tests\n"
    failed=1
fi    

echo "$failures"
exit $failed
