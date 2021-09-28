package uk.org.llgc.annotation.store.adapters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Collection;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.Target;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.IIIFSearchResults;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.stats.TopLevel;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.AnnotationUtils;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.github.jsonldjava.utils.JsonUtils;

import org.apache.jena.query.ReadWrite;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

public abstract class AbstractStoreAdapter implements StoreAdapter {
    protected static Logger _logger = LogManager.getLogger(AbstractStoreAdapter.class.getName());
	protected AnnotationUtils _annoUtils = null;

	public void init(final AnnotationUtils pAnnoUtils) {
		_annoUtils = pAnnoUtils;
	}

	public AnnotationList addAnnotationList(final AnnotationList pAnnoList) throws IOException, IDConflictException, MalformedAnnotation {
        AnnotationList tUpdatedList = new AnnotationList();
		for (Annotation tAnno : pAnnoList.getAnnotations()) {
			tUpdatedList.getAnnotations().add(this.addAnnotation(tAnno));
		}
		return tUpdatedList;
	}

    /**
     * Take the annotation add a unique ID if it doesn't have one
     * also add within links to the manifest if one can be found.
     */
	public Annotation addAnnotation(final Annotation pAnno) throws IOException, IDConflictException, MalformedAnnotation {
        pAnno.checkValid();
		if (this.getAnnotation(pAnno.getId()) != null) {
			_logger.debug("Found existing annotation with id " + pAnno.getId());
			pAnno.setId(pAnno.getId() + "1");
			if (pAnno.getId().length() > 400) {
				throw new IDConflictException("Tried multiple times to make this id unique but have failed " + pAnno.getId());
			}
			return this.addAnnotation(pAnno);
		} else {
            this.addWithins(pAnno);
			
			return addAnnotationSafe(pAnno);
		}
	}

	public Annotation updateAnnotation(final Annotation pAnno) throws IOException, MalformedAnnotation {
        pAnno.checkValid();
		// add modified date and retrieve created date
		_logger.debug("ID " + pAnno.getId());
		Annotation tStoredAnno = this.getAnnotation(pAnno.getId());
        if (tStoredAnno == null) {
            throw new IOException("Failed to find annotation with id " + pAnno.getId() + " so couldn't update.");
        }
        this.begin(ReadWrite.READ);
		if (tStoredAnno.getCreated() != null) {
            pAnno.setCreated(tStoredAnno.getCreated());
		}
        this.end();
		_logger.debug("Modified annotation " + pAnno.toString());
		deleteAnnotation(pAnno.getId());

		this.addWithins(pAnno);
        pAnno.updateModified();

		return addAnnotationSafe(pAnno);
	}

    protected String formatDate(final Date pDate) {
        SimpleDateFormat tDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return tDateFormatter.format(pDate);
    }

    protected Date parseDate(final String pDate) {
        try {
            SimpleDateFormat tDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            return tDateFormatter.parse(pDate);
        } catch (ParseException tExcpt) {
            tExcpt.printStackTrace();
            System.err.println("Failed to parse date " + pDate);
            return null;
        }
    }


    protected void addWithins(final Annotation pAnno) throws IOException {
        List<Target> tMissingWithins = pAnno.getMissingWithin();
        if (tMissingWithins != null && !tMissingWithins.isEmpty()) {
            // missing within so check to see if the canvas maps to a manifest
            String tCanvasId = "";
            for (Target tTarget: tMissingWithins) {
                Canvas tCanvas = tTarget.getCanvas();

                Manifest tManifest = this.getManifestForCanvas(tCanvas);
                if (tManifest != null) {
                    tTarget.setManifest(tManifest);
                }
            }
        }
    }

    /*
        // This is in Annotation.java
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
	}*/

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

	public String indexManifest(Manifest pManifest) throws IOException {
		return this.indexManifest(pManifest.getShortId(), pManifest);
	}

