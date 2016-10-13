#!/bin/bash

download() {
	FILE="$2.tgz"
	if [ -f $FILE ]; then
		echo "File $FILE exists."
		tar -zxf $FILE
	else
		echo "File $FILE does not exist. Downloading solr from $1..."
		curl -O $1
		tar -zxf $FILE
	fi
	echo "Downloaded!"
}

SOLR_VERSION="6.2.1"
SOLR_DIR="solr-$SOLR_VERSION"
SOLR_CORE="testannotations"
download "http://archive.apache.org/dist/lucene/solr/$SOLR_VERSION/solr-$SOLR_VERSION.tgz" "solr-$SOLR_VERSION"

SOLR_CORE_HOME="$SOLR_DIR/server/solr/$SOLR_CORE"
cp -r src/main/resources/solr $SOLR_CORE_HOME

cd $SOLR_DIR
./bin/solr start
