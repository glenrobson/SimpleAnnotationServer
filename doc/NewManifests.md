## Adding your own Manifests

You can add your own Manifests by creating a HTML page in the [webapp](../src/main/webapp) directory. For example if you wanted to add the following NLW Newspaper Manifest:

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
						url: 'annotation',
					}
				}
	});
	});
	</script>
	</body>
</html>
```

and if you saved it as demo.html in the [webapp](../src/main/webapp) directory you could access it at [http://localhost:8888/demo.html](http://localhost:8888/demo.html).

