package uk.org.llgc.annotation.store.adapters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import java.nio.charset.Charset;

import com.github.jsonldjava.utils.JsonUtils;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.AnnotationUtils;

import java.io.IOException;
import java.io.ByteArrayInputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

public abstract class AbstractStoreAdapter implements StoreAdapter {
    public static final String FULL_TEXT_PROPERTY = Annotation.FULL_TEXT_PROPERTY;

    protected static Logger _logger = LogManager.getLogger(AbstractStoreAdapter.class.getName());
	protected AnnotationUtils _annoUtils = null;
    protected SimpleDateFormat _dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	public void init(final AnnotationUtils pAnnoUtils) {
		_annoUtils = pAnnoUtils;
	}

	public List<Model> addAnnotationList(final List<Map<String,Object>> pJson) throws IOException, IDConflictException, MalformedAnnotation {
		List<Model> tModel = new ArrayList<Model>();
		for (Map<String,Object> tAnno : pJson) {
			tModel.add(this.addAnnotation(tAnno));
		}
		return tModel;
	}

    /**
     * Take the annotation add a unique ID if it doesn't have one
     * also add within links to the manifest if one can be found.
     */
	public Model addAnnotation(final Map<String,Object> pJson) throws IOException, IDConflictException, MalformedAnnotation {
        Annotation tAnno = new Annotation(pJson);
        tAnno.checkValid();
		if (this.getNamedModel(tAnno.getId()) != null) {
			_logger.debug("Found existing annotation with id " + tAnno.getId());
			tAnno.setId(tAnno.getId() + "1");
			if (tAnno.getId().length() > 400) {
				throw new IDConflictException("Tried multiple times to make this id unique but have failed " + tAnno.getId());
			}
			return this.addAnnotation(tAnno.toJson());
		} else {
			this.expandTarget(pJson);
            this.addWithins(tAnno);
			
			return addAnnotationSafe(pJson);
		}
	}

	public Model updateAnnotation(final Map<String,Object> pJson) throws IOException, MalformedAnnotation {
        Annotation tAnno = new Annotation(pJson);
        tAnno.checkValid();
		_logger.debug("processing " + JsonUtils.toPrettyString(tAnno.toJson()));
		// add modified date and retrieve created date
		_logger.debug("ID " + tAnno.getId());
		Model tStoredAnno = this.getNamedModel(tAnno.getId());
        if (tStoredAnno == null) {
            throw new IOException("Failed to find annotation with id " + tAnno.getId() + " so couldn't update.");
        }
        this.begin(ReadWrite.READ);
		Resource tAnnoRes = tStoredAnno.getResource(tAnno.getId());
		Statement tCreatedSt = tAnnoRes.getProperty(DCTerms.created);
		if (tCreatedSt != null) {
            tAnno.setCreated(tCreatedSt.getString());
		}
        this.end();
        tAnno.updateModified();
		_logger.debug("Modified annotation " + tAnno.toString());
		deleteAnnotation(tAnno.getId());

		addWithins(tAnno);

		return addAnnotationSafe(pJson);
	}

    protected void addWithins(final Annotation pAnno) throws IOException {
        List<Map<String, Object>> tMissingWithins = pAnno.getMissingWithin();
        if (tMissingWithins != null && !tMissingWithins.isEmpty()) {
            // missing within so check to see if the canvas maps to a manifest
            String tCanvasId = "";
            for (Map<String,Object> tOn : tMissingWithins) {
                tCanvasId = (String)tOn.get("full");

                List<String> tManifestURI = getManifestForCanvas(tCanvasId);
                if (tManifestURI != null && !tManifestURI.isEmpty()) {
                    List<Map<String,String>> tWithinLinks = new ArrayList<Map<String,String>>();
                    for (String tManifest : tManifestURI) {
                        tWithinLinks.add(this.createWithin(tManifest));
                    }
                    tOn.put("within", tWithinLinks.size() == 1 ? tWithinLinks.get(0) : tWithinLinks);
                }
            }
        }
    }

    protected Map<String,String> createWithin(final String pManifestURI) {
        Map<String,String> tWithin = new HashMap<String,String>();
        tWithin.put("@id", pManifestURI);
        tWithin.put("@type", "sc:Manifest");
        return tWithin;
    }

    // Could add to Annotation class
    protected void addWithin(Map<String, Object> pAnnoJson, final String pManifestURI, final String pTargetId) {
        Annotation tAnno = new Annotation(pAnnoJson);
        for (Map<String,Object> tOn : tAnno.getOn()) {
            if (tOn.get("full").equals(pTargetId)) {
                if (tOn.get("within") != null) {
                    if (tOn.get("within") instanceof Map) {
                        if (((Map<String,String>)tOn.get("within")).get("@id").equals(pManifestURI)) {
                            // job done this anno already links to the manifest.
                        } else {
                            // this contains a within but links to another manifest.
                            List<Map<String,String>> tWithinLinks = new ArrayList<Map<String,String>>();
                            tWithinLinks.add((Map<String,String>)tOn.get("within"));
                            tWithinLinks.add(this.createWithin(pManifestURI));
                            tOn.put("within", tWithinLinks);
                        }
                    } else {
                        // Must be a list check to see if this manifest id is present if not add it.
                        if (!((List<String>)tOn.get("within")).contains(pManifestURI)) {
                            ((List<String>)tOn.get("within")).add(pManifestURI);
                        }
                    }
                } else {
                    tOn.put("within", pManifestURI);
                }
            }
        }
    }

