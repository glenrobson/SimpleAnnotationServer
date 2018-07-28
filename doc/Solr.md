Setting up SOLR as an annotation store

Download SOLR from:

https://lucene.apache.org/solr/

(Note tested with 6.2.1). The SAS supports both Cloud and Cores but the instructions differ slightly between these two options.

## SOLR Cores

Start solr:

```
solr-6.2.1 gmr$ cd solr_dir
solr-6.2.1 gmr$ ./bin/solr start
Waiting up to 30 seconds to see Solr running on port 8983 [-]  
Started Solr server on port 8983 (pid=26908). Happy searching!
```

Copy the solr config from SAS to solr:

```
cd solr_dir
mkdir -p server/solr/annotations/data
cp -r $SAS_HOME/src/main/resources/solr/* server/solr/annotations/
```

Then create the core in solr by navigating to the SOLR web interface:

http://localhost:8983/solr/#/

and clicking create core. Make sure the instanceDir matches the name of the directory you used above (in this case it would be annotations).

Now configure SAS to use this core, open up the [sas.properties](../src/main/webapp/WEB-INF/sas.properties) file in the SAS folder:

src/main/webapp/WEB-INF/sas.properties

Now comment out the jena configuration:

```
# Uncomment this if you would like to use Jena as a backend
# store=jena
# data_dir=data
```

and uncomment the SOLR config:

```
# Uncomment the following if you want to use SOLR cores
# store=solr
# solr_connection=http://solr-host/solr/core
```

change the solr_connection from 'http://solr-host/solr/core' to 'http://localhost:8983/solr/annotations'. You should now be able to start SAS using:

```
mvn jetty:run
```

## SOLR Collections

Start SOLR:

```
 ./bin/solr -e cloud -noprompt
 ```

 Load annotations:

 ```
 # cp config from SAS:
 cp -r $SAS_HOME/src/main/resources/solr $SOLR_HOME/server/solr/configsets/annos
 ./bin/solr create_collection -c test -d server/solr/configsets/annos -shards 2
 ```

## Tips

If you need to manually change a record in SOLR you can do it with curl using the following commnad:

```
curl -X POST -d @/tmp/full_formmatted.xml -H "Content-Type: application/xml" http://localhost:8983/solr/testannotations/update?commit=true
```

Note content type is very important.
