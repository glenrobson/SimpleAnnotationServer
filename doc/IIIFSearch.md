Register a manifest by going to:

http://localhost:8888/uploadManifest.html

This may take some time depending on the number of pages in the manifest and how many existing annotations are in the repo. Note if SAS is being proxied through Apache you may get a timeout but the process is still runnning. 

Upload Manfiest indexes the manifest in the annotation store and looks for any annotations that have are related to canvas in this manifest. If there is an annotation that links to a canvas in this manifest then a 'within' link is added. The process then creates a short id to be used for the manfiest and forwards onto the newly created IIIF search endpoint. 
