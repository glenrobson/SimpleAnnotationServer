## Hosting Mirador and SimpleAnnotationServer on different servers

CORS is enabled in the SimpleAnnotationServer so to connect to a remote store change the following in your html page:

```
annotationEndpoint: {
	name: 'Simple Annotation Store Endpoint',
	module: 'SimpleASEndpoint',
	options: {
		url: 'annotation'
	}
}
```

Change the url from annotation to http://remote_host:port/simpleAnnotationStore/annotation where remote_host is the remote host that is hosting the SimpleAnnotationServer and port is the port its running on. See [remote.html](../src/main/webapp/remote.html) for an example.