	protected String indexManifest(final String pShortId, final Manifest pManifest) throws IOException {
		Manifest tExisting = this.getManifest(pManifest.getURI());
		if (tExisting != null) {
			if (tExisting.getURI().equals(pManifest.getURI())) {
				return tExisting.getShortId(); // manifest already indexed
			} else {
				// there already exists a document with this id but its a different manifest so try and make id unique
				return indexManifest(pShortId + "1", pManifest);
			}
		}

		pManifest.setShortId(pShortId);

		/*if (pManifest.get("@context") instanceof List) {
			List<Map<String,Object>> tListContext = (List<Map<String,Object>>)pManifest.get("@context");
			tListContext.add(tExtraContext);
		} else {
			String tContext = (String)pManifest.get("@context");
			List<Object> tListContext = new ArrayList<Object>();
			tListContext.add(tContext);
			tListContext.add(tExtraContext);

			pManifest.put("@context", tListContext);
		}*/
        if (pManifest.getCanvases().isEmpty()) {
            throw new IOException("Failed to load manifest " + pManifest.getURI() + " because it had no pages");
        }
		return this.indexManifestNoCheck(pShortId, pManifest);
	}

    public User retrieveUser(final User pUser) throws IOException {
        User tSavedUser = this.getUser(pUser);
        // overwrite saved user if short ID is out of sync
        if (tSavedUser == null || !pUser.getShortId().equals(tSavedUser.getShortId())) {
            return this.saveUser(pUser);
        } else {
            return tSavedUser;
        }
    }

    public List<User> getUsers(final String pGroup) throws IOException { 
        List<User> tGroup = new ArrayList<User>();
        if (pGroup.equals("admin")) {
            List<User> tAllUsers = this.getUsers();
            for (User tUser : tAllUsers) {
                if (tUser.isAdmin()) {
                    tGroup.add(tUser);
                }
            }
        }
        return tGroup;
    }

    public void updateCollection(final Collection pCollection) throws IOException {
        this.deleteCollection(pCollection);
        this.createCollection(pCollection);
    }

    public abstract int getTotalAnnotations(final User pUser) throws IOException;
    public abstract int getTotalManifests(final User pUser) throws IOException;
    public abstract int getTotalAnnoCanvases(final User pUser) throws IOException;

    public Map<String,Integer> getTotalAuthMethods() throws IOException {
        Map<String, Integer> tStats = new HashMap<String,Integer>();
        List<User> tUsers = this.getUsers();

        for (User tUser : tUsers) {
            if (tStats.get(tUser.getAuthenticationMethod()) == null) {
                tStats.put(tUser.getAuthenticationMethod(), 1);
            } else {
                int tCurrent = tStats.get(tUser.getAuthenticationMethod());
                tStats.put(tUser.getAuthenticationMethod(), tCurrent + 1);
            }
        }
        
        return tStats;
    }

	public abstract Manifest getManifestForCanvas(final Canvas pCanvas) throws IOException;
	public abstract Annotation addAnnotationSafe(final Annotation pJson) throws IOException;
	public abstract IIIFSearchResults search(final SearchQuery pQuery) throws IOException;
	protected abstract String indexManifestNoCheck(final String pShortID, final Manifest pManifest) throws IOException;
	public abstract List<Manifest> getManifests() throws IOException;
	public abstract List<Manifest> getSkeletonManifests(final User pUser) throws IOException;
	public abstract String getManifestId(final String pShortId) throws IOException;
	public abstract Manifest getManifest(final String pId) throws IOException;

    public abstract Canvas resolveCanvas(final String pShortId) throws IOException;
    public abstract void storeCanvas(final Canvas pCanvas) throws IOException;

    public abstract User getUser(final User pUser) throws IOException;
    public abstract User saveUser(final User pUser) throws IOException;

	public abstract Annotation getAnnotation(final String pId) throws IOException;

	protected void begin(final ReadWrite pWrite) {
	}
	protected void end() {
	}
}