    public void expandTarget(final Map<String,Object> pJson) {
		String tURI = null;
		Map<String,Object> tSpecificResource = null;
		if (pJson.get("on") instanceof String) {
			tURI = (String)pJson.get("on");
			tSpecificResource = new HashMap<String,Object>();
			pJson.put("on", tSpecificResource);
		} else if (pJson.get("on") instanceof Map) {
			tSpecificResource = (Map<String,Object>)pJson.get("on");

			if (tSpecificResource.get("@id") == null || ((String)tSpecificResource.get("@id")).indexOf("#") == -1) {
				return; // No id to split or no fragement
			}
			if (tSpecificResource.get("selector") != null) {
				return; // already have a selector
			}
			tURI = (String)tSpecificResource.get("@id");
			tSpecificResource.remove("@id");
		} else {
			return; // could be a list so not processing
		}
		int tIndexOfHash = tURI.indexOf("#");
		tSpecificResource.put("@type","oa:SpecificResource");
		Map<String,Object> tFragement = new HashMap<String,Object>();
		tSpecificResource.put("selector", tFragement);
		tSpecificResource.put("full", tURI.substring(0, tIndexOfHash));

		tFragement.put("@type", "oa:FragmentSelector");
		tFragement.put("value", tURI.substring(tIndexOfHash + 1));
	}

	protected boolean isMissingWithin(final Map<String,Object> pAnno) {
		if (pAnno.get("on") != null) {
			if (pAnno.get("on") instanceof String) {
				return true;
			}
			if (pAnno.get("on") instanceof Map) {
				return ((Map<String,Object>)pAnno.get("on")).get("within") == null;
			}
		}
		return true;
	}
	protected String getFirstCanvasId(final Object pOn) {
		if (pOn instanceof Map) {
			return (String)((Map<String,Object>)pOn).get("full");
		} else if (pOn instanceof String) {
			String tURL = (String)pOn;
			return tURL.split("#")[0];
		} else if (pOn instanceof List) {
			return getFirstCanvasId(((List)pOn).get(0));
		}
		_logger.error("On in annotation is a format I don't regocnise its a format type " + pOn.getClass().getName());
		return null;
	}

	public String indexManifest(Map<String,Object> pManifest) throws IOException {
		String tShortId = this.createShortId((String)pManifest.get("@id"));
		return this.indexManifest(tShortId, pManifest);
	}

	protected String indexManifest(final String pShortId, Map<String,Object> pManifest) throws IOException {
		String tManifestId = (String)pManifest.get("@id");

		Map<String,Object> tExisting = this.getManifest(pShortId);
		if (tExisting != null) {
			if (((String)tExisting.get("@id")).equals((String)pManifest.get("@id"))) {
				return (String)tExisting.get("short_id"); // manifest already indexed
			} else {
				// there already exists a document with this id but its a different manifest so try and make id unique
				return indexManifest(pShortId + "1", pManifest);
			}
		}
		pManifest.put("short_id",pShortId);//may need to make this a uri...

		Map<String,Object> tShortIdContext = new HashMap<String,Object>();
		tShortIdContext.put("@id","http://purl.org/dc/elements/1.1/identifier");
		Map<String,Object> tExtraContext = new HashMap<String,Object>();
		tExtraContext.put("short_id", tShortIdContext);
		if (pManifest.get("@context") instanceof List) {
			List<Map<String,Object>> tListContext = (List<Map<String,Object>>)pManifest.get("@context");
			tListContext.add(tExtraContext);
		} else {
			String tContext = (String)pManifest.get("@context");
			List<Object> tListContext = new ArrayList<Object>();
			tListContext.add(tContext);
			tListContext.add(tExtraContext);

			pManifest.put("@context", tListContext);
		}

		return this.indexManifestNoCheck(pShortId, pManifest);
	}

	public String createShortId(final String pLongId) throws IOException {
		if (pLongId.endsWith("manifest.json")) {
			String[] tURI = pLongId.split("/");
			return tURI[tURI.length - 2];
		} else {
			return _annoUtils.getHash(pLongId, "md5");
		}
	}

	public abstract List<String> getManifestForCanvas(final String pCanvasId) throws IOException;
	public abstract Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException;
	public abstract Map<String, Object> search(final SearchQuery pQuery) throws IOException;
	protected abstract String indexManifestNoCheck(final String pShortID, final Map<String,Object> pManifest) throws IOException;
	public abstract List<Manifest> getManifests() throws IOException;
	public abstract String getManifestId(final String pShortId) throws IOException;
	public abstract Map<String,Object> getManifest(final String pShortId) throws IOException;

	public Model getAnnotation(final String pId) throws IOException {
		return getNamedModel(pId);
	}

	protected abstract Model getNamedModel(final String pName) throws IOException;

	protected void begin(final ReadWrite pWrite) {
	}
	protected void end() {
	}

	protected Model convertAnnoToModel(final Map<String,Object> pJson) throws IOException {
		return _annoUtils.convertAnnoToModel(pJson);
	}

}

