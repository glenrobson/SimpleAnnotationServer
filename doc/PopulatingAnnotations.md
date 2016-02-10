## Populating the Annotation Store with IIIF Annotation List

If you already have an IIIF Annotation List you can populate the Annotation Server with this data. This might be the case if you had an Annotation List generated from OCR data but wanted to correct it in Mirador. There is an example Annotation List for a NLW Newspaper inspired by the work of [azaroth42](https://github.com/azaroth42) at:

[http://dev.llgc.org.uk/iiif/examples/newspapers/3100187-anno.json](http://dev.llgc.org.uk/iiif/examples/newspapers/3100187-anno.json)

To load this navigate to:

[http://localhost:8888/populate.html](http://localhost:8888/populate.html)

and add the Annotation list in the box and click submit. Wait until you see SUCCESS displayed in the browser. If you loaded the example Newspaper Manifest in [Adding your own Manifests](NewManifests.md) you can navigate to:

[http://localhost:8888/demo.html](http://localhost:8888/demo.html)

and open the Newspaper. If you click the Speech bubble on the first page (bottom left of the Mirador screen) you should see the OCR annotations appear. You can now edit/delete these annotations. 


