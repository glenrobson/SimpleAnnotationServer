## IIIF Search API

SimpleAnnotationServer (SAS) can act as a IIIF Search API endpoint to allow your annotations to be searched by the Universal Viewer or through Mirador. Once the search service is added to your manifest any annotations you make using SAS will automatically appear in search results. To set this up you need to do the following:

 1. Register the manifest you would like to search.
 2. Annotation the manifest (note this can be done before step 1).
 3. Add the SAS IIIF Search API Endpoint to your manifest.

More details on these steps below.

### Registering your manifest with SAS

Register a manifest by going to:

http://localhost:8888/uploadManifest.html

This may take some time depending on the number of pages in the manifest and how many existing annotations are in the repo. Note if SAS is being proxied through Apache you may get a timeout but the process is still running. The process should forward you on to the location of the Search URI. Copy this search URI for adding to your mainfest. It will look something like:

```http://localhost:8888/search-api/identifier/search```

to test the end point is working correctly append `?q=` and you should see all of your annotations in the response.

For a practical example follow the steps in [Populating Annotations](PopulatingAnnotations.md) to load a page of annotations for a NLW Newspaper. Then load the following manifest into the uploadManifest.html page:

[http://localhost:8888/examples/Cambrian_1804-01-28.json](http://localhost:8888/examples/Cambrian_1804-01-28.json)

`uploadManifest.html` indexes the manifest in the annotation store and looks for any annotations that are related to canvas in this manifest. If there is an annotation that links to a canvas in this manifest then a 'within' link is added. The process then creates a short id to be used for the manifest and forwards onto the newly created IIIF search endpoint. The next stage is to add a link to this search service in your manifest.

### Adding a link to the search service in your mainfest

For the UnivesralViewer or Mirador to find your search service you need to add it to your manifest. Add the following section:

```
"service": {
    "@context": "http://iiif.io/api/search/0/context.json",
    "@id": "http://localhost:8888/search-api/3320640/search",
    "profile": "http://iiif.io/api/search/0/search"
},
```

at the top level of your manifest near ```label, description, license or logo```. Change the `@id` to the URL you copied in the previous step. Note the context and profile link to version `0.9` rather than the latest which is `1.0`. This is because as far as I know the UV only supports version `0.9`. Once you have this saved in your manifest you can copy the URL to your manifest and open it in the UnivesralViewer to test the search:

[UnivesralViewer](http://universalviewer.io/uv.html?manifest=http://localhost:8888/Cambrian_1804-01-28.json)

You can also test it either using the locally installed Mirador or the version at ProjectMirador:

[Project Mirador (Advanced Features)](http://projectmirador.org/demo/advanced_features.html)
