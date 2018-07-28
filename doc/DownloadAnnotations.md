# Hosting the annotations locally and linking to a manifest

Sometimes it can be helpful to publish your annotations as static files to allow readonly access to the annotations. To do this you need to create an annotation list per canvas you have annotated. You can do this by running the included `scripts/downloadAnnotationListsByCanvas.py`. This is run as follows:

```
# downloadAnnotationListsByCanvas.py [manifest] [sas_endpoint] [output_dir] [optional outputfilename proc]
./scripts/downloadAnnotationListsByCanvas.py http://dams.llgc.org.uk/iiif/newspapers/3320639.json http://localhost:8888 /tmp/annotations
```

Where:
 * mainfest is the manifest you want the annotations for. Note this will download a file per canvas in this manifest.
 * sas_endpoint the SimpleAnnotationServer that contains the annotations.
 * output_dir the output directory for the annotation files.

If you followed the instructions on loading the [example newspaper manifest](NewManifests.md) and [populating the annotation list](PopulatingAnnotations.md), you will see the following:

```
SimpleAnnotationServer glen$ ./scripts/downloadAnnotationListsByCanvas.py http://dams.llgc.org.uk/iiif/newspaper/issue/3320640/manifest.json  http://localhost:8888 /tmp/annotations
Downloading manifest
Downloading http://dams.llgc.org.uk/iiif/3320640/canvas/3320641
Downloading http://dams.llgc.org.uk/iiif/3320640/canvas/3320642
Downloading http://dams.llgc.org.uk/iiif/3320640/canvas/3320643
Downloading http://dams.llgc.org.uk/iiif/3320640/canvas/3320644
SimpleAnnotationServer glen$ ls /tmp/annotations/
page1.json      page2.json      page3.json      page4.json
```

You will need to put these json files somewhere publicly accessible on a http server along with your manifest. For example if you were to host them on `http://example.com/files/page1.json` you could then reference them from your manifest by adding the following within the canvas section:

```
  "@id": "http://dams.llgc.org.uk/iiif/3320640/canvas/3320641",
  "@type": "sc:Canvas",
  ...
 "images": [
    ...
 ],
 "otherContent": [
    {
        "@id": "http://example.com/files/page1.json",
        "@type": "sc:AnnotationList",
        "label": "My fantastic annotations"
    }]
```
