# SimpleAnnotationServer endpoints

Below are a list of SAS endpoints but note they assume SAS is running as a root application. For the tomcat docker deployment you would need to add `/sas/` to the start of the URL. For example the create annotation endpoint would be:

```
/sas/annotation/create
```

# Search API endpoint (GET)
The endpoint for the IIIF Search implementation:

```
/search-api/_manifest_short_id/search
```

where `_manifest_short_id` is a URL safe nickname for the manifest. This is currently built in the following method:

https://github.com/glenrobson/SimpleAnnotationServer/blob/114f1024af86b03acc20cdc89682bcd4c11c2318/src/main/java/uk/org/llgc/annotation/store/adapters/AbstractStoreAdapter.java#L265

# Show annotations for a canvas (GET)
Returns annotations for a canvas:

```
/annotation/search?uri= 
```

Parameters are:
 * `uri` is the canvas URI

# Create annotation (POST)
Creates an annotation which is posted to the URL. 

```
/annotation/create
```

# Edit/update an annotation (POST)
Updates an existing annotation with the content of the posted annotation:

```
/annotation/update
```

The posted annotation should have an `@id` which exists in the store

# Delete an annotation (DELETE)

```
/annotation/destroy?uri=
```

The `uri` parameter is the ID of the annotation to delete. 

# Import a AnnotationList into the Annotation store (POST)

Useful to import an existing annotation list for editing. See [Populating Annotations](PopulatingAnnotations.md) for details on how to use this function. A AnnotationList is posted to the annotation store.

```
/annotation/populate
```
