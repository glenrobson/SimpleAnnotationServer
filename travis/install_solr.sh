#!/bin/bash

download() {
	FILE="$2.tgz"
	if [ -f $FILE ]; then
		echo "File $FILE exists. Extracting.."
		if [ -d $2 ]; then
			echo "File already extracted"
		else 
			tar -zvxf $FILE
		fi	
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
if [ -z "$1" ];then
	SAS_HOME=".";
else 
	SAS_HOME="$1";
fi
download "http://archive.apache.org/dist/lucene/solr/$SOLR_VERSION/solr-$SOLR_VERSION.tgz" "solr-$SOLR_VERSION"

SOLR_CORE_HOME="$SOLR_DIR/server/solr/$SOLR_CORE"
mkdir -p $SOLR_CORE_HOME/data
cp -r $SAS_HOME/src/main/resources/solr/* $SOLR_CORE_HOME

cd $SOLR_DIR
./bin/solr start

curl "http://localhost:8983/solr/admin/cores?action=CREATE&name=testannotations&instanceDir=testannotations" | xmllint -format -
curl http://localhost:8983/solr/admin/cores |xmllint -format -
