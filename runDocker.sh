#!/bin/bash

if [ $# -eq 0 ]; then
    echo "What backend do you want to use? Jena / Solr / Cloud / Elastic"
    read backend
else
    backend="$1"
fi    

if [[ "$backend" =~ ^[Jj]ena$ ]]; then
    echo "Running SAS with Jena on http://0.0.0.0:8888/sas/"
    docker build -t sas:latest -f docker/sas-tomcat/Dockerfile . && docker run -v /tmp/data:/usr/src/app/data --rm -p 8888:8080 --name sas sas:latest
elif [[ "$backend" =~ [Ss]olr ]]; then
    echo "Running SAS with SOLR on port 8888. Solr port: 8983"
    docker-compose -f docker/sas-solr/docker-compose.yml --project-directory . up
elif [[ "$backend" =~ [Cc]loud ]]; then
    echo "Running SAS with SOLR Cloud on port 8888"
    docker-compose -f docker/sas-solr-cloud/docker-compose.yml --project-directory . up
elif [[ "$backend" =~ [Ee]lastic ]]; then
    echo "Running SAS with Elastic Cloud on port 8888"
    docker-compose -f docker/sas-elastic/docker-compose.yml --project-directory . up
elif [[ "$backend" =~ [Aa]uth ]]; then
    echo "Running SAS with Elastic Cloud on port 8888 with Auth enabled"
    docker-compose -f docker/sas-auth/docker-compose.yml --project-directory . up
else
    echo "I don't recognise '$backend'. Options are Jena / Solr / Cloud"
fi
