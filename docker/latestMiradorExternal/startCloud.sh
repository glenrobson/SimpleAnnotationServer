#!/bin/bash

export SOLR_HOME="/opt/solr/solr-6.6.1"
#export SOLR_OPTS="-Djetty.host=127.0.0.1"
$SOLR_HOME/bin/solr start -cloud -p 8983 -s "$SOLR_HOME/example/cloud/node1/solr"
$SOLR_HOME/bin/solr start -cloud -p 7574 -s "$SOLR_HOME/example/cloud/node2/solr" -z localhost:9983 -f
