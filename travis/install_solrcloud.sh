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

SOLR_VERSION="6.6.1"
SOLR_DIR="solr-$SOLR_VERSION"
SOLR_CORE="testannotations"
if [ -z "$1" ];then
	SAS_HOME="`pwd`";
else 
	SAS_HOME="$1";
fi
download "http://archive.apache.org/dist/lucene/solr/$SOLR_VERSION/solr-$SOLR_VERSION.tgz" "solr-$SOLR_VERSION"

cd $SOLR_DIR
./bin/solr -e cloud -noprompt

./bin/solr create_collection -c test -d $SAS_HOME/src/main/resources/solr -shards 2
