# SimpleAnnotationServer
This is an Annotation Server which is compatiable with [IIIF](http://iiif.io) and [Mirador](https://github.com/IIIF/mirador). This Annotation Server includes
a copy of Mirador so you can get started creating annotations straight away. The annotations are stored as linked data in an [Apache Jena](https://jena.apache.org/) triple store. 


## Getting Started
**Requires:**
 * Java 1.7
 * [maven](https://maven.apache.org/)

To begin working with Mirador and the Simple Annotation Server do the following:

 * Download code

```git clone https://github.com/glenrobson/SimpleAnnotationServer.git```

 * Move into the SimpleAnnotationServer directory.

```cd SimpleAnnotationServer```

 * Start the jetty http server

```mvn jetty:run```

 * Start Annotating 

Navigate to [http://localhost:8888/index.html](http://localhost:8888/index.html)

You should now see Mirador with the default example objects. You can choose any manifest to start annotating.

## Jetty auto deploy changes

If you make any changes to the HTML, Javascript, css (anything apart from the Java files) Jetty will automatically pick up the changes so a browser refersh should get the new content. There is no need to restart jetty. If you run mvn compile while also running mvn jetty:run jetty will pick up the new versions of the Java file. 

## Adding your own Manifests

You can add your own Manifests by creating a HTML page in the [webapp](tree/master/src/main/webapp) directory. For example if you wanted to add the following NLW Newspaper Manifest:

[http://dev.llgc.org.uk/iiif/examples/newspapers/1861-01-02.json](http://dev.llgc.org.uk/iiif/examples/newspapers/1861-01-02.json)

you would create the following file:

```
<!DOCTYPE html>
<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
	<link rel="stylesheet" type="text/css" href="mirador/css/mirador-combined.css">
	<title>Mirador Viewer</title>
	<style type="text/css">
		body { padding: 0; margin: 0; overflow: hidden; font-size: 70%; }
		#viewer { background: #333 url(mirador/images/debut_dark.png) left top repeat; width: 100%; height: 100%; position: fixed; }
	</style>
</head>
<body>
	<div id="viewer"></div>

	<script src="mirador/mirador.js"></script>
	<script src="js/simpleASEndpoint.js"></script>
	<script type="text/javascript">

	$(function() {
			Mirador({
				"id": "viewer",
				"layout": "1x1",
				"mainMenuSettings" :
				{
					"show": true,
					"buttons" : {"bookmark" : true, "layout" : true},
					"userLogo": {"label": "IIIF", "attributes": {"href": "http://iiif.io"}}
				},
				'showAddFromURLBox' : false,
				"saveSession": true,
				"data": [
					/** Put your manifest below **/
					{ "manifestUri": "http://dev.llgc.org.uk/iiif/examples/newspapers/1861-01-02.json", "location": "National Library of Wales"}
				],
				"windowObjects": [],
				/** Annotations Config **/
				annotationEndpoint: {
					name: 'Simple Annotation Store Endpoint',
					module: 'SimpleASEndpoint',
					options: {
						url: '/api/annotation',
						storeId: 'comparison',
						APIKey: 'user_auth'
					}
				}
	});
	});
	</script>
	</body>
</html>
```

and if you saved it as demo.html in the [webapp]((tree/master/src/main/webapp) directory you could access it at [http://localhost:8888/demo.html](http://localhost:8888/demo.html).

## Populating the Annotation Store with IIIF Annotation List

If you already have a IIIF Annotation List you can populate the Annotation Server with this data. This might be the case if you had an Annotation List genrated from OCR data but wanted to correct it in Mirador. There is an example Annotation List for the Newspaper in the previous example created by [azaroth42](https://github.com/azaroth42) at:

[http://showcase.iiif.io/shims/wales/potter/list/3100187.json](http://showcase.iiif.io/shims/wales/potter/list/3100187.json)

To load this navigate to:

[http://localhost:8888/populate.html](http://localhost:8888/populate.html)

and add the Annotation list in the box and click submit. Wait until you see SUCCESS displayed in the browser. If you've followed the prevous section and loaded the NLW Newspaper to demo.html navigate to:

[http://localhost:8888/demo.html](http://localhost:8888/demo.html)

and open the Newspaper. If you click the Speach bubble on the first page (bottom left of the Mirador screen) you should see the OCR annotations appear. You can now edit/delete these annoations. 

## Updating Mirador

Mirador is included in the SimpleAnnotationServer to aid quick deployment. If you would like to update Mirador follow the instructions on the [Mirador Site](https://github.com/IIIF/mirador) and copy everything from mirador/build/mirador/* to [src/main/webapp/mirador](tree/master/src/main/webapp/mirador).

## Missing Functions

Note this project doesn't currently contain Authentication or Versioning of Annotations. If your looking for these functions have a look at [Triannon from Stanford](https://github.com/sul-dlss/triannon) or [Catch from Harvard](https://github.com/annotationsatharvard/catcha). 

## Thanks

Thanks to [azaroth42](https://github.com/azaroth42) for help with JsonLd framing and other useful tips. Thanks also to [Illtud](https://github.com/illtud) and [Paul](https://twitter.com/sankesolutions) for help with testing and fixing build problems. 
