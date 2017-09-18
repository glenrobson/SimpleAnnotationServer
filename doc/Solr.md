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

Now configure SAS to use this core, open up the following file in the SAS folder:

src/main/webapp/WEB-INF/web.xml

Now comment out the jena configuration:

```
<!--
	<init-param>
			<param-name>store</param-name>
		    <param-value>jena</param-value>
		    <description>RDF Store to use</description>
		</init-param>
		<init-param>
			<param-name>data_dir</param-name>
		    <param-value>data</param-value>
		    <description>Sets the directory containing TDB RDF database</description>
		</init-param>
-->
```

and uncomment the SOLR config:

```
		<init-param>
	        <param-name>store</param-name>
	        <param-value>solr</param-value>
	        <description>Annotation Store to use</description>
	    </init-param>
	    <init-param>
	        <param-name>solr_connection</param-name>
	        <param-value>http://solr-host/solr/core</param-value>
	        <description>set the solr connection details</description>
	    </init-param>
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
 
